package general;

import java.util.Date;
import java.util.TimerTask;
import java.util.Timer;
import choral.io.UserLed;
import java.io.IOException;

/**
 *
 * @author christoph krey
 */
public class LocationManager {

    private Timer timer = null;
    private TimerTask timerTask = null;
    private boolean fix = false;
    private boolean timeout = false;
    final private UserLed userLed;

    private boolean stationary = false;
    private boolean once = false;

    private Location firstLocation = null;
    private Location lastLocation = null;
    private Location lastReportedLocation = null;
    private Location currentLocation = null;

    private double trip = 0.0;

    private String rmc;
    private Date tempDate;

    private double tempLon;
    private double tempLat;
    private double tempVel;
    private double tempCog;

    private String gga;
    private double tempAlt;

    private int numSat = 0;

    private LocationManager() {
        fix = false;
        userLed = new UserLed();
        setLED(false);
        startTimer();
    }

    public static LocationManager getInstance() {
        return LocationManagerHolder.INSTANCE;
    }

    private static class LocationManagerHolder {

        private static final LocationManager INSTANCE = new LocationManager();
    }

    class FixTimeout extends TimerTask {

        public void run() {
            SLog.log(SLog.Informational, "LocationManager", "fixTimeout");
            timeout = true;
        }
    }

    private void startTimer() {
        stopTimer();
        timer = new Timer();
        timerTask = new FixTimeout();
        timer.schedule(timerTask, Settings.getInstance().getSetting("fixTimeout", 600) * 1000);
        SLog.log(SLog.Debug, "LocationManager", "start fixTimeout timer");
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timeout = false;
        SLog.log(SLog.Debug, "LocationManager", "stop fixTimeout timer");
    }

    private void setLED(boolean on) {
        SLog.log(SLog.Debug, "LocationManager", "Setting LED: " + on);
        try {
            userLed.setLed(on);
        } catch (IOException ioe) {
            SLog.log(SLog.Error, "LocationManager", "IOException UserLed.setLed");
        }
    }

    public boolean isFix() {
        return fix;
    }

    public int getNumSat() {
        return numSat;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public void zero() {
        trip = 0.0;
    }

    public Date dateLastFix() {
        if (currentLocation != null) {
            return currentLocation.date;
        } else if (lastReportedLocation != null) {
            return lastReportedLocation.date;
        }
        return null;
    }

    public boolean isOnce() {
        return once;
    }

    /*
     * RMC - Recommended Minimum Navigation Information
     *
     * 12 1 2 3 4 5 6 7 8 9 10 11| | | | | | | | | | | | |
     * $--RMC,hhmmss.ss,A,llll.ll,a,yyyyy.yy,a,x.x,x.x,xxxx,x.x,a*hh<CR><LF>
     *
     * Field Number: 1) UTC Time 2) Status, V = Navigation receiver warning, P =
     * Precise 3) Latitude 4) N or S 5) Longitude 6) E or W 7) Speed over
     * ground, knots 8) Track made good, degrees true 9) Date, ddmmyy 10)
     * Magnetic Variation, degrees 11) E or W 12) Checksum
     */
    public void processGPRMCString(String gprmc) {
        SLog.log(SLog.Debug, "LocationManager", "processGPRMCString: " + gprmc.substring(gprmc.indexOf("$GPRMC")));

        rmc = gprmc.substring(gprmc.indexOf("$GPRMC"));
        int pos = rmc.indexOf("\r\n");
        if (pos >= 0) {
            rmc = rmc.substring(0, pos);
        }

        if (Settings.getInstance().getSetting("raw", false)) {
            SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                    + "/raw",
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", true),
                    rmc.getBytes()
            );
        }

        String[] components = StringSplitter.split(rmc, ",");
        if (components.length == 13) {
            try {
                tempDate = DateFormatter.parse(components[9], components[1].substring(0, 6));

                if (fix) {
                    if (!components[2].equalsIgnoreCase("A")) {
                        fix = false;
                        setLED(false);
                        startTimer();
                        String json = getlastJSONString("l");
                        send(json);
                    }

                } else {
                    if (components[2].equalsIgnoreCase("A")) {
                        fix = true;
                        setLED(true);
                        stopTimer();
                    }
                }

                if (components[3].length() > 2) {
                    tempLat = Double.parseDouble(components[3].substring(0, 2))
                            + Double.parseDouble(components[3].substring(2)) / 60;
                    if (components[4].equalsIgnoreCase("S")) {
                        tempLat *= -1;
                    }
                    {
                        long latLong = (long) (tempLat * 1000000);
                        tempLat = latLong / 1000000.0;
                    }
                } else {
                    tempLat = 0.0;
                }

                if (components[5].length() > 3) {
                    tempLon = Double.parseDouble(components[5].substring(0, 3))
                            + Double.parseDouble(components[5].substring(3)) / 60;
                    if (components[6].equalsIgnoreCase("W")) {
                        tempLon *= -1;
                    }
                    {
                        long lonLong = (long) (tempLon * 1000000);
                        tempLon = lonLong / 1000000.0;
                    }
                } else {
                    tempLon = 0.0;
                }

                if (components[8].length() > 0) {
                    tempCog = Double.parseDouble(components[8]);
                } else {
                    tempCog = 0.0;
                }

                if (components[7].length() > 0) {
                    tempVel = Double.parseDouble(components[7]);
                    tempVel *= 1.852; // knots/h -> km/h
                    {
                        long speedLong = (long) (tempVel * 1000000);
                        tempVel = speedLong / 1000000.0;
                    }
                } else {
                    tempVel = 0.0;
                }
            } catch (NumberFormatException nfe) {
                SLog.log(SLog.Error, "LocationManager", "RMC NumberFormatException");
                rmc = null;
            } catch (StringIndexOutOfBoundsException sioobe) {
                SLog.log(SLog.Error, "LocationManager", "RMC StringIndexOutOfBoundsException");
                rmc = null;
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                SLog.log(SLog.Error, "LocationManager", "RMC ArrayIndexOutOfBoundsException");
                rmc = null;
            }
        }
    }

