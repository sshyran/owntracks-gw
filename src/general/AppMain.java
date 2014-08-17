/* Class 	AppMain
 * 
 * This software is developed for Choral devices with Java
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import com.m2mgo.util.GPRSConnectOptions;
import choral.io.CheckUpgrade;
import choral.io.MovSens;
import choral.io.MovListener;
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
public class AppMain extends MIDlet implements GlobCost, MovListener {
    private String executionState;
    
    private String text;
    private String msgRicevuto = "";
    private boolean restart = false;
    int timeToOff = 0;

    MovSens movSens;
    boolean moved = false;

    public boolean shouldReboot = false;
    
    public boolean airplaneMode = false;

    final public int ignitionWakeup = 1;
    final public int motionWakeup = 2;
    final public int alarmWakeup = 3;
    public int wakeupMode = motionWakeup;

    /**
     * <BR> Thread for update configuration parameters through CSD call.
     */
    UpdateCSD th7;
    /**
     * <BR> Thread for (received) SMS management.
     */
    ProcessSMSThread processSMSThread;

    /*
     * file and recordstore
     */
    /**
     * <BR> Manager for save and take data from configuration file.
     */
    Settings settings;
    InfoStato infoS;

    /**
     * <BR> Timer for cyclic monitoring of network registration status.
     */
    Timer regTimer;
    /**
     * <BR> Task for execute operations about network registration status
     * monitoring.
     */
    TimeoutTask regTimeoutTask;

    UserwareWatchDogTask userwareWatchDogTask;
    GPIO6WatchDogTask gpio6WatchDogTask;



    /**
     * <BR> Timer for delay tracking start when key is activated.
     */
    Timer trackTimer;
    private boolean trackTimerAlive = false;
    /**
     * <BR> Task for delay tracking start when key is activated.
     */
    TimeoutTask trackTimeoutTask;

    static AppMain appMain;

    public static AppMain getInstance() {
        return AppMain.appMain;
    }

    public AppMain() {

        CheckUpgrade fw = new CheckUpgrade("");

        settings = Settings.getInstance();
        settings.setfileURL("file:///a:/file/OwnTracks.properties");

        settings.setSettingNoWrite("MIDlet-Name", getAppProperty("MIDlet-Name"));
        settings.setSettingNoWrite("MIDlet-Vendor", getAppProperty("MIDlet-Vendor"));
        settings.setSetting("MIDlet-Version", getAppProperty("MIDlet-Version"));

        System.out.println(
                "Running " + settings.getSetting("MIDlet-Name", "")
                + " " + settings.getSetting("MIDlet-Vendor", "")
                + " " + settings.getSetting("MIDlet-Version", "")
        );

        ATManager.getInstance();
        infoS = InfoStato.getInstance();

        CommGPSThread.getInstance();
        processSMSThread = new ProcessSMSThread();
        CommASC0Thread.getInstance();
        SocketGPRSThread.getInstance();

        SocketGPRSThread.getInstance().put(
                Settings.getInstance().getSetting("publish", "owntracks/gw/")
                + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                + "/sw/midlet",
                Settings.getInstance().getSetting("qos", 1),
                Settings.getInstance().getSetting("retain", true),
                (settings.getSetting("MIDlet-Name", "")
                + " " + settings.getSetting("MIDlet-Vendor", "")
                + " " + settings.getSetting("MIDlet-Version", "")).getBytes()
        );

        BearerControl.addListener(Bearer.getInstance());
    }

    /*
     *  methods
     */
    /**
     * Contains the main body of the application.
     * <BR> ---------- <BR>
     * Executed operations: <br>
     * <ul type="disc">
     * <li> Threads init and start;
     * <li> Recover system settings from the configuration file;
     * <li> Determination of the STATE with which start the application;
     * <li> Retrieving data stored in the internal flash by RecordStore;
     * <li> Set AUTOSTART (only if applicazion is in state <i>execFIRST</i> or
     * <i>execPOSTRESET</i>);
     * <li> Execution of AT^SJNET to set UDP connections through GPRS;
     * <li> GPIO driver activation;
     * <li> Check key status (GPIO n.7). If key is active, the device is powered
     * up at voltage 24 VDC and GPIO n.7 has logic value "0". The device must
     * remain powered up in STAND-BY mode (accessible but with tracking
     * disabled), then the application waits for GPIO n.7 goes to logic value
     * "1". If at application startup the key is active yet, then it means that
     * the device has been awakened from trailer coupling. If trailer is
     * attached when application is executed with status different from
     * <i>execCHIAVEattivata</i>, then application goes to STAND-BY mode.
     * <li> Disabling motion sensor;
     * <li> Preparing timeout about FIX GPS and position strings sending through
     * GPRS connection;
     * <li> Possible radio parts activation (AIRPLANE MODE) and start of GPS
     * receiver in transparent mode;
     * <li> Wait for messages from threads and messages management, main class
     * waiting for messages from threads and coordinates operations to do, based
     * on received messages and priorities.
     * <li> Power of module and stop application.
     * </ul>
     * ---------- <BR>
     * Messages sent from threads or events managed in AppMain are the
     * following:
     * <ul type="circle">
     * <li> Key activation/deactivation;
     * <li> Valid FIX GPS or 'FIXtimeout' expired;
     * <li> Request to send GPS position strings through GPRS;
     * <li> Sending successfully completed of position strings through GPRS or
     * 'FIXgprsTimeout' expired;
     * <li> Request to close application or transition to STAND-BY mode;
     * <li> RING event (CSD call) to configure application parameters.
     * </ul>
     */
    protected void startApp() throws MIDletStateChangeException {
        AppMain.appMain = this;
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
                airplaneMode = true;
                wakeupMode = alarmWakeup;
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
                new LogError("AppMain: ERROR, I can not determine the status of execution of the application!");
            }

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: STATOexecApp is " + executionState);
            }

            ATManager.getInstance().executeCommandSynchron("AT\r");
            ATManager.getInstance().executeCommandSynchron("at+cpin=5555\r");
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
                        + Settings.getInstance().getSetting("MIDlet-Name", "OwnTracks") + ".jar\"\r");
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

            movSens = new MovSens();
            movSens.addMovListener(this);
            
            if (Settings.getInstance().getSetting("motion", 4) > 0) {
                movSens.setMovSens(Settings.getInstance().getSetting("motion", 4));
            }

            BatteryManager.getInstance();
            GPIOInputManager.getInstance();

            if (GPIOInputManager.getInstance().gpio7 == 0) {
                wakeupMode = ignitionWakeup;
            }

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: wakeupMode is " + wakeupMode);
            }
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: airplaneMode is " + airplaneMode);
            }
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: moved is " + moved);
            }

            movSens.movSensOff();
            
            ATManager.getInstance().executeCommandSynchron("at+crc=1\r");

            // Not sure what to do here
            //ATManager.getInstance().executeCommandSynchron("AT^SCFG=\"MEopMode/Airplane\",\"off\"\r");
            //Settings.getInstance().setSetting("closeMode", closeAIR);
            //reboot

            if (!airplaneMode) {
                if (Settings.getInstance().getSetting("mainDebug", false)) {
                    System.out.println("AppMain: SWITCH ON RADIO PART of the module...");
                }
            
                ATManager.getInstance().executeCommandSynchron("AT+CREG=1\r");
                ATManager.getInstance().executeCommandSynchron("AT+CGREG=1\r");

                CommGPSThread.getInstance().start();
                processSMSThread.start();
                CommASC0Thread.getInstance().start();
                SocketGPRSThread.getInstance().start();

                InfoStato.getInstance().setEnableCSD(true);

                while (!loop()) {
                    Thread.sleep(1000);
                }

                InfoStato.getInstance().setEnableCSD(false);

                CommGPSThread.getInstance().terminate = true;
                CommGPSThread.getInstance().join();

                SocketGPRSThread.getInstance().terminate = true;
                SocketGPRSThread.getInstance().join();
            }
            shutdown();
        } catch (InterruptedException ie) {
            new LogError("Interrupted Exception AppMain");
        } catch (NumberFormatException nfe) {
            new LogError("NumberFormatException AppMain");
        } catch (IOException ioe) {
            new LogError("IOException AppMain");
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

        if (BatteryManager.getInstance().isBatteryVoltageLow()) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain, low battery: motion sensor DISABLED!");
            }
        } else {
            if (Settings.getInstance().getSetting("motion", 4) > 0) {
                try {
                    movSens.movSensOn();
                } catch (IOException ioe) {
                    System.err.println("IOException movSensOn");
                }
            }
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

    public void ringEvent(String event) {
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("ringEvent " + event);
        }
    }
    
    public void underVoltageEvent(String event) {
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("underVoltageEvent " + event);
        }
    }
    
    public void movSensEvent(String event) {
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("movSensEvent " + event);
        }

        if (event.equalsIgnoreCase("^MOVE: 0")) {
            moved = false;
        } else if (event.equalsIgnoreCase("^MOVE: 1")) {
            moved = true;
        }
    }

    private boolean loop() {
        if (wakeupMode == ignitionWakeup && GPIOInputManager.getInstance().gpio7 == 1) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("ignitionWakeup && gpio7");
            }
            return true;
        }

        if (wakeupMode == motionWakeup && LocationManager.getInstance().dateLastFix() != null) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: motionWakeup && dateLastFix");
            }
            return true;
        }

        if (wakeupMode == motionWakeup && LocationManager.getInstance().isTimeout()) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: motionWakeup && isTimeout");
            }
            return true;
        }

        if (wakeupMode == alarmWakeup && LocationManager.getInstance().dateLastFix() != null) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: alarmWakeup && dateLastFix");
            }
            return true;
        }

        if (wakeupMode == alarmWakeup && LocationManager.getInstance().isTimeout()) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: alarmWakeup && isTimeout");
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

