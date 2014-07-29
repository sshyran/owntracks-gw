package general;

import java.util.Calendar;

/**
 *
 * @author christoph krey
 */
public class LocationManager {

    private Location firstLocation = null;
    private Location lastReportedLocation = null;
    private Location currentLocation = null;
    private String reason = "";
    
    private int minDistance = 0; // in meters
    private int maxInterval = 0; // in seconds

    private LocationManager() {
    }

    public static LocationManager getInstance() {
        return LocationManagerHolder.INSTANCE;
    }

    private static class LocationManagerHolder {

        private static final LocationManager INSTANCE = new LocationManager();
    }

    public int getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(int min) {
        if (min < 0) {
            min = 0;
        }
        minDistance = min;
    }

    public int getMaxInterval() {
        return maxInterval;
    }

    public void setMaxInterval(int max) {
        if (max < 0) {
            max = 0;
        }
        maxInterval = max;
    }

    public boolean handleNMEAString(String nmea) {
        /*
         NMEA string example:
         $,CHORAL1,A,07,060217,174624,4540.1420,N,01155.8320,E,12.6,0.06,95.6,012768,4.3V,E,01,00,FFFFFFFF*76

         Field name      Example Unit Definition Dimension
         0   Header          $       Packet identifier Max 15 chars
         1   Device ID       CHORAL1 Device identifier Max 15 chars
         2   GPS Valid data  A A = valid, V = invalid 1 char
         3   NumSat          07 Satellites number 2 chars
         4   Date            060217 Date yymmdd 6 chars
         5   Time            174624 Time hhmmss 6 chars
         6   Latitude        4540.1420 deg ddmm.mmmm 9 chars
         7   Indicator       N/S N N = nord, S = sud 1 char
         8   Longitude       01155.8320 deg dddmm.mmmm 9 chars
         9   Indicator       E/W E E = est, W = ovest 1 chars
         10  Course          12.6 deg Course Max 6 chars
         11  Speed           0.06 knots Speed over ground Max 6 chars
         12  Altitude        95.6 msl Altitude sea level Max 6 chars
         13  Distance        012768 m Incremental distance 6 chars
         14  Vsupply         4.3V V Internal voltage 4 chars
         15  Indicator       E/B E Indicates if external power supply is connected E or if the device is working on battery B 1 char
         16  Digital input   01 Bit-coded input values. LSB is associated to input 1 1 byte
         17  Digital output  00 Bit-coded output values. LSB is associated to output 1 1 byte
         18a Analogue input  FFFFFFFF 16 Bit-coded analogue values. LSHW is associated to AI1 4 byte
         18b Separator       * '*' 1 char
         18c Checksum        76 NMEA standard checksum 2 chars
         */

        if (Settings.getInstance().getSetting("debug", false)) {
            System.out.println("LocationManager.handleNMEAString: " + nmea);
        }

        String[] parts = StringSplitter.split(nmea, ",");
        if (parts.length == 19) {
            if (parts[2].equalsIgnoreCase("A")) {
                try {
                    long day;
                    day = Long.parseLong(parts[4]);

                    long time;
                    time = Long.parseLong(parts[5]);

                    Calendar cal;
                    cal = Calendar.getInstance();
                    if (day == 0) {
                        cal.set(Calendar.YEAR, 1970);
                        cal.set(Calendar.MONTH, 1);
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                    } else {
                        cal.set(Calendar.YEAR, (int) (day / 10000 + 2000));
                        cal.set(Calendar.MONTH, (int) ((day / 100) % 100) - 1);
                        cal.set(Calendar.DAY_OF_MONTH, (int) (day % 100));
                    }
                    cal.set(Calendar.HOUR, (int) (time / 10000));
                    cal.set(Calendar.MINUTE, (int) ((time / 100) % 100));
                    cal.set(Calendar.SECOND, (int) (time % 100));

                    double lat;
                    lat = Double.parseDouble(parts[6].substring(0, 2))
                            + Double.parseDouble(parts[6].substring(2)) / 60;
                    if (parts[7].equalsIgnoreCase("S")) {
                        lat *= -1;
                    }
                    long latLong = (long)(lat * 1000000);
                    lat = latLong / 1000000.0;

                    double lon;
                    lon = Double.parseDouble(parts[8].substring(0, 3))
                            + Double.parseDouble(parts[8].substring(3)) / 60;
                    if (parts[9].equalsIgnoreCase("W")) {
                        lon *= -1;
                    }
                    long lonLong = (long)(lon * 1000000);
                    lon = lonLong / 1000000.0;
                    
                    double course;
                    course = Double.parseDouble(parts[10]);

                    double speed;
                    speed = Double.parseDouble(parts[11]);
                    speed *= 1.852; // knots/h -> km/h
                    long speedLong = (long)(speed * 1000000);
                    speed = speedLong / 1000000.0;

                    double altitude;
                    altitude = Double.parseDouble(parts[12]);
                    
                    long distance;
                    distance = Long.parseLong(parts[13]);
                    
                    String battery;
                    battery = parts[15].concat(parts[14]);

                    currentLocation = new Location();
                    currentLocation.date = cal.getTime();
                    currentLocation.longitude = lon;
                    currentLocation.latitude = lat;
                    currentLocation.course = course;
                    currentLocation.speed = speed;
                    currentLocation.altitude = altitude;
                    currentLocation.distance = distance;
                    currentLocation.battery = battery;

                    if (firstLocation == null) {
                        firstLocation = currentLocation;
                    }

                } catch (NumberFormatException nfe) {
                    System.err.println(nfe.toString());
                    return false;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    System.err.println(aioobe.toString());
                    return false;
                }

                if (lastReportedLocation != null) {
                    if (currentLocation.date.getTime() / 1000 - lastReportedLocation.date.getTime() / 1000 < maxInterval) {
                        if (currentLocation.distance(lastReportedLocation) < minDistance) {
                            return false;
                        } else {
                            reason = "d";
                            return true;
                        }
                    } else {
                        reason = "t";
                        return true;
                    }
                } else {
                    reason = "f";
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
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
    
    public String getJSONString(String[] fields) {
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
                            json = json.concat(",\"crs\":\"" + location.course + "\"");
            }
            if (isInStringArray("speed", fields)) {
                            json = json.concat(",\"spd\":\"" + location.speed + "\"");
            }
            if (isInStringArray("altitude", fields)) {
                            json = json.concat(",\"alt\":\"" + location.altitude + "\"");
            }
            if (isInStringArray("distance", fields)) {
                            json = json.concat(",\"dist\":\"" + location.distance + "\"");
            }
            if (isInStringArray("battery", fields)) {
                            json = json.concat(",\"batt\":\"" + location.battery + "\"");
            }

            json = json.concat("}");
            return json;
        } else {
            return null;
        }
    }
}