    /*
     * GGA - Global Positioning System Fix Data, Time, Position and fix related
     * data fora GPS receiver.
     *
     * 11 1 2 3 4 5 6 7 8 9 10 | 12 13 14 15 | | | | | | | | | | | | | | |
     * $--GGA,hhmmss.ss,llll.ll,a,yyyyy.yy,a,x,xx,x.x,x.x,M,x.x,M,x.x,xxxx*hh<CR><LF>
     *
     * Field Number: 1) Universal Time Coordinated (UTC) 2) Latitude 3) N or S
     * (North or South) 4) Longitude 5) E or W (East or West) 6) GPS Quality
     * Indicator, 0 - fix not available, 1 - GPS fix, 2 - Differential GPS fix
     * 7) Number of satellites in view, 00 - 12 8) Horizontal Dilution of
     * precision 9) Antenna Altitude above/below mean-sea-level (geoid) 10)
     * Units of antenna altitude, meters 11) Geoidal separation, the difference
     * between the WGS-84 earth ellipsoid and mean-sea-level (geoid), "-" means
     * mean-sea-level below ellipsoid 12) Units of geoidal separation, meters
     * 13) Age of differential GPS data, time in seconds since last SC104 type 1
     * or 9 update, null field when DGPS is not used 14) Differential reference
     * station ID, 0000-1023 15) Checksum
     */
    public void processGPGGAString(String gpgga) {
        SLog.log(SLog.Debug, "LocationManager", "processGPGGAString: " + gpgga.substring(gpgga.indexOf("$GPGGA")));

        gga = gpgga.substring(gpgga.indexOf("$GPGGA"));
        int pos = gga.indexOf("\r\n");
        if (pos >= 0) {
            gga = gga.substring(0, pos);
        }

        if (Settings.getInstance().getSetting("raw", false)) {
            SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                    + "/raw",
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", true),
                    gga.getBytes()
            );
        }

