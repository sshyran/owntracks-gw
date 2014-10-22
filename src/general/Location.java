/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package general;

import java.util.Date;

/**
 *
 * @author christoph
 */
public class Location {

    public Date date;
    public double longitude;
    public double latitude;
    public double course;
    public double speed;
    public double altitude; 
    public double incrementalDistance;
    
    /** arcus cosinus implementation
     * using Newton's iterative method
     * @param a the angle in radians
     * @return the arcus cosinus of a 
     */
    double acos(double asin) {
        return -1 * (asin - Math.PI / 2);
    }
    
    /** arcus sinus implementation
     * using Newton's iterative method
     * @param a the angle in radians
     * @return the arcus sinus of a 
     */
    double asin(double a) {
        double x = a;
        do {
            x -=(Math.sin(x) - a) / Math.cos(x);
        } while (Math.abs(Math.sin(x) - a) > Math.E);
        return x;
}

    /** Calculate the great circle distance between two points 
     * on the earth (specified in decimal degrees)
     * @param location the other location which distance is to be calculated
     * @return the distance to the other location in m
     */
    public double distance(Location location) {
        // convert decimal degrees to radians 
        double lon1 = Math.toRadians(longitude);
        double lon2 = Math.toRadians(location.longitude);
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(location.latitude);
        
        // haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        
        // haversine formula 
        double sinDlat = Math.sin(dlat/2);
        double sinDlon = Math.sin(dlon/2);
        
        double a = sinDlat * sinDlat + Math.cos(lat1) * Math.cos(lat2) * sinDlon * sinDlon; 
        double c = 2 * asin(Math.sqrt(a));
        
        // 6367 km is the radius of the Earth
        double m = 6367000.0 * c;
        return m;
    }    
}
