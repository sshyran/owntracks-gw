/* Class 	AppMain
 * 
 * This software is developed for Choral devices with Java
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import com.m2mgo.util.GPRSConnectOptions;
import choral.io.CheckUpgrade;
import com.cinterion.io.BearerControl;
import java.util.*;
import javax.microedition.midlet.*;
import java.io.IOException;

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

        System.out.println("Running " + getAppProperty("MIDlet-Version") + " @ " + new Date());

        ATManager.getInstance();

        CommGPSThread.getInstance();
        CommASC0Thread.getInstance();
        SocketGPRSThread.getInstance();
        ProcessSMSThread.setup();

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
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: Recover system settings in progress...");
            }
            String closeMode = Settings.getInstance().getSetting("closeMode", closeAppResetHW);
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: Last closing of application: " + closeMode);
            }
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
                Log.log("AppMain: ERROR, I can not determine the status of execution of the application!");
            }

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: STATOexecApp is " + executionState);
            }

            ATManager.getInstance().executeCommandSynchron("AT\r");
            
            String pin = Settings.getInstance().getSetting("pin", "");
            if (pin.length() > 0) {
                ATManager.getInstance().executeCommandSynchron("at+cpin=" + pin + "\r");
            }
            
            ATManager.getInstance().executeCommandSynchron("at^spio=1\r");
            
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: watchdogs starting");
            }

            userwareWatchDogTask = new UserwareWatchDogTask();
            gpio6WatchDogTask = new GPIO6WatchDogTask();

            if (executionState.equalsIgnoreCase(execFIRST)
                    || executionState.equalsIgnoreCase(execPOSTRESET)) {

                if (Settings.getInstance().getSetting("mainDebug", false)) {
                    System.out.println("AppMain: Set AUTOSTART...");
                }
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

            if (GPIOInputManager.getInstance().gpio7 == 0) {
                wakeupMode = ignitionWakeup;
            } else if (alarm) {
                wakeupMode = alarmWakeup;
            }
            
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: wakeupMode is " + wakeupMode);
            }
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: airplaneMode is " + airplaneMode);
            }
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: moved is " + MicroManager.getInstance().hasMoved());
            }
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: alarm is " + alarm);
            }
            
            ATManager.getInstance().executeCommandSynchron("at+crc=1\r");

            if (!airplaneMode) {
                if (Settings.getInstance().getSetting("mainDebug", false)) {
                    System.out.println("AppMain: SWITCH ON RADIO PART of the module...");
                }
            
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
            Log.log("Interrupted Exception AppMain");
        } catch (NumberFormatException nfe) {
            Log.log("NumberFormatException AppMain");
        }
    }

    protected void pauseApp() {
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("AppMain: pauseApp");
        }
        try {
            destroyApp(true);
        } catch (MIDletStateChangeException msce) {
            //
        }
    }

    protected void destroyApp(boolean cond) throws MIDletStateChangeException {
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("AppMain: destroyApp");
        }
        notifyDestroyed();
    }

    protected void cleanup() {
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("AppMain: cleanup");
        }
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("AppMain: sending remaining messages");
        }
        
        while (SocketGPRSThread.getInstance().qSize() > 0) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: waiting qSize= " + SocketGPRSThread.getInstance().qSize());
            } 
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                //
            }
        }        
    }
    protected void shutdown() {
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("AppMain: shutdown");
        }
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("AppMain: powerDown from " + executionState);
        }

        Date date = LocationManager.getInstance().dateLastFix();
        if (date != null) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: powerDown @ last fix time " + date.toString());
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            String rtc = "at+cclk=\""
                    + two(cal.get(Calendar.YEAR) - 2000)
                    + "/" + two(cal.get(Calendar.MONTH) + 1)
                    + "/" + two(cal.get(Calendar.DAY_OF_MONTH))
                    + ","
                    + two(cal.get(Calendar.HOUR) + (cal.get(Calendar.AM_PM) == Calendar.PM ? 12 : 0))
                    + ":" + two(cal.get(Calendar.MINUTE))
                    + ":" + two(cal.get(Calendar.SECOND))
                    + "\"\r";

            ATManager.getInstance().executeCommandSynchron(rtc);
        } else {
            date = new Date();
        }

        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("AppMain: powerDown @ " + date.toString());
        }

        if (BatteryManager.getInstance().isBatteryVoltageLow()) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("PAppMain: powerDown on low battery, no wakeup call");
            }

        } else if (airplaneMode) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("PAppMain: powerDown on airplaneMode");
            }
            
            ATManager.getInstance().executeCommandSynchron("AT^SCFG=\"MEopMode/Airplane\",\"off\"\r");

        } else {
            date.setTime(date.getTime() + Settings.getInstance().getSetting("sleep", 6 * 3600) * 1000L);

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: powerDown setting wakeup call for " + date.toString());
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            String rtc = "at+cala=\""
                    + two(cal.get(Calendar.YEAR) - 2000)
                    + "/" + two(cal.get(Calendar.MONTH) + 1)
                    + "/" + two(cal.get(Calendar.DAY_OF_MONTH))
                    + ","
                    + two(cal.get(Calendar.HOUR) + (cal.get(Calendar.AM_PM) == Calendar.PM ? 12 : 0))
                    + ":" + two(cal.get(Calendar.MINUTE))
                    + ":" + two(cal.get(Calendar.SECOND))
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

        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("AppMain: powerDown closeMode is " + Settings.getInstance().getSetting("closeMode", closeAppNormaleOK));
        }

        ATManager.getInstance().executeCommandSynchron("AT^SPIO=0\r");
        
        gpio6WatchDogTask.stop();
        userwareWatchDogTask.stop();
        
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("AppMain: watchdogs stopped");
            System.out.flush();
        }
        
        BatteryManager.getInstance().lowPowerMode();
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("AppMain: lowPowerMode");
            System.out.flush();
        }
        ATManager.getInstance().executeCommandSynchron("AT^SMSO\r");
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("AppMain: SMSO");
            System.out.flush();
        }
    }

    private boolean loop() {
        if (invalidSIM) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("invalidSim");
            }
            return true;
        }
        
        if (LocationManager.getInstance().isTimeout() ||
                SocketGPRSThread.getInstance().isGPRSTimeout() ||
                SocketGPRSThread.getInstance().isMQTTTimeout()) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("fixTimeout | gprsTimeout | mqttTimeout");
            }
            return true;
        }

        if ((wakeupMode.equals(motionWakeup) || wakeupMode.equals(alarmWakeup)) &&
                LocationManager.getInstance().dateLastFix() != null &&
                SocketGPRSThread.getInstance().qSize() == 0) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: " + wakeupMode + " && dateLastFix && qSize");
            }
            return true;
        }

        if (wakeupMode.equals(ignitionWakeup) &&
                GPIOInputManager.getInstance().gpio7 == 1 &&
                SocketGPRSThread.getInstance().qSize() == 0) {

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: " + wakeupMode + " && gpio7 && qSize");
            }
            return true;
        }

        if (BatteryManager.getInstance().isBatteryVoltageLow()) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: lowBattery");
            }
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