        String[] components = StringSplitter.split(gga, ",");
        if (components.length == 15) {
            try {
                if (components[7].length() > 0) {
                    numSat = Integer.parseInt(components[7]);
                } else {
                    numSat = 0;
                }

                if (components[9].length() > 0) {
                    tempAlt = Double.parseDouble(components[9]);
                    {
                        long altitudeLong = (long) (tempAlt * 1000000);
                        tempAlt = altitudeLong / 1000000.0;
                    }
                } else {
                    tempAlt = 0.0;
                }
            } catch (NumberFormatException nfe) {
                SLog.log(SLog.Error, "LocationManager", "GGA NumberFormatException");
                return;
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                SLog.log(SLog.Error, "LocationManager", "GGA ArrayIndexOutOfBoundsException");
                return;
            }
            if (fix && rmc != null) {
                rollLocation(tempDate, tempLon, tempLat, tempCog, tempVel, tempAlt);
            }
        }
    }

    private void rollLocation(Date date, double lon, double lat, double cog, double vel, double alt) {
        Location secretLocation;

        secretLocation = new Location();
        secretLocation.date = date;
        secretLocation.longitude = lon;
        secretLocation.latitude = lat;
        secretLocation.course = cog;
        secretLocation.speed = vel;
        secretLocation.altitude = alt;

        Date offUntil = new Date(Settings.getInstance().getSetting("offUntil", 0L) * 1000);
        SLog.log(SLog.Debug, "LocationManager",
                "offUntil " + DateFormatter.isoString(offUntil)
                + " date " + DateFormatter.isoString(date));
        if (offUntil.getTime() < date.getTime()) {
            int sensitivity = Settings.getInstance().getSetting("sensitivity", 1);
            int minDistance = Settings.getInstance().getSetting("minDistance", 100);
            int minSpeed = Settings.getInstance().getSetting("minSpeed", 5);
            int maxInterval = Settings.getInstance().getSetting("maxInterval", 60);
            int minInterval = Settings.getInstance().getSetting("minInterval", 1800);

            currentLocation = secretLocation;

            if (firstLocation == null) {
                firstLocation = currentLocation;
                trip = 0.0;
            }

            if (lastLocation != null) {
                double distance = lastLocation.distance(currentLocation);
                SLog.log(SLog.Debug, "LocationManager",
                        "move: " + distance + " speed: " + currentLocation.speed);
                if (distance > sensitivity) {
                    trip += distance;
                }
            }
            lastLocation = currentLocation;

            if (lastReportedLocation != null) {
                boolean transitionMoveToPark = false;
                boolean transitionParkToMove = false;
                double distance = lastReportedLocation.distance(currentLocation);
                if (vel > minSpeed || distance > minDistance) {
                    if (stationary) {
                        transitionParkToMove = true;
                    }
                    stationary = false;
                } else {
                    if (!stationary) {
                        transitionMoveToPark = true;
                    }
                    stationary = true;
                }

                long timeSinceLast = currentLocation.date.getTime() / 1000 - lastReportedLocation.date.getTime() / 1000;

                if (stationary && timeSinceLast > minInterval) {
                    String json = getJSONString("T");
                    send(json);
                } else if (!stationary && timeSinceLast > maxInterval) {
                    String json = getJSONString("t");
                    send(json);
                } else if (transitionMoveToPark) {
                    String json = getJSONString("k");
                    send(json);
                } else if (transitionParkToMove) {
                    String json = getJSONString("v");
                    send(json);
                }
            } else {
                Date fixDate = currentLocation.date;
                SLog.log(SLog.Debug, "LocationManager", "set RTC w/ first fix " + DateFormatter.isoString(date));
                String rtc = "at+cclk=\""
                        + DateFormatter.atString(date)
                        + "\"\r";
                ATManager.getInstance().executeCommandSynchron(rtc);
                String json = getJSONString("f");
                send(json);
                if (vel > minSpeed) {
                    stationary = false;
                } else {
                    stationary = true;
                }
            }
        } else {
            if (!once) {
                if (AppMain.getInstance().wakeupMode.equals(AppMain.accelerometerWakeup) && !once) {
                    String json = JSONString(secretLocation, "a", 0);
                    send(json);
                    sendAlarm(json);
                    once = true;
                } else if (AppMain.getInstance().wakeupMode.equals(AppMain.alarmClockWakeup)) {
                    String json = getJSONString("c");
                    send(json);
                    once = true;
                }
            }
        }
    }

    public void send(String json) {
        sendAnywhere(json, "");
    }

    public void sendAlarm(String json) {
        if (json != null) {
            SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                    + "/alarm",
                    Settings.getInstance().getSetting("qos", 1),
                    false,
                    json.getBytes()
            );
        }
    }

    private synchronized void sendAnywhere(String json, String subTopic) {
        if (json != null) {
            SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                    + subTopic,
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", true),
                    json.getBytes()
            );
        }
    }

    private synchronized String getJSONString(String reason) {
        if (currentLocation != null) {
            double distance = 0;
            if (lastReportedLocation != null) {
                distance = lastReportedLocation.distance(currentLocation);
                SLog.log(SLog.Debug, "LocationManager", "dist: " + distance);
            }
            lastReportedLocation = currentLocation;
            currentLocation = null;
            return JSONString(lastReportedLocation, reason, distance);
        } else {
            return null;
        }
    }

    public String getlastJSONString(String reason) {
        if (currentLocation != null) {
            return JSONString(currentLocation, reason, 0);
        } else {
            return JSONString(lastReportedLocation, reason, 0);
        }
    }

    private String JSONString(Location location, String reason, double distance) {
        if (location != null) {
            String[] fields = StringSplitter.split(
                    Settings.getInstance().getSetting("fields", "course,speed,altitude,distance,battery,trip"), ",");

            String json;
            json = "{\"_type\":\"location\"";
            json = json.concat(",\"t\":\"" + reason + "\"");

            String tid = Settings.getInstance().getSetting("tid", null);
            if (tid == null) {
                String clientID = Settings.getInstance().getSetting("clientID",
                        MicroManager.getInstance().getIMEI());
                int len = clientID.length();
                if (len > 2) {
                    tid = clientID.substring(len - 2);
                } else {
                    tid = clientID;
                }
            }
            json = json.concat(",\"tid\":\"" + tid + "\"");

            json = json.concat(",\"tst\":\"" + (location.date.getTime() / 1000) + "\"");
            json = json.concat(",\"lon\":\"" + location.longitude + "\"");
            json = json.concat(",\"lat\":\"" + location.latitude + "\"");

            if (StringSplitter.isInStringArray("course", fields)) {
                json = json.concat(",\"cog\":\"" + (long) location.course + "\"");
            }
            if (StringSplitter.isInStringArray("speed", fields)) {
                json = json.concat(",\"vel\":\"" + (long) location.speed + "\"");
            }
            if (StringSplitter.isInStringArray("altitude", fields)) {
                json = json.concat(",\"alt\":\"" + (long) location.altitude + "\"");
            }
            if (StringSplitter.isInStringArray("distance", fields)) {
                json = json.concat(",\"dist\":\"" + (long) distance + "\"");
            }
            if (StringSplitter.isInStringArray("trip", fields)) {
                json = json.concat(",\"trip\":\"" + (long) trip + "\"");
            }
            if (StringSplitter.isInStringArray("battery", fields)) {
                json = json.concat(",\"batt\":\"" + BatteryManager.getInstance().getExternalVoltageString() + "\"");
            }

            json = json.concat("}");
            return json;
        } else {
            return null;
        }
    }

    public String getLastHumanString() {
        Location location = null;
        if (currentLocation != null) {
            location = currentLocation;
        } else if (lastReportedLocation != null) {
            location = lastReportedLocation;
        }
        if (location != null) {
            String human;

            /*
             * dow mon dd hh:mm:ss zzz yyyy
             * MON JAN 01 16:54:07 UTC 2014
             * 0123456789012345678901234567
             * 0         1         2
             */
            human = DateFormatter.isoString(location.date) + "\r\n";
            human = human.concat("Latitude " + location.latitude + "\r\n");
            human = human.concat("Longitude " + location.longitude + "\r\n");
            human = human.concat("Altitude " + (long) location.altitude + "m\r\n");
            human = human.concat("Speed " + (long) location.speed + "kph\r\n");
            human = human.concat("Course " + (long) location.course + "\r\n");
            human = human.concat("Trip " + (long) trip + "m\r\n");

            return human;
        } else {
            return null;
        }
    }
}
