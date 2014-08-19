/*	
 * Class 	SocketGPRSThread
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Task that tales care of sending strings through a GPRS connection using a TCP
 * socket service
 *
 * @version	1.01 <BR> <i>Last update</i>: 05-10-2007
 * @author alessioza
 *
 */
public class SocketGPRSThread extends Thread {

    private Timer timeoutTimer = null;
    private TimerTask timeoutTimerTask = null;
    private boolean timeout;

    final private int nothingSleep = 500;
    final private int errorSleep = 5000;
    final private int closingSleep = 2000;

    public boolean terminate = false;
    private boolean sending = false;
    private boolean network = false;
    private String operator = "";
    
    public int creg = -1;
    public int cgreg = -1;
    
    private final Queue gpsQ;
    private Publish publish = null;

    private final Timer timer;
    private final TimerTask timerTask;
    private final int NetworkCheckLoop = 30;

    public boolean isSending() {
        return sending;
    }

    public boolean isNetwork() {
        return network;
    }
    
    public String getOperator() {
        return operator;
    }

    public boolean isTimeout() {
        return timeout;
    }
    
    public SocketGPRSThread() {
        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("TT*SocketGPRStask: CREATED");
        }
        gpsQ = new Queue(100, "gpsQ");
        
        timer = new Timer();
        timerTask = new NetworkCheckTimerTask();
        timer.schedule(timerTask, 0, NetworkCheckLoop * 1000);
        
        startTimeoutTimer();
    }

    class GPRSTimeout extends TimerTask {

        public void run() {
            if (Settings.getInstance().getSetting("gprsDebug", false)) {
                System.out.println("GPRSTimeout");
            }
            timeout = true;
        }
    }

    private void startTimeoutTimer() {
        stopTimeoutTimer();
        timeoutTimer = new Timer();
        timeoutTimerTask = new GPRSTimeout();
        timeoutTimer.schedule(timeoutTimerTask, Settings.getInstance().getSetting("gprsTimeout", 600) * 1000);
        if (Settings.getInstance().getSetting("gprsDebug", false)) {
            System.out.println("start gprsTimeout timer");
        }
    }

    private void stopTimeoutTimer() {
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
        }
        timeout = false;
    }


    public static SocketGPRSThread getInstance() {
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
    }
    
    public synchronized boolean put(String topic, int qos, boolean retain, byte[] payload) {
        Publish publish = new Publish();
        publish.topic = topic;
        publish.payload = payload;
        publish.retain = retain;
        publish.qos = qos;
        boolean putResult = gpsQ.put(publish);
        if (Settings.getInstance().getSetting("gprsDebug", false)) {
            System.out.println("gpsQ.size " + qSize());
        }
        return putResult;
    }
    
    public synchronized int qSize() {
        return gpsQ.size() + ((publish == null) ? 0 : 1);
    }
    
    public void open() {
        ATManager.getInstance().executeCommandSynchron("at^smong\r");
        try {
            Thread.sleep(1001);
        } catch (InterruptedException ie) {
            //
        }
        ATManager.getInstance().executeCommandSynchron("at+cgatt=1\r");

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
                    Settings.getInstance().getSetting("cleanSession", true),
                    Settings.getInstance().getSetting("subscription",
                            Settings.getInstance().getSetting("publish", "owntracks/gw/")
                            + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI()) + "/cmd"),
                    Settings.getInstance().getSetting("subscriptionQos", 1)
            );
            MQTTHandler.getInstance().connectToBroker();
        }
        sending = MQTTHandler.getInstance().isConnected(); 
    }

    public void close() {
        MQTTHandler.getInstance().disconnect();
        ATManager.getInstance().executeCommandSynchron("at+cgatt=0\r");
        sending = false;
    }

    public void run() {
        while (!terminate) {
            AppMain.getInstance().userwareWatchDogTask.GPRSRunning = true;
            AppMain.getInstance().gpio6WatchDogTask.GPRSRunning = true;

            if (!sending) {
                open();
            }
            if (sending) {
                if (publish == null) {
                    if (Settings.getInstance().getSetting("gprsDebug", false)) {
                        System.out.println("gpsQ.size " + qSize());
                    }
                    publish = (Publish)gpsQ.get();
                    if (Settings.getInstance().getSetting("gprsDebug", false)) {
                        System.out.println("gpsQ.size " + qSize());
                    }
                }
                if (publish != null) {
                    if (processMessage(publish)) {
                        publish = null;
                    } else {
                        sending = false;
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
        if (Settings.getInstance().getSetting("gprsDebug", false)) {
            System.out.println("processMessage: " + publish.topic);
        }
        MQTTHandler.getInstance().publish(publish.topic, publish.qos, publish.retain, publish.payload);
        return MQTTHandler.getInstance().isConnected();
    }
    
    class NetworkCheckTimerTask extends TimerTask {
        public void run() {
            String response = ATManager.getInstance().executeCommandSynchron("AT+COPS?\r");
            final String COPS = "+COPS: ";
            if (response.indexOf(COPS) >= 0 &&
                    response.length() > response.indexOf(COPS) + COPS.length()) {
                String[] values = StringSplitter.split(response.substring(response.indexOf(COPS) + COPS.length()), ",");
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
        }
    }

}
