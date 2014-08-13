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

    final private int fixTimeout = 120;
    private Timer timer = null;
    private TimerTask timerTask = null;
    private boolean fix;
    private boolean timeout;
    final private UserLed userLed;

    private boolean stationary = false;

    private Location firstLocation = null;
    private Location lastReportedLocation = null;
    private Location currentLocation = null;
    private String reason = "";

    private double travel = 0.0;

    private String rmc;
    private Date tempDate;

    private double tempLon;
    private double tempLat;
    private double tempVel;
    private double tempCog;

    private String gga;
    private double tempAlt;
    private int tempNumSat;

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
            if (Settings.getInstance().getSetting("locDebug", false)) {
                System.out.println("FixTimeout");
            }
            timeout = true;
            SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI())
                    + "/error",
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", false),
                    "FixTimeout".getBytes()
            );

        }
    }

    private void startTimer() {
        stopTimer();
        timer = new Timer();
        timerTask = new FixTimeout();
        timer.schedule(timerTask, fixTimeout * 1000);
        if (Settings.getInstance().getSetting("locDebug", false)) {
            System.out.println("start fixTimeout timer");
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timeout = false;
    }

    private void setLED(boolean on) {
        if (Settings.getInstance().getSetting("locDebug", false)) {
            System.out.println("Setting LED: " + on);
        }
        try {
            userLed.setLed(on);
        } catch (IOException ioe) {
            System.err.println("IOException UserLed.setLed");
        }
    }

    public boolean isFix() {
        return fix;
    }

    public Date dateLastFix() {
        if (currentLocation != null) {
            return currentLocation.date;
        } else if (lastReportedLocation != null) {
            return lastReportedLocation.date;
        }
        return null;
    }

    /**
     * RMC - Recommended Minimum Navigation Information
     *
     * 12 1 2 3 4 5 6 7 8 9 10 11| | | | | | | | | | | | |
     * $--RMC,hhmmss.ss,A,llll.ll,a,yyyyy.yy,a,x.x,x.x,xxxx,x.x,a*hh<CR><LF>
     *
 Field Number: 1) UTC Time 2) Status, V = Navigation receiver warning, P =
     * Precise 3) Latitude 4) N or S 5) Longitude 6) E or W 7) Speed over
     * ground, knots 8) Track made good, degrees true 9) Date, ddmmyy 10)
     * Magnetic Variation, degrees 11) E or W 12) Checksum
     */
    public void processGPRMCString(String gprmc) {
        if (Settings.getInstance().getSetting("locDebug", false)) {
            System.out.println("LocationManager.processGPRMCString: " + gprmc.substring(gprmc.indexOf("$GPRMC")));
        }

        rmc = gprmc.substring(gprmc.indexOf("$GPRMC"));
        int pos = rmc.indexOf("\r\n");
        if (pos >= 0) {
            rmc = rmc.substring(0, pos);
        }

        if (Settings.getInstance().getSetting("raw", false)) {
            SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI())
                    + "/raw",
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", true),
                    rmc.getBytes()
            );
        }

        String[] components = StringSplitter.split(rmc, ",");
        if (components.length == 13) {
            try {
                tempDate = new DateParser(components[9], components[1].substring(0, 6)).getDate();
                if (Settings.getInstance().getSetting("locDebug", false)) {
                    System.out.println("LocationManager tempDate: " + tempDate);
                }

                if (fix || timeout) {
                    if (!components[2].equalsIgnoreCase("A")) {
                        fix = false;
                        setLED(false);
                        startTimer();
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
                System.err.println("RMC NumberFormatException");
                rmc = null;
                return;
            } catch (StringIndexOutOfBoundsException sioobe) {
                System.err.println("RMC StringIndexOutOfBoundsException");
                rmc = null;
                return;
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                System.err.println("RMC ArrayIndexOutOfBoundsException");
                rmc = null;
                return;
            }
        }
    }

    /**
     * GGA - Global Positioning System Fix Data, Time, Position and fix related
     * data fora GPS receiver.
     *
     * 11 1 2 3 4 5 6 7 8 9 10 | 12 13 14 15 | | | | | | | | | | | | | | |
     * $--GGA,hhmmss.ss,llll.ll,a,yyyyy.yy,a,x,xx,x.x,x.x,M,x.x,M,x.x,xxxx*hh<CR><LF>
     *
 Field Number: 1) Universal Time Coordinated (UTC) 2) Latitude 3) N or S
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
        if (Settings.getInstance().getSetting("locDebug", false)) {
            System.out.println("LocationManager.processGPGGAString: " + gpgga.substring(gpgga.indexOf("$GPGGA")));
        }
        gga = gpgga.substring(gpgga.indexOf("$GPGGA"));
        int pos = gga.indexOf("\r\n");
        if (pos >= 0) {
            gga = gga.substring(0, pos);
        }

        if (Settings.getInstance().getSetting("raw", false)) {
            SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI())
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
                    tempNumSat = Integer.parseInt(components[7]);
                } else {
                    tempNumSat = 0;
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
                System.err.println("GGA NumberFormatException");
                return;
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                System.err.println("GGA ArrayIndexOutOfBoundsException");
                return;
            }
            if (fix && rmc != null) {
                rollLocation(tempDate, tempLon, tempLat, tempCog, tempVel, tempAlt);
            }
        }
    }

    private void rollLocation(Date date, double lon, double lat, double cog, double vel, double alt) {

        int minDistance = Settings.getInstance().getSetting("minDistance", 100);
        int minSpeed = Settings.getInstance().getSetting("minSpeed", 5);
        int maxInterval = Settings.getInstance().getSetting("maxInterval", 60);
        int minInterval = Settings.getInstance().getSetting("minInterval", 1800);

        currentLocation = new Location();
        currentLocation.date = date;
        currentLocation.longitude = lon;
        currentLocation.latitude = lat;
        currentLocation.course = cog;
        currentLocation.speed = vel;
        currentLocation.altitude = alt;

        if (firstLocation == null) {
            firstLocation = currentLocation;
            travel = 0.0;
        }

        if (lastReportedLocation != null) {
            double distance = lastReportedLocation.distance(currentLocation);
            boolean transitionFromMoveToPark = false;

            if (vel > minSpeed || distance > minDistance) {
                stationary = false;
                travel += lastReportedLocation.distance(currentLocation);
            } else {
                if (!stationary) {
                    transitionFromMoveToPark = true;
                }
                stationary = true;
            }

            long timeSinceLast = currentLocation.date.getTime() / 1000 - lastReportedLocation.date.getTime() / 1000;

            if ((stationary && timeSinceLast > minInterval)
                    || (!stationary && timeSinceLast > maxInterval)
                    || transitionFromMoveToPark) {
                reason = (stationary ? transitionFromMoveToPark ? "k" : "T" : "t");
                String[] fields = StringSplitter.split(Settings.getInstance().getSetting("fields", "course,speed,altitude,distance,battery"), ",");
                SocketGPRSThread.getInstance().put(
                        Settings.getInstance().getSetting("publish", "owntracks/gw/")
                        + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI()),
                        Settings.getInstance().getSetting("qos", 1),
                        Settings.getInstance().getSetting("retain", true),
                        getJSONString(fields).getBytes()
                );
            }
        } else {
            reason = "f";
            String[] fields = StringSplitter.split(Settings.getInstance().getSetting("fields", "course,speed,altitude,distance,battery"), ",");
            SocketGPRSThread.getInstance().put(
                    Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI()),
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", true),
                    getJSONString(fields).getBytes()
            );
        }
    }

    private boolean isInStringArray(String string, String[] stringArray) {
        for (int i = 0; i < stringArray.length; i++) {
            if (string.equals(stringArray[i])) {
                return true;
            }
        }
        return false;
    }

    private String getJSONString(String[] fields) {
        if (currentLocation != null) {
            lastReportedLocation = currentLocation;
            currentLocation = null;
            return JSONString(lastReportedLocation, fields, reason);
        } else {
            return null;
        }
    }

    public String getlastJSONString(String[] fields) {
        if (currentLocation != null) {
            return JSONString(currentLocation, fields, "m");
        } else {
            return JSONString(lastReportedLocation, fields, "m");
        }
    }

    private String JSONString(Location location, String[] fields, String reason) {
        if (location != null) {
            String json;
            json = "{\"_type\":\"location\"";
            json = json.concat(",\"t\":\"" + reason + "\"");
            json = json.concat(",\"tst\":\"" + (location.date.getTime() / 1000) + "\"");
            json = json.concat(",\"lon\":\"" + location.longitude + "\"");
            json = json.concat(",\"lat\":\"" + location.latitude + "\"");

            if (isInStringArray("course", fields)) {
                json = json.concat(",\"cog\":\"" + location.course + "\"");
            }
            if (isInStringArray("speed", fields)) {
                json = json.concat(",\"vel\":\"" + location.speed + "\"");
            }
            if (isInStringArray("altitude", fields)) {
                json = json.concat(",\"alt\":\"" + location.altitude + "\"");
            }
            if (isInStringArray("distance", fields)) {
                json = json.concat(",\"dist\":\"" + (long) travel + "\"");
            }
            if (isInStringArray("battery", fields)) {
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
            String s = location.date.toString();

            human = s.substring(4, 19) + "\r\n";
            human = human.concat("Latitude " + location.latitude + "\r\n");
            human = human.concat("Longitude " + location.longitude + "\r\n");
            human = human.concat("Altitude " + location.altitude + "m\r\n");
            human = human.concat("Speed " + location.speed + "kph\r\n");
            human = human.concat("Course " + location.course + "\r\n");
            human = human.concat("Distance " + (long) travel + "m\r\n");
            human = human.concat("Battery " + BatteryManager.getInstance().getExternalVoltageString() + "\r\n");

            return human;
        } else {
            return null;
        }
    }
}
