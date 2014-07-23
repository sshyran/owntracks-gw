/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package general;

import java.util.Calendar;
import java.util.Vector;

/**
 *
 * @author christoph krey
 */
public class LocationManager {

    private Location firstLocation = null;
    private Location lastReportedLocation = null;
    private Location currentLocation = null;
    
    private int minDistance = 1000; // in meters
    private int maxInterval = 180; // in seconds
            
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
    
    
    private String[] split(String original,String separator) {
        Vector nodes = new Vector();
        // Parse nodes into vector
        int index = original.indexOf(separator);
        while(index >= 0) {
            nodes.addElement( original.substring(0, index) );
            original = original.substring(index+separator.length());
            index = original.indexOf(separator);
        }
        // Get the last node
        nodes.addElement( original );

        // Create split string array
        String[] result = new String[ nodes.size() ];
        if( nodes.size() > 0 ) {
            for(int loop = 0; loop < nodes.size(); loop++)
            {
                result[loop] = (String)nodes.elementAt(loop);
                //System.out.println(result[loop]);
            }
        }
        return result;
    }
    
    static double acos(double a)
    {
        final double epsilon=1.0E-7; 
        double x=a;
        do {
            x-=(Math.sin(x)-a)/Math.cos(x);
        }
        while (Math.abs(Math.sin(x)-a)>epsilon);

        return -1*(x-Math.PI/2);
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
        18  Analogue input  FFFFFFFF 16 Bit-coded analogue values. LSHW is associated to AI1 4 byte
            Separator       * '*' 1 char
            Checksum        76 NMEA standard checksum 2 chars
        */
        //System.out.println(nmea);
        String []parts = split(nmea, ",");
        
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
                cal.set(Calendar.DAY_OF_MONTH , 1);
            } else {
                cal.set(Calendar.YEAR, (int) (day / 10000 + 2000));
                cal.set(Calendar.MONTH, (int) ((day / 100) % 100) - 1);
                cal.set(Calendar.DAY_OF_MONTH , (int) (day % 100));
            }
            cal.set(Calendar.HOUR, (int) (time / 10000));
            cal.set(Calendar.MINUTE, (int) ((time / 100) % 100));
            cal.set(Calendar.SECOND, (int) (time % 100));

            double lat;
            lat = Double.parseDouble(parts[6].substring(0, 2))
                    + Double.parseDouble(parts[6].substring(2)) / 60;
            if (parts[7].equalsIgnoreCase("S")) {
                lat *=-1;
            }

            double lon;
            lon = Double.parseDouble(parts[8].substring(0, 3))
                    + Double.parseDouble(parts[8].substring(3)) / 60;
            if (parts[9].equalsIgnoreCase("W")) {
                lon *=-1;
            }
            currentLocation = new Location();
            currentLocation.date = cal.getTime();
            currentLocation.longitude = lon;
            currentLocation.latitude = lat;

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
                /*
                System.out.println("maxInterval " + maxInterval 
                    + " currentLocation.date " + currentLocation.date.getTime() / 1000
                    + " lastReportedLocation.date " +lastReportedLocation.date.getTime() / 1000);
                */
                
                if (currentLocation.date.getTime() / 1000 - lastReportedLocation.date.getTime() / 1000 < maxInterval) {
                    
                    double lambdaA = Math.toRadians(lastReportedLocation.longitude);
                    double lambdaB = Math.toRadians(currentLocation.longitude);
                    double phiA = Math.toRadians(lastReportedLocation.latitude);
                    double phiB = Math.toRadians(currentLocation.latitude);
                    
                    double dist = acos((Math.sin(phiA) * Math.sin(phiB) + Math.cos(phiA) * Math.cos(phiB) * Math.cos(lambdaB - lambdaA))) * 6370000;

                    /*
                    System.out.println("lastReported " + lastReportedLocation.longitude + " " + lastReportedLocation.latitude
                            + " " + currentLocation.longitude + " " + currentLocation.latitude
                            + " " + lambdaA + " " + phiA
                            + " " + lambdaB + " " + phiB);

                    System.out.println("minDistance " + minDistance 
                    + " distance " + dist);
                    */

                    if (dist < minDistance) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } else {
            // no valid GPS
            return false;
        }
    }
    
    public String getJSONString() {
        if (currentLocation != null) {
            String json;
            json = "{\"_type\":\"location\","
                + "\"tst\":\"" + (currentLocation.date.getTime() / 1000) + "\","
                + "\"lon\":\"" + currentLocation.longitude + "\","
                + "\"lat\":\"" + currentLocation.latitude + "\""
                + "}";
            lastReportedLocation = currentLocation;
            currentLocation = null;
            return json;
        } else {
            return null;
        }
    }
}