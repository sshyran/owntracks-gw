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
    public boolean airplaneMode = false;
    public boolean alarm = false;

    final public String ignitionWakeup = "IgnitionWakeup";
    final public String motionWakeup = "MotionWakeup";
    final public String alarmWakeup = "AlarmWakeup";
    final public String batteryWakeup = "BatteryWakeup";
    public String wakeupMode = motionWakeup;

    /**
     * Application execution status
     */
    public static final String execFIRST = "FirstExecution";
    public static final String execNORMALE = "NormalExecution";
    public static final String execCHIAVEattivata = "KeyOnExecution";
    public static final String execTrack = "Tracking";
    public static final String execCHIAVEdisattivata = "KeyOffExecution";
    public static final String execMOVIMENTO = "MoveExecution";
    public static final String execPOSTRESET = "PostResetExecution";
    public static final String execBATTSCARICA = "BatteriaScaricaExecution";
    public static final String execSMStrack = "TrackingFromSMSExecution";
    private String executionState;

    /**
     * Application closure modes
     */
    public static final String closeAppFactory = "Factory";
    public static final String closeAppDisattivChiaveOK = "DisattivChiaveOK";
    public static final String closeAppDisattivChiaveTimeout = "DisattivChiaveTimeout";
    public static final String closeAppDisattivChiaveFIRST = "DisattivChiaveFIRST";
    public static final String closeAppNormaleOK = "NormaleOK";
    public static final String closeAppNormaleTimeout = "NormaleTimeout";
    public static final String closeAppMovimentoOK = "SensMovOK";
    public static final String closeAppResetHW = "ResetHW";
    public static final String closeAppBatteriaScarica = "BatteriaScarica";
    public static final String closeAppPostReset = "DisattivChiavePostReset";
    public static final String closeAppOTAP = "OTAP";
    public static final String closeAIR = "closeAIR";

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
        settings.setfileURL("file:///a:/file/OwnTracks.properties");

        ATManager.getInstance();
        CommASC0Thread.getInstance();
        SocketGPRSThread.getInstance();
        CommGPSThread.getInstance();
        ProcessSMSThread.setup();

        SLog.log(SLog.Informational, "AppMain", "Running " + getAppProperty("MIDlet-Version") + " " + DateFormatter.isoString(new Date()));

        SocketGPRSThread.getInstance().put(
                settings.getSetting("publish", "owntracks/gw/")
                + settings.getSetting("clientID", MicroManager.getInstance().getIMEI())
                + "/sw/midlet",
                settings.getSetting("qos", 1),
                settings.getSetting("retain", true),
                getAppProperty("MIDlet-Version").getBytes()
        );

        BearerControl.addListener(Bearer.getInstance());
        try {
            SLog.log(SLog.Debug, "AppMain", "Recover system settings in progress...");
            String closeMode = Settings.getInstance().getSetting("closeMode", closeAppResetHW);
            SLog.log(SLog.Informational, "AppMain", "Last closing of application: " + closeMode);
            Settings.getInstance().setSetting("closeMode", closeAppResetHW);

            if (closeMode.equalsIgnoreCase(closeAppFactory)) {
                executionState = execFIRST;
            } else if (closeMode.equalsIgnoreCase(closeAIR)) {
                executionState = execNORMALE;
            } else if (closeMode.equalsIgnoreCase(closeAppNormaleOK)) {
                executionState = execNORMALE;
            } else if (closeMode.equalsIgnoreCase(closeAppNormaleTimeout)) {
                executionState = execNORMALE;
            } else if (closeMode.equalsIgnoreCase(closeAppDisattivChiaveFIRST)) {
                executionState = execNORMALE;
            } else if (closeMode.equalsIgnoreCase(closeAppDisattivChiaveOK)) {
                executionState = execNORMALE;
            } else if (closeMode.equalsIgnoreCase(closeAppDisattivChiaveTimeout)) {
                executionState = execNORMALE;
            } else if (closeMode.equalsIgnoreCase(closeAppMovimentoOK)) {
                executionState = execNORMALE;
            } else if (closeMode.equalsIgnoreCase(closeAppPostReset)) {
                executionState = execNORMALE;
            } else if (closeMode.equalsIgnoreCase(closeAppBatteriaScarica)) {
                executionState = execNORMALE;
            } else if (closeMode.equalsIgnoreCase(closeAppResetHW)) {
                executionState = execPOSTRESET;
            } else if (closeMode.equalsIgnoreCase(closeAppBatteriaScarica)) {
                executionState = execPOSTRESET;
            } else {
                SLog.log(SLog.Notice, "AppMain", "I can not determine the status of execution of the application!");
            }

            SLog.log(SLog.Informational, "AppMain", "excecutionState is " + executionState);

            ATManager.getInstance().executeCommandSynchron("AT\r");

            String pin = Settings.getInstance().getSetting("pin", "");
            if (pin.length() > 0) {
                ATManager.getInstance().executeCommandSynchron("at+cpin=" + pin + "\r");
            }

            ATManager.getInstance().executeCommandSynchron("at^spio=1\r");

            SLog.log(SLog.Debug, "AppMain", "watchdogs starting");

            userwareWatchDogTask = new UserwareWatchDogTask();
            gpio6WatchDogTask = new GPIO6WatchDogTask();

            if (executionState.equalsIgnoreCase(execFIRST)
                    || executionState.equalsIgnoreCase(execPOSTRESET)) {

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

                executionState = execNORMALE;

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
            GPIOInputManager.getInstance();

            if (Settings.getInstance().getSetting("battery", false)) {
                wakeupMode = batteryWakeup;
            } else {
                if (GPIOInputManager.getInstance().gpio7 == 0) {
                    wakeupMode = ignitionWakeup;
                } else if (alarm) {
                    wakeupMode = alarmWakeup;
                }
            }

            SLog.log(SLog.Debug, "AppMain", "wakeupMode is " + wakeupMode);
            SLog.log(SLog.Debug, "AppMain", "airplaneMode is " + airplaneMode);
            SLog.log(SLog.Debug, "AppMain", "moved is " + MicroManager.getInstance().hasMoved());
            SLog.log(SLog.Debug, "AppMain", "alarm is " + alarm);

            ATManager.getInstance().executeCommandSynchron("at+crc=1\r");

            if (!airplaneMode) {
                SLog.log(SLog.Debug, "AppMain", "SWITCH ON RADIO PART of the module...");

                ATManager.getInstance().executeCommandSynchron("AT+CREG=1\r");
                ATManager.getInstance().executeCommandSynchron("AT+CGREG=1\r");

                CommGPSThread.getInstance().start();
                CommASC0Thread.getInstance().start();
                SocketGPRSThread.getInstance().start();

                while (!loop()) {
                    Thread.sleep(1000);
                }

                cleanup();

                CommGPSThread.getInstance().terminate = true;
                CommGPSThread.getInstance().join();

                SocketGPRSThread.getInstance().terminate = true;
                SocketGPRSThread.getInstance().join();
            }
            shutdown();
        } catch (InterruptedException ie) {
            //
        } catch (NumberFormatException nfe) {
            SLog.log(SLog.Error, "AppMain", "NumberFormatException");
        }
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
        SLog.log(SLog.Debug, "AppMain", "powerDown from " + executionState);
        Date date = LocationManager.getInstance().dateLastFix();
        if (date != null) {
            SLog.log(SLog.Debug, "AppMain", "powerDown @ last fix time " + DateFormatter.isoString(date));
            String rtc = "at+cclk=\""
                    + DateFormatter.atString(date)
                    + "\"\r";

            ATManager.getInstance().executeCommandSynchron(rtc);
        } else {
            date = new Date();
        }

        SLog.log(SLog.Informational, "AppMain", "powerDown @ " + DateFormatter.isoString(date));
        if (BatteryManager.getInstance().isBatteryVoltageLow()) {
            SLog.log(SLog.Debug, "AppMain", "powerDown on low battery, no wakeup call");

        } else if (airplaneMode) {
            SLog.log(SLog.Debug, "AppMain", "powerDown on airplaneMode");
            ATManager.getInstance().executeCommandSynchron("AT^SCFG=\"MEopMode/Airplane\",\"off\"\r");

        } else {
            if (wakeupMode.equals(batteryWakeup)) {
                date.setTime(date.getTime() + 1 * 1000L);
            } else {
                date.setTime(date.getTime() + Settings.getInstance().getSetting("sleep", 6 * 3600) * 1000L);
            }
            SLog.log(SLog.Informational, "AppMain", "powerDown setting wakeup call for " + DateFormatter.isoString(date));

            String rtc = "at+cala=\""
                    + DateFormatter.atString(date)
                    + "\"\r";

            ATManager.getInstance().executeCommandSynchron(rtc);
        }

        if (executionState.equalsIgnoreCase(execFIRST)) {
            Settings.getInstance().setSetting("closeMode", closeAppDisattivChiaveFIRST);
        } else if (executionState.equalsIgnoreCase(execNORMALE)) {
            if (LocationManager.getInstance().isFix()) {
                Settings.getInstance().setSetting("closeMode", closeAppNormaleOK);
            } else {
                Settings.getInstance().setSetting("closeMode", closeAppNormaleTimeout);
            }
        } else if (executionState.equalsIgnoreCase(execCHIAVEdisattivata)) {
            if (LocationManager.getInstance().isFix()) {
                Settings.getInstance().setSetting("closeMode", closeAppDisattivChiaveOK);
            } else {
                Settings.getInstance().setSetting("closeMode", closeAppDisattivChiaveTimeout);
            }
        } else if (executionState.equalsIgnoreCase(execMOVIMENTO)) {
            Settings.getInstance().setSetting("closeMode", closeAppMovimentoOK);
        } else if (executionState.equalsIgnoreCase(execPOSTRESET)) {
            Settings.getInstance().setSetting("closeMode", closeAppPostReset);
        }

        if (airplaneMode) {
            Settings.getInstance().setSetting("closeMode", closeAIR);
        }

        if (BatteryManager.getInstance().isBatteryVoltageLow()) {
            Settings.getInstance().setSetting("closeMode", closeAppBatteriaScarica);
        }

        SLog.log(SLog.Informational, "AppMain", "powerDown closeMode is " + Settings.getInstance().getSetting("closeMode", closeAppNormaleOK));

        ATManager.getInstance().executeCommandSynchron("AT^SPIO=0\r");

        gpio6WatchDogTask.stop();
        userwareWatchDogTask.stop();

        SLog.log(SLog.Debug, "AppMain", "watchdogs stopped");

        BatteryManager.getInstance().lowPowerMode();
        SLog.log(SLog.Debug, "AppMain", "lowPowerMode");
        ATManager.getInstance().executeCommandSynchron("AT^SMSO\r");
        SLog.log(SLog.Debug, "AppMain", "SMSO");
    }

    private boolean loop() {
        if (invalidSIM) {
            SLog.log(SLog.Debug, "AppMain", "invalidSim");
            return true;
        }

        if (LocationManager.getInstance().isTimeout()) {
            SLog.log(SLog.Debug, "AppMain", "fixTimeout");
            return true;
        }

        if (SocketGPRSThread.getInstance().isGPRSTimeout()) {
            SLog.log(SLog.Debug, "AppMain", "gprsTimeout");
            return true;
        }

        if (SocketGPRSThread.getInstance().isMQTTTimeout()) {
            SLog.log(SLog.Debug, "AppMain", "mqttTimeout");
            return true;
        }

        if ((wakeupMode.equals(motionWakeup) || wakeupMode.equals(alarmWakeup))
                && LocationManager.getInstance().dateLastFix() != null
                && SocketGPRSThread.getInstance().qSize() == 0) {
            SLog.log(SLog.Debug, "AppMain", wakeupMode + " && dateLastFix && qSize");
            return true;
        }

        if (wakeupMode.equals(ignitionWakeup)
                && GPIOInputManager.getInstance().gpio7 == 1
                && SocketGPRSThread.getInstance().qSize() == 0) {

            SLog.log(SLog.Debug, "AppMain", wakeupMode + " && gpio7 && qSize");
            return true;
        }

        if (BatteryManager.getInstance()
                .isBatteryVoltageLow()) {
            SLog.log(SLog.Debug, "AppMain", "lowBattery");
            return true;
        }

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
