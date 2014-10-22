/* Class 	AppMain
 * 
 * This software is developed for Choral devices with Java
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import choral.io.CheckUpgrade;
import com.cinterion.io.BearerControl;
import com.m2mgo.util.GPRSConnectOptions;
import java.util.Date;
import javax.microedition.midlet.*;

/**
 * Main application class.
 * <BR>
 * startApp() method contains the main body of the application.
 *
 * @version	1.00 <BR> <i>Last update</i>: 28-05-2008
 * @author matteobo
 */
public class AppMain extends MIDlet {

    public boolean invalidSIM = false;

    final public static String ignitionWakeup = "IgnitionWakeup";
    final public static String accelerometerWakeup = "AccelerometerWakeup";
    final public static String alarmClockWakeup = "AlarmClockWakeup";
    public String wakeupMode = accelerometerWakeup;

    UserwareWatchDogTask userwareWatchDogTask;
    GPIO6WatchDogTask gpio6WatchDogTask;

    static AppMain appMain;

    public static AppMain getInstance() {
        return AppMain.appMain;
    }

    public AppMain() {
    }

    protected void startApp() throws MIDletStateChangeException {
        AppMain.appMain = this;
        CheckUpgrade fw = new CheckUpgrade("");

        Settings settings = Settings.getInstance();
        SLog.log(SLog.Informational, "AppMain", "Running "
                + MicroManager.getInstance().getIMEI()
                + " " + getAppProperty("MIDlet-Version")
                + " " + DateFormatter.isoString(new Date()));

        ATManager.getInstance();
        CommASC0Thread.getInstance();
        SocketGPRSThread.getInstance();
        CommGPSThread.getInstance();
        if (MicroManager.getInstance().isAdvanced()) {
            CanManagerThread.getInstance();
        }
        ProcessSMSThread.setup();

        SocketGPRSThread.getInstance().put(
                settings.getSetting("publish", "owntracks/gw/")
                + settings.getSetting("clientID", MicroManager.getInstance().getIMEI())
                + "/start",
                settings.getSetting("qos", 1),
                settings.getSetting("retain", true),
                (MicroManager.getInstance().getIMEI()
                + " " + getAppProperty("MIDlet-Version")
                + " " + DateFormatter.isoString(new Date())).getBytes()
        );

        BearerControl.addListener(Bearer.getInstance());
        try {
            ATManager.getInstance().executeCommandSynchron("AT\r");

            String pin = Settings.getInstance().getSetting("pin", "");
            if (pin.length() > 0) {
                ATManager.getInstance().executeCommandSynchron("at+cpin=" + pin + "\r");
            }

            GPIOManager.getInstance();

            SLog.log(SLog.Debug, "AppMain", "watchdogs starting");

            userwareWatchDogTask = new UserwareWatchDogTask();
            gpio6WatchDogTask = new GPIO6WatchDogTask();

            SLog.log(SLog.Debug, "AppMain", "Set AUTOSTART...");
            ATManager.getInstance().executeCommandSynchron("at^scfg=\"Userware/Autostart/AppName\",\"\",\"a:/app/"
                    + AppMain.getInstance().getAppProperty("MIDlet-Name") + ".jar\"\r");
            ATManager.getInstance().executeCommandSynchron("at^scfg=\"Userware/Autostart/Delay\",\"\",10\r");
            ATManager.getInstance().executeCommandSynchron("at^scfg=\"Userware/Autostart\",\"\",\"1\"\r");
            if (Settings.getInstance().getSetting("usbDebug", false)) {
                ATManager.getInstance().executeCommandSynchron("at^scfg=\"Userware/StdOut\",USB\r");
            } else {
                ATManager.getInstance().executeCommandSynchron("at^scfg=\"Userware/StdOut\",ASC0\r");
            }

            ATManager.getInstance().executeCommandSynchron("AT^SBC=5000\r");

            ATManager.getInstance().executeCommandSynchron("at^sjnet="
                    + "\"" + GPRSConnectOptions.getConnectOptions().getBearerType() + "\","
                    + "\"" + GPRSConnectOptions.getConnectOptions().getAPN() + "\","
                    + "\"" + GPRSConnectOptions.getConnectOptions().getUser() + "\","
                    + "\"" + GPRSConnectOptions.getConnectOptions().getPasswd() + "\","
                    + "\"\"," // DNS
                    + "0\r"); // TIMEOUT

            ATManager.getInstance().executeCommandSynchron("AT^SCKS=1\r");

            BatteryManager.getInstance();
            GPIOManager.getInstance();

            setWakeupMode();

            ATManager.getInstance().executeCommandSynchron("at+crc=1\r");

            SLog.log(SLog.Debug, "AppMain", "SWITCH ON RADIO PART of the module...");

            CommGPSThread.getInstance().start();
            CommASC0Thread.getInstance().start();
            SocketGPRSThread.getInstance().start();
            if (MicroManager.getInstance().isAdvanced()) {
                CanManagerThread.getInstance().start();
            }
            
            while (!loop()) {
                Thread.sleep(1000);
            }

            cleanup();

            if (VersionChecker.getInstance().mismatch()) {
                SLog.log(SLog.Informational, "AppMain", "version changed");
                String parameters[] = new String[1];
                parameters[0] = "upgrade";
                CommandProcessor.getInstance().perform(parameters[0], parameters);
                while (true) {
                    SLog.log(SLog.Informational, "AppMain", "upgrading...");
                    Thread.sleep(1000);
                }
            }

            if (MicroManager.getInstance().isAdvanced()) {
                CanManagerThread.getInstance().terminate = true;
                CanManagerThread.getInstance().join();
            }
            
            CommGPSThread.getInstance().terminate = true;
            CommGPSThread.getInstance().join();

            SocketGPRSThread.getInstance().terminate = true;
            SocketGPRSThread.getInstance().join();

            shutdown();

            while (true) {
                Thread.sleep(1000);
            }

        } catch (InterruptedException ie) {
            //
        } catch (NumberFormatException nfe) {
            SLog.log(SLog.Error, "AppMain", "NumberFormatException");
        }
    }

