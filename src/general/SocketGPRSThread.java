/*	
 * Class 	SocketGPRSThread
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

/**
 * Task that tales care of sending strings through a GPRS connection using a TCP
 * socket service
 *
 * @version	1.01 <BR> <i>Last update</i>: 05-10-2007
 * @author alessioza
 *
 */
public class SocketGPRSThread extends Thread implements GlobCost {

    final private int nothingSleep = 500;
    final private int errorSleep = 5000;
    final private int closingSleep = 2000;

    public boolean terminate = false;
    private boolean sending = false;
  
    public int creg = -1;
    public int cgreg = -1;
    
    private Queue gpsQ;

    public boolean isSending() {
        return sending;
    }

    public SocketGPRSThread() {
        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("TT*SocketGPRStask: CREATED");
        }
        gpsQ = new Queue(100, "gpsQ");
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
        return gpsQ.size();
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
        InfoStato.getInstance().setEnableCSD(true);
        ATManager.getInstance().executeCommandSynchron("at+cgatt=0\r");
        sending = false;
    }

    public void run() {
        while (!terminate) {
            AppMain.getInstance().userwareWatchDogTask.GPRSRunning = true;
            AppMain.getInstance().gpio6WatchDogTask.GPRSRunning = true;
            Publish publish = null;

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
}
