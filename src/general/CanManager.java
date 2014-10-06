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

    private long lastFms = 0;
    private long lastObd2 = 0;

    private CanManager() {
    }

    public static CanManager getInstance() {
        return CanManagerHolder.INSTANCE;
    }

    private static class CanManagerHolder {

        private static final CanManager INSTANCE = new CanManager();
    }

    public void run() {
        while (!terminate) {
            long fms = Settings.getInstance().getSetting("fmsInterval", 0);
            if (fms != 0 && System.currentTimeMillis() / 1000L > lastFms + fms) {
                lastFms = System.currentTimeMillis() / 1000L;
                can = new Can();
                try {
                    can.setCan("EXT", 250, "FMS", "SILENT");
                    can.canOn();

                    SLog.log(SLog.Debug, "Can", "canState=" + can.canState());
                    SLog.log(SLog.Debug, "Can", "canMode=" + can.getCanMode());
                    SLog.log(SLog.Debug, "Can", "canNodeState=" + can.getCanNodeState());
                    SLog.log(SLog.Debug, "Can", "canSpeed=" + can.getCanSpeed());
                    SLog.log(SLog.Debug, "Can", "canType=" + can.getCanType());
                    SLog.log(SLog.Debug, "Can", "canDriverID=" + StringFunc.toHexString(can.getDriverID()));
                    SLog.log(SLog.Debug, "Can", "canFMSData=" + StringFunc.toHexString(can.getFMSdata()));
                    SLog.log(SLog.Debug, "Can", "canStandardData=" + StringFunc.toHexString(can.getStandardData()));
                    SLog.log(SLog.Debug, "Can", "canTimeDate=" + StringFunc.toHexString(can.getTimeDate()));
                    SLog.log(SLog.Debug, "Can", "canVehicleID=" + StringFunc.toHexString(can.getVehicleID()));
                    SLog.log(SLog.Debug, "Can", "canWatchList=" + StringFunc.toHexString(can.getWatchList()));

                    can.canOff();
                    can = null;
                } catch (IOException ioe) {
                    SLog.log(SLog.Error, "Can", "IOException " + ioe);
                }
            }
            
            long obd2 = Settings.getInstance().getSetting("obd2Interval", 0);
            if (obd2 != 0 && System.currentTimeMillis() / 1000L > lastObd2 + obd2) {
                lastObd2 = System.currentTimeMillis() / 1000L;
                can = new Can();
                try {

                    can.setCan("STD", 500, "STD", "ACTIVE");
                    can.setAddress("000007E8");
                    can.setAddress("000007E9");
                    can.canOn();

                    int[] bitmap;

                    SLog.log(SLog.Debug, "Can", "canState=" + can.canState());
                    SLog.log(SLog.Debug, "Can", "canMode=" + can.getCanMode());
                    SLog.log(SLog.Debug, "Can", "canNodeState=" + can.getCanNodeState());
                    SLog.log(SLog.Debug, "Can", "canSpeed=" + can.getCanSpeed());
                    SLog.log(SLog.Debug, "Can", "canType=" + can.getCanType());

                    // get vehicle information
                    bitmap = getObd2(9, 0x00);
                    for (int B = 0; B < 4; B++) {
                        for (int b = 7; b >= 0; b--) {
                            if ((bitmap[7 + B] & (0x01 << b)) != 0) {
                                getObd2(9, B * 8 + 7 - b);
                            }
                        }
                    }

                    // get current data
                    for (int block = 0x00; block < 0xE0; block += 0x20) {
                        bitmap = getObd2(1, block);
                        for (int B = 0; B < 4; B++) {
                            for (int b = 7; b >= 0; b--) {
                                if ((bitmap[7 + B] & (0x01 << b)) != 0) {
                                    getObd2(1, block + B * 8 + 7 - b);
                                }
                            }
                        }
                    }

                    // get diagnostic data
                    getObd2(3, 0);

                    can.canOff();
                    can = null;
                } catch (IOException ioe) {
                    SLog.log(SLog.Error, "Can", "IOException " + ioe);
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                //
            }
        }
    }

    private int[] getObd2(int mode, int pid) {
        int[] answer;
        answer = new int[0];

        try {
            //               Address      Len    Mode              PID              Filler
            String request = "000007DF" + "02" + hexString(mode) + hexString(pid) + "0000000000";
            SLog.log(SLog.Debug, "Can", "send=" + request);
            can.send(request);
            answer = can.getIntStandardData();
            int[] truncated = new int[7 + answer[4]];
            System.arraycopy(answer, 0, truncated, 0, truncated.length);
            SLog.log(SLog.Debug, "Can", "recv=" + StringFunc.toHexString(truncated));
        } catch (IOException ioe) {
            SLog.log(SLog.Error, "Can", "IOException " + ioe);
        }
        return answer;
    }

    private String hexString(int i) {
        String string = Integer.toHexString(i);
        if (string.length() < 2) {
            string = "0" + string;
        }
        return string;
    }
}