//#ifdef TODO
//#             //*** [12i] RING EVENT, FOR CSD CALL (IF POSSIBLE)
//#                         /*
//#              * 'msgRING' received and CSD procedure enabled
//#              * I can activate CSD procedure and disable every other operation
//#              * until CSD procedure will be finished
//#              */
//#             if (msgRicevuto.equalsIgnoreCase(msgRING) && InfoStato.getInstance().getEnableCSD() == true && InfoStato.getInstance().getCSDattivo() == false) {
//# 
//#                 if (Settings.getInstance().getSetting("mainDebug", false)) {
//#                     System.out.println("AppMain: received instruction to start CSD procedure");
//#                 }
//# 
//#                 // Init and start UpdateCSD
//#                 th7 = new UpdateCSD();
//#                 th7.setPriority(5);
//#                 th7.start();
//# 
//#                 // Wait for UpdateCSD answers to incoming call
//#                 Thread.sleep(2000);
//# 
//#             } //msgRING
//# 
//#             if (InfoStato.getInstance().getReboot()) {
//#                 System.out.println("Reboot for GPIO");
//#                 /*
//#                  if (InfoStato.getInstance().getInfoFileInt(UartNumTent) > 0) {
//#                  FlashFile.getInstance().setImpostazione(UartNumTent, "1");
//#                  InfoStato.getFile();
//#                  FlashFile.getInstance().writeSettings();
//#                  InfoStato.freeFile();
//#                  }
//#                  */
//#                 restart = true;
//#                 //break;
//#             }
//# 
//#             Thread.sleep(10);
//# 
//#         } catch (InterruptedException ie) {
//#             new LogError("Interrupted Exception AppMain");
//#             restart = true;
//#         } catch (Exception ioe) {
//#             new LogError("Generic Exception AppMain");
//#             restart = true;
//#         }
//#         if (restart == true) {
//#             restart = false;
//#             if (Settings.getInstance().getSetting("mainDebug", false)) {
//#                 System.out.println("AppMain: Reboot module in progress...");
//#             }
//#             new LogError("Reboot for GPIO");
//#             BatteryManager.getInstance().reboot();
//#             return true;
//#         }
//#         return false;
//#     }
//#endif
}
