/*	
 * Class 	SocketGPRSThread
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class SocketGPRSThread extends Thread {

    private Timer MQTTTimeoutTimer = null;
    private TimerTask MQTTTimeoutTimerTask = null;
    private boolean MQTTTimeout;

    private Timer GPRSTimeoutTimer = null;
    private TimerTask GPRSTimeoutTimerTask = null;
    private boolean GPRSTimeout;

    final private int nothingSleep = 1000;
    final private int errorSleep = 5000;
    final private int closingSleep = 2000;

    public boolean terminate = false;
    private boolean network = false;
    private String operator = "";
    private String lastOperatorList = "";

    public int creg = -1;
    public int cgreg = -1;

    private final Queue gpsQ;

    private final Timer networkCheckTimer;
    private final TimerTask networkCheckTimerTask;
    private final int NetworkCheckLoop = 30;

    private final Timer providerCheckTimer;
    private final TimerTask providerCheckTimerTask;
    private final int ProviderCheckLoop = 300;

    public boolean isConnected() {
        return MQTTHandler.getInstance().isConnected();
    }

    public boolean isNetwork() {
        return network;
    }

    public String getOperator() {
        return operator;
    }

    public String getOperatorList() {
        return lastOperatorList;
    }

    public boolean isGPRSTimeout() {
        return GPRSTimeout;
    }

    public boolean isMQTTTimeout() {
        return MQTTTimeout;
    }

    public SocketGPRSThread() {
        gpsQ = new Queue(Settings.getInstance().getSetting("maxSize", 1024L * 1024L), "gpsQ");

        networkCheckTimer = new Timer();
        networkCheckTimerTask = new NetworkCheckTimerTask();
        networkCheckTimer.schedule(networkCheckTimerTask, 0, NetworkCheckLoop * 1000);

        providerCheckTimer = new Timer();
        providerCheckTimerTask = new ProviderCheckTimerTask();
        providerCheckTimer.schedule(providerCheckTimerTask, 0, ProviderCheckLoop * 1000);

        startTimeoutTimer();
    }

    class GPRSTimeout extends TimerTask {

        public void run() {
            SLog.log(SLog.Informational, "SocketGRPSThread", "gprsTimeout");
            GPRSTimeout = true;
        }
    }

    class MQTTTimeout extends TimerTask {

        public void run() {
            SLog.log(SLog.Informational, "SocketGRPSThread", "mqttTimeout");
            MQTTTimeout = true;
        }
    }

    private synchronized void startTimeoutTimer() {
        stopTimeoutTimer();

        GPRSTimeoutTimer = new Timer();
        GPRSTimeoutTimerTask = new GPRSTimeout();
        GPRSTimeoutTimer.schedule(GPRSTimeoutTimerTask,
                Settings.getInstance().getSetting("gprsTimeout", 600) * 1000);

        MQTTTimeoutTimer = new Timer();
        MQTTTimeoutTimerTask = new MQTTTimeout();
        MQTTTimeoutTimer.schedule(MQTTTimeoutTimerTask,
                Settings.getInstance().getSetting("mqttTimeout", 600) * 1000);

        SLog.log(SLog.Debug, "SocketGRPSThread", "start gprsTimeout  & mqttTimeout timer");
    }

    private synchronized void stopTimeoutTimer() {
        if (GPRSTimeoutTimer != null) {
            GPRSTimeoutTimer.cancel();
        }
        if (MQTTTimeoutTimer != null) {
            MQTTTimeoutTimer.cancel();
        }
        GPRSTimeout = false;
        MQTTTimeout = false;
    }

    public synchronized static SocketGPRSThread getInstance() {
        return SocketGPRSThreadHolder.INSTANCE;
    }

    private static class SocketGPRSThreadHolder {

        private static final SocketGPRSThread INSTANCE = new SocketGPRSThread();
    }

    class Publish {

        public String topic;
        public byte[] payload;
        public boolean retain;
        public int qos;

        byte[] serialize() {
            JSONObject json = new JSONObject();
            try {
                json.put("retain", retain);
                json.put("qos", qos);
                json.put("topic", topic);
                json.put("payload", new String(payload));
            } catch (JSONException je) {
                //
            }
            SLog.log(SLog.Debug, "SocketGPRSThread",
                    "JSON put " + json.toString());
            byte[] bytes = json.toString().getBytes();
            /*
             byte[] bytes = new byte[4 + topic.getBytes().length + payload.length];
             bytes[0] = (byte) (retain ? 1 : 0);
             bytes[1] = (byte) (qos % 3);
             byte[] topicBytes = topic.getBytes();
             bytes[2] = (byte) (topicBytes.length);
             bytes[3] = (byte) (payload.length);
             System.arraycopy(topicBytes, 0, bytes, 4, topicBytes.length);
             System.arraycopy(payload, 0, bytes, 4 + topicBytes.length, payload.length);
             SLog.log(SLog.Debug, "SocketGPRSThread",
             "Packed put " + bytes.length + " " + bytes[0] + " " + bytes[1] + " " + bytes[2] + " " + bytes[3]);
             */
            return bytes;
        }
    }

    Publish deserialize(byte[] bytes) {
        Publish publish = null;
        if (bytes != null) {
            try {
                JSONObject json = new JSONObject(new String(bytes));
                SLog.log(SLog.Debug, "SocketGPRSThread",
                        "JSON get " + json.toString());

                publish = new Publish();
                publish.retain = json.getBoolean("retain");
                publish.qos = json.getInt("qos");
                publish.topic = json.getString("topic");
                publish.payload = json.getString("payload").getBytes();
            } catch (JSONException je) {
                SLog.log(SLog.Debug, "SocketGPRSThread",
                        "Packed get " + bytes.length + " " + bytes[0] + " " + bytes[1] + " " + bytes[2] + " " + bytes[3]);

                publish = new Publish();
                publish.retain = (bytes[0] == 1);
                publish.qos = bytes[1];
                int slen = bytes[2] >= 0 ? bytes[2] : bytes[2] + 256;
                publish.topic = new String(bytes, 4, slen);
                int plen = bytes[3] >= 0 ? bytes[3] : bytes[3] + 256;
                publish.payload = new byte[plen];
                System.arraycopy(bytes, 4 + slen, publish.payload, 0, plen);
            }
        }
        return publish;
    }

    public synchronized boolean put(String topic, int qos, boolean retain, byte[] payload) {
        Publish publish = new Publish();
        publish.topic = topic;
        publish.payload = payload;
        publish.retain = retain;
        publish.qos = qos;

        boolean putResult = gpsQ.put(publish.serialize());
        return putResult;
    }

    public synchronized int qSize() {
        return gpsQ.size();
    }

    public void open() {
        ATManager.getInstance().executeCommandSynchron("at^smong\r");

        String cgatt;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                //
            }
            ATManager.getInstance().executeCommandSynchron("at+cgatt=0\r");
            cgatt = ATManager.getInstance().executeCommandSynchron("at+cgatt=1\r");
        } while (cgatt.indexOf("ERROR") >= 0);

        if (!MQTTHandler.getInstance().isConnected()) {
            MQTTHandler.getInstance().init(
                    Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI()),
                    Settings.getInstance().getSetting("host", "tcp://localhost") + ":" + Settings.getInstance().getSetting("port", 1883),
                    Settings.getInstance().getSetting("user", null),
                    Settings.getInstance().getSetting("password", null),
                    Settings.getInstance().getSetting("willTopic",
                            Settings.getInstance().getSetting("publish", "owntracks/gw/")
                            + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                            + "/status"),
                    Settings.getInstance().getSetting("will", "0").getBytes(),
                    Settings.getInstance().getSetting("willQos", 1),
                    Settings.getInstance().getSetting("willRetain", true),
                    Settings.getInstance().getSetting("keepAlive", 60),
                    Settings.getInstance().getSetting("cleanSession", false),
                    Settings.getInstance().getSetting("subscription",
                            Settings.getInstance().getSetting("publish", "owntracks/gw/")
                            + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI()) + "/cmd"),
                    Settings.getInstance().getSetting("subscriptionQos", 2)
            );
            MQTTHandler.getInstance().connectToBroker();
        }
    }

    public void close() {
        MQTTHandler.getInstance().disconnect();
        ATManager.getInstance().executeCommandSynchron("at+cgatt=0\r");
    }

    public void run() {
        while (!terminate) {
            AppMain.getInstance().userwareWatchDogTask.GPRSRunning = true;
            AppMain.getInstance().gpio6WatchDogTask.GPRSRunning = true;

            if (!MQTTHandler.getInstance().isConnected()) {
                open();
            }
            if (MQTTHandler.getInstance().isConnected()) {
                Publish publish = deserialize(gpsQ.get());
                if (publish != null) {
                    if (processMessage(publish)) {
                        gpsQ.consume();
                    } else {
                        try {
                            Thread.sleep(errorSleep);
                        } catch (InterruptedException e) {
                        }
                    }
                } else {
                    try {
                        Thread.sleep(nothingSleep);
                    } catch (InterruptedException e) {
                    }
                }
            }
            Thread.yield();
        }

        close();
    }

    boolean processMessage(Publish publish) {
        SLog.log(SLog.Debug, "SocketGRPSThread", "processMessage: " + publish.topic);
        MQTTHandler.getInstance().publish(publish.topic, publish.qos, publish.retain, publish.payload);
        return MQTTHandler.getInstance().isConnected();

    }

    class NetworkCheckTimerTask extends TimerTask {

        public void run() {
            String response = ATManager.getInstance().executeCommandSynchron("AT+COPS?\r");
            String[] lines = StringFunc.split(response, "\r\n");
            if (lines.length >= 2) {
                final String COPS = "+COPS: ";
                if (lines[1].startsWith(COPS)
                        && lines[1].length() > COPS.length()) {
                    String[] values = StringFunc.split(lines[1].substring(COPS.length()), ",");
                    if (values.length == 3) {
                        stopTimeoutTimer();
                        network = true;
                        operator = values[2];
                    } else {
                        if (network) {
                            startTimeoutTimer();
                        }
                        network = false;
                        operator = "";
                    }
                } else {
                    if (network) {
                        startTimeoutTimer();
                    }
                    network = false;
                    operator = "";
                }
            } else {
                if (network) {
                    startTimeoutTimer();
                }
                network = false;
                operator = "";
            }
            response = ATManager.getInstance().executeCommandSynchron("AT+CSQ\r");
        }
    }

    class ProviderCheckTimerTask extends TimerTask {
        //  AT+COPS=?\r\r\n+COPS: (2,"Vodafone.de",,"26202"),(1,"o2 - de",,"26207"),(1,"E-Plus",,"26203"),(1,"Telekom.de",,"26201"),,(0-4),(0,2)\r\n\r\nOK\r\n

        public void run() {
            String response = ATManager.getInstance().executeCommandSynchron("AT+COPS=?\r");
            String currentOperator = "";
            String availableOperators = "";
            String forbiddenOperators = "";
            String unknownOperators = "";
            final String cops = "+COPS: ";
            if (response.indexOf(cops) != -1) {
                int pos = response.indexOf(cops) + cops.length();
                while (response.substring(pos, pos + 1).equals("(")) {
                    int end = response.indexOf(")", pos);
                    if (end != -1) {
                        String[] values = StringFunc.split(response.substring(pos + 1, end), ",");
                        if (values.length == 4) {
                            String operatorNumber = values[3].substring(1, values[3].length() - 1);
                            String operatorName = values[1].substring(1, values[1].length() - 1);
                            switch (Integer.parseInt(values[0])) {
                                case 1:
                                    availableOperators = availableOperators.concat(" +" + operatorNumber);
                                    break;
                                case 2:
                                    currentOperator = operatorNumber;
                                    break;
                                case 3:
                                    forbiddenOperators = forbiddenOperators.concat(" -" + operatorNumber);
                                    break;
                                case 0:
                                default:
                                    unknownOperators = unknownOperators.concat(" ?" + operatorNumber);
                                    break;
                            }
                        }
                    }
                    pos = end + 2;
                }
            }
            String operatorList = currentOperator + availableOperators + forbiddenOperators + unknownOperators;
            if (!lastOperatorList.equals(operatorList)) {
                put(
                        Settings.getInstance().getSetting("publish", "owntracks/gw/")
                        + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                        + "/operators",
                        Settings.getInstance().getSetting("qos", 1),
                        Settings.getInstance().getSetting("retain", true),
                        operatorList.getBytes()
                );
                lastOperatorList = operatorList;
            }
        }
    }
}
