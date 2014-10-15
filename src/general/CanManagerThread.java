/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import choral.io.Can;
import java.io.IOException;
import java.util.Hashtable;

/**
 *
 * @author christoph
 */
public class CanManagerThread extends Thread {

    Can can;
    public boolean terminate = false;
    Hashtable hashtable;

    private long lastFms = 0;
    private long lastObd2 = 0;
    private long lastSensors = 0;

    private CanManagerThread() {
        hashtable = new Hashtable();
    }

    public static CanManagerThread getInstance() {
        return CanManagerHolder.INSTANCE;
    }

    private static class CanManagerHolder {

        private static final CanManagerThread INSTANCE = new CanManagerThread();
    }

    class canResult {

        public int[] data8;
        public String payload;
    }

    public void run() {
        while (!terminate) {
            if (Settings.getInstance().getSetting("fms", false)) {
                can = new Can();
                try {
                    can.canOff();
                    can.setCan("EXT", 250, "FMS", "SILENT");
                    can.canOn();

                    SLog.log(SLog.Debug, "Can", "canState=" + can.canState());
                    SLog.log(SLog.Debug, "Can", "canMode=" + can.getCanMode());
                    SLog.log(SLog.Debug, "Can", "canSpeed=" + can.getCanSpeed());
                    SLog.log(SLog.Debug, "Can", "canType=" + can.getCanType());
                    SLog.log(SLog.Debug, "Can", "canNodeState=" + can.getCanNodeState());

                    while (!terminate && Settings.getInstance().getSetting("fms", false)) {
                        SLog.log(SLog.Debug, "Can", "canDriverID=" + StringFunc.toHexString(can.getDriverID()));
                        cacheAndPut("/fms/driverid", StringFunc.toHexString(can.getDriverID()));
                        SLog.log(SLog.Debug, "Can", "canTimeDate=" + StringFunc.toHexString(can.getTimeDate()));
                        cacheAndPut("/fms/timedate", StringFunc.toHexString(can.getTimeDate()));
                        SLog.log(SLog.Debug, "Can", "canVehicleID=" + StringFunc.toHexString(can.getVehicleID()));
                        cacheAndPut("/fms/vehicl) {\n" +
"                        SLog.log(SLogeid", StringFunc.toHexString(can.getVehicleID()));
                        SLog.log(SLog.Debug, "Can", "canFMSData=" + StringFunc.toHexString(can.getFMSdata()));
                        cacheAndPut("/fms/data", StringFunc.toHexString(can.getFMSdata()));
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            //
                        }
                    }
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
                    can.canOff();
                    can.deleteAllAddress();

                    can.setCan(Settings.getInstance().getSetting("obd2Mode", "STD"),
                            Settings.getInstance().getSetting("obd2Speed", 500),
                            "STD",
                            "ACTIVE");

                    String odb2Addresses = Settings.getInstance().getSetting("obd2Addresses",
                            "000007e8,000007e9,000007ea,000007eb,000007ec,000007ed,000007ee,000007ef");
                    String[] addresses = StringFunc.split(odb2Addresses, ",");
                    for (int i = 0; i < addresses.length; i++) {
                        can.setAddress(addresses[i]);
                    }
                    can.canOn();

                    SLog.log(SLog.Debug, "CanRaw", "canState=" + can.canState());
                    SLog.log(SLog.Debug, "CanRaw", "canMode=" + can.getCanMode());
                    SLog.log(SLog.Debug, "CanRaw", "canNodeState=" + can.getCanNodeState());
                    SLog.log(SLog.Debug, "CanRaw", "canSpeed=" + can.getCanSpeed());
                    SLog.log(SLog.Debug, "CanRaw", "canType=" + can.getCanType());
                    SLog.log(SLog.Debug, "CanRAw", "canWatchList=" + StringFunc.toHexString(can.getWatchList()));

                    String ecus = "";
                    for (int i = 0; i < addresses.length; i++) {
                        canResult result = getObd2(true, addresses[i], "0900");
                        if (result.data8 != null) {
                            if (ecus.length() > 0) {
                                ecus = ecus.concat(" ");
                            }
                            ecus = ecus.concat(addresses[i]);
                            getEcu(addresses[i]);
                        }
                    }

                    can.canOff();
                    can = null;
                } catch (IOException ioe) {
                    SLog.log(SLog.Error, "Can", "IOException " + ioe);
                }
            }

            long sensors = Settings.getInstance().getSetting("obd2Sensors", 0);
            if (sensors != 0 && System.currentTimeMillis() / 1000L > lastSensors + sensors) {
                lastSensors = System.currentTimeMillis() / 1000L;
                can = new Can();
                try {
                    can.canOff();
                    can.deleteAllAddress();

                    can.setCan(Settings.getInstance().getSetting("obd2Mode", "STD"),
                            Settings.getInstance().getSetting("obd2Speed", 500),
                            "STD",
                            "ACTIVE");

                    String odb2Addresses = Settings.getInstance().getSetting("obd2Addresses",
                            "000007e8,000007e9,000007ea,000007eb,000007ec,000007ed,000007ee,000007ef");
                    String[] addresses = StringFunc.split(odb2Addresses, ",");
                    for (int i = 0; i < addresses.length; i++) {
                        can.setAddress(addresses[i]);
                    }
                    can.canOn();

                    SLog.log(SLog.Debug, "CanRaw", "canState=" + can.canState());
                    SLog.log(SLog.Debug, "CanRaw", "canMode=" + can.getCanMode());
                    SLog.log(SLog.Debug, "CanRaw", "canNodeState=" + can.getCanNodeState());
                    SLog.log(SLog.Debug, "CanRaw", "canSpeed=" + can.getCanSpeed());
                    SLog.log(SLog.Debug, "CanRaw", "canType=" + can.getCanType());
                    SLog.log(SLog.Debug, "CanRAw", "canWatchList=" + StringFunc.toHexString(can.getWatchList()));

                    for (int i = 0; i < addresses.length; i++) {
                        canResult result = getObd2(true, addresses[i], "0900");
                        if (result.data8 != null) {
                            getSensors(addresses[i]);
                        }
                    }

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

    private void getEcu(String address) {
        canResult result;
        result = getObd2(true, address, "090001");
        if (result.data8 != null) {
            int[] four = new int[4];
            System.arraycopy(result.data8, 4 + 4, four, 0, four.length);
            String pids = StringFunc.toHexString(four);
            cacheAndPut("/obd2/" + address + "/09/00", result.payload.substring(6));
            getPids(address, 0x09, 0x00, Long.parseLong(pids, 16), "");
        }

        getSensors(address);

        result = getObd2(true, address, "0101");
        if (result.data8 != null) {
            if (result.data8[4] == 6) {
                int numDtc = result.data8[4 + 3] & 0x7f;
                for (int dtc = 1; dtc <= numDtc; dtc++) {
                    int base = 0;
                    result = getObd2(true, address, "02" + hexString(base) + hexString(dtc));
                    if (result.data8 != null) {
                        int[] four = new int[4];
                        System.arraycopy(result.data8, 4 + 4, four, 0, four.length);
                        String pids = StringFunc.toHexString(four);
                        cacheAndPut("/obd2/" + address + "/02/" + hexString(base) + "/" + hexString(dtc),
                                result.payload.substring(6));
                        getPids(address, 0x02, base, Long.parseLong(pids, 16), hexString(dtc));
                        if ((four[3] & 0x01) == 0x01) {
                            base += 0x20;
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        result = getObd2(true, address, "03");
        if (result.data8 != null) {
            cacheAndPut("/obd2/" + address + "/03", result.payload.substring(2));
        }
    }

    private void getSensors(String address) {
        canResult result;
        int base = 0;

        while (true) {
            result = getObd2(true, address, "01" + hexString(base));
            if (result.data8 != null) {
                int[] four = new int[4];
                System.arraycopy(result.data8, 4 + 3, four, 0, four.length);
                String pids = StringFunc.toHexString(four);
                cacheAndPut("/obd2/" + address + "/01/" + hexString(base), result.payload.substring(4));
                getPids(address, 0x01, base, Long.parseLong(pids, 16), "");
                if ((four[3] & 0x01) == 0x01) {
                    base += 0x20;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private void getPids(String address, int mode, int base, long pids, String payload) {
        for (int p = 31; p >= 0; p--) {
            if ((pids & (1 << p)) != 0) {
                canResult result = getObd2(true, address, hexString(mode) + hexString(base + (32 - p)) + payload);
                if (result.data8 != null) {
                    cacheAndPut("/obd2/" + address + "/" + hexString(mode) + "/" + hexString(base + (32 - p))
                            + ((payload.length() > 0) ? ("/" + payload) : ""),
                            result.payload.substring((payload.length() > 0) ? 6 : 4));
                }
            }
        }
    }

    private canResult getObd2(boolean broadcast, String address, String payload) {
        SLog.log(SLog.Debug, "Can", "getObd2 " + address + ":" + payload);

        canResult result = new canResult();
        result.data8 = null;
        result.payload = "";
        int retries = 4;

        while (retries > 0) {
            retries--;
            int[] raw = getObd2RawNomatch(broadcast, address, payload);
            if (raw != null) {
                if (raw[4] < 8) {
                    String hexString = StringFunc.toHexString(raw);
                    if (hexString.substring(11, 12).equals(payload.substring(1, 2))
                            && hexString.substring(12).startsWith(payload.substring(2))) {
                        result.payload = hexString.substring(10, 10 + raw[4] * 2);
                        result.data8 = raw;
                    }
                    return result;
                } else if ((raw[4] & 0xf0) == 0x10) {
                    result.data8 = raw;
                    int length = (raw[4] * 256 + raw[5]) & 0x0fff;
                    int[] multiframe = new int[length];
                    System.arraycopy(raw, 6, multiframe, 0, 6);
                    int fill = 6;
                    SLog.log(SLog.Debug, "Can", "multi " + length);
                    int next = 1;

                    while (length > fill) {
                        SLog.log(SLog.Debug, "Can", "multi to go " + (length - fill));
                        retries = 4;

                        while (retries > 0) {
                            retries--;
                            result.data8 = null;
                            int[] multiframeData = getObd2RawNomatch(false, address, "300100");
                            if (multiframeData != null) {
                                int seq = multiframeData[4];
                                if ((seq & 0xf0) == 0x20) {
                                    result.data8 = multiframeData;
                                    if ((seq & 0x0f) == next) {
                                        int len = (length - fill >= 7) ? 7 : (length - fill);
                                        System.arraycopy(multiframeData, 4 + 1, multiframe, fill, len);
                                        fill += len;
                                        SLog.log(SLog.Debug, "Can", "multi got " + fill);
                                        next++;
                                        next &= 0x0f;
                                        if (length == fill) {
                                            break;
                                        }
                                    }
                                } else if ((seq & 0xf0) != 0x10) {
                                    length = 0;
                                }
                            }
                            if (retries == 0) {
                                length = 0;
                            }
                        }
                    }
                    if (length == 0) {
                        SLog.log(SLog.Debug, "Can", "multi abort");
                        result.data8 = null;
                        result.payload = "";
                        return result;
                    }
                    SLog.log(SLog.Debug, "Can", "multi done");
                    result.payload = StringFunc.toHexString(multiframe);
                    return result;
                }
            } else {
                SLog.log(SLog.Debug, "Can", "getObd2 again");
                result.data8 = null;
                result.payload = "";
            }
        }
        SLog.log(SLog.Debug, "Can", "getObd2=" + result.payload);
        return result;
    }

    private int[] getObd2RawNomatch(boolean broadcast, String address, String payload) {
        SLog.log(SLog.Debug, "CanRaw", "getObd2RawNomatch " + address + ":" + payload);
        String sendAddress = address.substring(0, 6);
        int i = Integer.parseInt(address.substring(6), 16);
        i &= 0xf7;
        sendAddress = sendAddress.concat(hexString(i));

        int retries = 4;

        while (retries > 0) {
            retries--;
            try {
                String request = (broadcast ? "000007df" : sendAddress)
                        + (broadcast ? hexString(payload.length() / 2) : "") + payload;

                while (request.length() < ((4 + 8) * 2)) {
                    request = request.concat("55");
                }

                //DEBUG
                SLog.log(SLog.Debug, "CanRaw", "xxxx= --------");
                SLog.log(SLog.Debug, "CanRaw", "send= " + request.substring(0, 8)
                        + " " + request.substring(8, 10)
                        + " " + request.substring(10)
                );
                //

                can.send(request);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    //
                }
                int[] answer = can.getIntStandardData();

                //DEBUG
                for (int D = 0; D < answer.length; D += 4 + 8) {
                    if (answer[D + 0] + answer[D + 1] + answer[D + 2] + answer[D + 3] > 0) {
                        int[] truncated = new int[5 + 7];
                        System.arraycopy(answer, D, truncated, 0, truncated.length);
                        String hexString = StringFunc.toHexString(truncated);
                        SLog.log(SLog.Debug, "CanRaw", "recv= " + hexString.substring(0, 8)
                                + " " + hexString.substring(8, 10)
                                + " " + hexString.substring(10)
                        );
                    }
                }
                //

                for (int D = 0; D < answer.length; D += 4 + 8) {
                    int[] truncated = new int[5 + 7];
                    System.arraycopy(answer, D, truncated, 0, truncated.length);
                    String hexString = StringFunc.toHexString(truncated);
                    if (hexString.startsWith(address)) {
                        SLog.log(SLog.Debug, "CanRaw", "getObd2RawNomatch=" + hexString);
                        return truncated;
                    }
                }
            } catch (IOException ioe) {
                SLog.log(SLog.Error, "Can", "IOException " + ioe);
            }
        }
        SLog.log(SLog.Debug, "CanRaw", "getObd2RawNomatch=null");
        return null;
    }

    private String hexString(int i) {
        String string = Integer.toHexString(i);
        if (string.length() < 2) {
            string = "0" + string;
        }
        return string;
    }

    private void cacheAndPut(String subtopic, String payload) {
        String topic = Settings.getInstance().getSetting("publish", "owntracks/gw/")
                + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                + subtopic;

        String value = (String) hashtable.get(topic);
        if (value == null || !value.equals(payload)) {
            SocketGPRSThread.getInstance().put(
                    topic,
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", true),
                    payload.getBytes()
            );
            hashtable.put(topic, payload);
        }
    }
}
