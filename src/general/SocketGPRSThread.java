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

    public boolean isSending() {
        return sending;
    }

    public SocketGPRSThread() {
        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("TT*SocketGPRStask: CREATED");
        }
    }

    boolean open() {
        try {
            SemAT.getInstance().getCoin(5);
            InfoStato.getInstance().writeATCommand("at^smong\r");
            InfoStato.getInstance().writeATCommand("at+cgatt=1\r");
            SemAT.getInstance().putCoin();
        } catch (Exception e) {
        }

        if (!MQTTHandler.getInstance().isConnected()) {
            SemAT.getInstance().getCoin(5);
            MQTTHandler.getInstance().init(
                    Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI()),
                    Settings.getInstance().getSetting("host", "tcp://localhost") + ":" + Settings.getInstance().getSetting("port", 1883),
                    Settings.getInstance().getSetting("user", null),
                    Settings.getInstance().getSetting("password", null),
                    Settings.getInstance().getSetting("willTopic",
                            Settings.getInstance().getSetting("publish", "owntracks/gw/")
                            + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI())
                            + "/status"),
                    Settings.getInstance().getSetting("will", "0").getBytes(),
                    Settings.getInstance().getSetting("willQos", 1),
                    Settings.getInstance().getSetting("willRetain", true),
                    Settings.getInstance().getSetting("keepAlive", 60),
                    Settings.getInstance().getSetting("cleanSession", true),
                    Settings.getInstance().getSetting("subscription",
                            Settings.getInstance().getSetting("publish", "owntracks/gw/")
                            + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI()) + "/cmd"),
                    Settings.getInstance().getSetting("subscriptionQos", 1)
            );
            MQTTHandler.getInstance().connectToBroker();
            SemAT.getInstance().putCoin();
        }
        return MQTTHandler.getInstance().isConnected();
    }

    void close() {
        SemAT.getInstance().getCoin(5);
        MQTTHandler.getInstance().disconnect();
        SemAT.getInstance().putCoin();

        InfoStato.getInstance().setEnableCSD(true);

        SemAT.getInstance().getCoin(5);
        InfoStato.getInstance().writeATCommand("at+cgatt=0\r");
        SemAT.getInstance().putCoin();
    }

    public void run() {

        while (!terminate) {
            String message = null;

            if (Settings.getInstance().getSetting("generalDebug", false)) {
                System.out.println("SocketGPRS tracking " + Settings.getInstance().getSetting("tracking", false));
            }
            if (!sending) {
                sending = open();
            }
            if (sending) {
                if (message == null) {
                    message = (String) InfoStato.getInstance().gpsQ.get();
                }
                if (message != null) {
                    if (processMessage(message)) {
                        message = null;
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
            AppMain.getInstance().watchDogTask.GPRSRunning = true;
            Thread.yield();
        }
        close();
    }

    boolean processMessage(String message) {
        if (Settings.getInstance().getSetting("raw", true)) {
            SemAT.getInstance().getCoin(5);
            MQTTHandler.getInstance().publish(Settings.getInstance().getSetting("publish", "owntracks/gw/")
                    + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI())
                    + "/raw",
                    Settings.getInstance().getSetting("qos", 1),
                    Settings.getInstance().getSetting("retain", true),
                    message.getBytes());
            SemAT.getInstance().putCoin();
        }

        if (!MQTTHandler.getInstance().isConnected()) {
            return false;
        }

        LocationManager.getInstance().setMinDistance(Settings.getInstance().getSetting("minDistance", 0));
        LocationManager.getInstance().setMaxInterval(Settings.getInstance().getSetting("maxInterval", 0));
        LocationManager.getInstance().setMinSpeed(Settings.getInstance().getSetting("minSpeed", 0));

        if (LocationManager.getInstance().handleNMEAString(message)) {
            String[] fields = StringSplitter.split(
                    Settings.getInstance().getSetting("fields", "course,speed,altitude,distance,battery"), ",");
            String json = LocationManager.getInstance().getJSONString(fields);
            if (json != null) {
                SemAT.getInstance().getCoin(5);
                MQTTHandler.getInstance().publish(Settings.getInstance().getSetting("publish", "owntracks/gw/")
                        + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI()),
                        Settings.getInstance().getSetting("qos", 1),
                        Settings.getInstance().getSetting("retain", true),
                        json.getBytes());
                SemAT.getInstance().putCoin();
                if (!MQTTHandler.getInstance().isConnected()) {
                    return false;
                }
            }
        }
        return true;
    }
}