    void setWakeupMode() {
        wakeupMode = accelerometerWakeup;
        if (GPIOManager.getInstance().gpio7 == 0) {
            wakeupMode = ignitionWakeup;
        } else {
            String airplane = ATManager.getInstance().executeCommandSynchron("at^scfg=MEopMode/Airplane\r");
            if (airplane.indexOf("^SCFG: \"MEopMode/Airplane\",\"on\"") >= 0) {
                wakeupMode = alarmClockWakeup;
                ATManager.getInstance().executeCommandSynchron("AT^SCFG=\"MEopMode/Airplane\",\"off\"\r");
            } else {
                Date wakeupTime = new Date(Settings.getInstance().getSetting("wakeupTime", 0) * 1000L);
                Date now = new Date();
                SLog.log(SLog.Debug, "AppMain",
                        "wakeupTime " + DateFormatter.isoString(wakeupTime)
                        + " now " + DateFormatter.isoString(now)
                );

                if (wakeupTime.getTime() < now.getTime()) {
                    wakeupMode = alarmClockWakeup;
                }
            }
        }
        SLog.log(SLog.Informational, "AppMain", "wakeupMode is " + wakeupMode);
    }

    protected void pauseApp() {
        SLog.log(SLog.Debug, "AppMain", "pauseApp");
        try {
            destroyApp(true);
        } catch (MIDletStateChangeException msce) {
            //
        }
    }

    protected void destroyApp(boolean cond) throws MIDletStateChangeException {
        SLog.log(SLog.Informational, "AppMain", "destroyApp");
        notifyDestroyed();
    }

