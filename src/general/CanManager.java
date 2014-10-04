/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import choral.io.Can;
import java.io.IOException;

/**
 *
 * @author christoph
 */
public class CanManager extends Thread {

    Can can;
    public boolean terminate = false;

    private CanManager() {
    }

    public static CanManager getInstance() {
        return CanManagerHolder.INSTANCE;
    }

    private static class CanManagerHolder {

        private static final CanManager INSTANCE = new CanManager();
    }

    public void run() {
        can = new Can();
        try {
            can.setCan("EXT", 250, "FMS","SILENT");
            can.canOn();

            while (!terminate) {
                SLog.log(SLog.Debug, "Can", "canState=" + can.canState());
                can.getCanMode();
                can.getCanNodeState();
                can.getCanSpeed();
                can.getCanType();
                can.getDriverID();
                can.getFMSdata();
                can.getStandardData();
                can.getTimeDate();
                can.getVehicleID();
                can.getWatchList();
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    //
                }
            }

            can.canOff();
        } catch (IOException ioe) {
            SLog.log(SLog.Error, "Can", "IOException " + ioe);
        }
    }
}
