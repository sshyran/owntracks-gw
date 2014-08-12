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
    
    double acos(double a) {
        final double epsilon = 1.0E-7;
        double x = a;
        do {
            x -= (Math.sin(x) - a) / Math.cos(x);
        } while (Math.abs(Math.sin(x) - a) > epsilon);

        return -1 * (x - Math.PI / 2);
    }

    public double distance(Location location) {
        double lambdaA = Math.toRadians(longitude);
        double lambdaB = Math.toRadians(location.longitude);
        double phiA = Math.toRadians(latitude);
        double phiB = Math.toRadians(location.latitude);

        double dist = acos((Math.sin(phiA) * Math.sin(phiB) + Math.cos(phiA) * Math.cos(phiB) * Math.cos(lambdaB - lambdaA))) * 6370;
        
        return dist;
    }    
}