    protected void cleanup() {
        SLog.log(SLog.Debug, "AppMain", "cleanup");

        String json = LocationManager.getInstance().getlastPayloadString("L");
        LocationManager.getInstance().send(json);
        Settings.getInstance().setSetting("lastFix", json);

        SLog.log(SLog.Debug, "AppMain", "sending remaining messages");
        while (SocketGPRSThread.getInstance().qSize() > 0) {
            SLog.log(SLog.Debug, "AppMain", "waiting qSize= " + SocketGPRSThread.getInstance().qSize());
            if (SocketGPRSThread.getInstance().isGPRSTimeout()
                    || SocketGPRSThread.getInstance().isMQTTTimeout()) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                //
            }
        }
    }

    protected void shutdown() {
        SLog.log(SLog.Debug, "AppMain", "shutdown");

        Date date = LocationManager.getInstance().dateLastFix();
        if (date != null) {
            SLog.log(SLog.Debug, "AppMain", "powerDown last fix time " + DateFormatter.isoString(date));
            String rtc = "at+cclk=\""
                    + DateFormatter.atString(date)
                    + "\"\r";

            ATManager.getInstance().executeCommandSynchron(rtc);
        } else {
            date = new Date();
        }

        SLog.log(SLog.Informational, "AppMain", "powerDown");
        if (BatteryManager.getInstance().isBatteryVoltageLow()) {
            SLog.log(SLog.Debug, "AppMain", "powerDown on low battery, no wakeup call");
        } else {
            date.setTime(date.getTime() + Settings.getInstance().getSetting("sleep", 6 * 3600) * 1000L);
            SLog.log(SLog.Informational, "AppMain", "powerDown setting wakeup call for " + DateFormatter.isoString(date));

            String rtc = "at+cala=\""
                    + DateFormatter.atString(date)
                    + "\"\r";

            ATManager.getInstance().executeCommandSynchron(rtc);
            Settings.getInstance().setSetting("wakeupTime", Long.toString(date.getTime() / 1000));
        }

        gpio6WatchDogTask.stop();
        GPIOManager.getInstance().close();

        userwareWatchDogTask.stop();

        SLog.log(SLog.Debug, "AppMain", "watchdogs stopped");

        BatteryManager.getInstance().lowPowerMode();
        SLog.log(SLog.Debug, "AppMain", "lowPowerMode");
        ATManager.getInstance().executeCommandSynchron("AT^SMSO\r");
        SLog.log(SLog.Debug, "AppMain", "SMSO");
    }

    private boolean loop() {
        if (invalidSIM) {
            SLog.log(SLog.Error, "AppMain", "invalidSim");
            return true;
        }

        if ((wakeupMode.equals(accelerometerWakeup) || wakeupMode.equals(alarmClockWakeup))
                && LocationManager.getInstance().isOnce()
                && SocketGPRSThread.getInstance().qSize() == 0) {
            SLog.log(SLog.Informational, "AppMain", wakeupMode + " && once && qSize");
            return true;
        }

        if (wakeupMode.equals(ignitionWakeup)
                && !Settings.getInstance().getSetting("battery", false)
                && GPIOManager.getInstance().gpio7 == 1
                && SocketGPRSThread.getInstance().qSize() == 0) {

            SLog.log(SLog.Informational, "AppMain", wakeupMode + " && gpio7 && qSize");
            return true;
        }

        if (BatteryManager.getInstance()
                .isBatteryVoltageLow()) {
            SLog.log(SLog.Warning, "AppMain", "lowBattery");
            return true;
        }

        if (VersionChecker.getInstance().mismatch()) {
            SLog.log(SLog.Informational, "AppMain", "VersionChecker mismatch");
            return true;
        }

        //if (LocationManager.getInstance().isTimeout()) {
        //    SLog.log(SLog.Debug, "AppMain", "fixTimeout");
        //    return true;
        //}
        //if (SocketGPRSThread.getInstance().isGPRSTimeout()) {
        //    SLog.log(SLog.Debug, "AppMain", "gprsTimeout");
        //    return true;
        //}
        //if (SocketGPRSThread.getInstance().isMQTTTimeout()) {
        //    SLog.log(SLog.Debug, "AppMain", "mqttTimeout");
        //    return true;
        //}
        return false;
    }

    private String two(int i) {
        String s = "" + i;
        if (s.length() < 2) {
            s = "0" + s;
        }
        return s;
    }
}
