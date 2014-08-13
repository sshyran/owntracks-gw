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
import javax.microedition.rms.*;

/**
 * Main application class.
 * <BR>
 * startApp() method contains the main body of the application.
 *
 * @version	1.00 <BR> <i>Last update</i>: 28-05-2008
 * @author matteobo
 */
public class AppMain extends MIDlet implements GlobCost, MovListener {

    /*
     * local variables
     */
    private String text;
    private String msgRicevuto = "";
    private boolean GOTOstagePwDownSTANDBY = false;
    private boolean elabBATT, elabPWD = false;
    private boolean PowDown = false;
    private boolean restart = false;
    int timeToOff = 0;

    MovSens movSens;
    boolean moved = false;

    /**
     * <BR> Thread for update configuration parameters through CSD call.
     */
    UpdateCSD th7;
    /**
     * <BR> Thread for (received) SMS management.
     */
    ProcessSMSThread processSMSThread;
    /**
     * <BR> Thread for serial (ASC0) management.
     */
    Seriale th9;
    /**
     * <BR> Thread for UDP socket management.
     */
    //AccelerometerTask	th12;

    /*
     * file and recordstore
     */
    /**
     * <BR> Manager for save and take data from configuration file.
     */
    Settings settings;
    InfoStato infoS;
    Mailboxes mailboxes;

    /**
     * <BR> Timer for cyclic monitoring of network registration status.
     */
    Timer regTimer;
    /**
     * <BR> Task for execute operations about network registration status
     * monitoring.
     */
    TimeoutTask regTimeoutTask;

    WatchDogTask watchDogTask;

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

    /*
     * constructors 
     */
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
        mailboxes = Mailboxes.getInstance();

        CommGPSThread commGPStrasparent = CommGPSThread.getInstance();
        processSMSThread = new ProcessSMSThread();
        th9 = new Seriale();
        SocketGPRSThread socketGPRSThread = SocketGPRSThread.getInstance();
        //th12 = new AccelerometerTask();

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

            /* 
             * [1] INITIALIZATION AND START OF THREADS
             * 
             */
            // Set threads priority (default value=5, min=1, max=10)
            CommGPSThread.getInstance().setPriority(5);
            processSMSThread.setPriority(5);
            th9.setPriority(5);
            SocketGPRSThread.getInstance().setPriority(5);

            /*
             * [2] RECOVER SSYSTEM SETTINGS FROM CONFIGURATION FILE
             * 
             */
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: Recover system settings in progress...");
            }
            String closeMode = Settings.getInstance().getSetting("closeMode", closeAppResetHW);
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: Last closing of application: " + closeMode);
            }
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: Last valid string: "
                        + Settings.getInstance().getSetting("lastGPSValid", ""));
            }
            Settings.getInstance().setSetting("closeMode", closeAppResetHW);

            /*
             * [3] DETERMINATION OF THE STATE WITH WHICH START THE APPLICATION
             * 
             */
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("GPS insensibility: " + Settings.getInstance().getSetting("minDistance", 0));
                System.out.println("AppMain: Last CloseMode: " + closeMode);
                System.out.print("AppMain: Determination of application state...");
            }

            /*
             * By default it is supposed that the awakening with '^SYSTART' is due
             * to the motion sensor, then if I verify that the key is activated
             * then it means that it was due to the key
             */
            InfoStato.getInstance().setOpMode("NORMAL");
            InfoStato.getInstance().setTipoRisveglio(risveglioMovimento);

            // First execution after leaving the factory
            if (closeMode.equalsIgnoreCase(closeAppFactory)) {
                InfoStato.getInstance().setSTATOexecApp(execFIRST);
            } // Reboot after AIRPLANE MODE
            else if (closeMode.equalsIgnoreCase(closeAIR)) {
                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                // Set AIRPLANE MODE to deactivate radio parts of the module 
                InfoStato.getInstance().setOpMode("AIRPLANE");
                InfoStato.getInstance().setTipoRisveglio(risveglioCala);
            } // Normal OK
            else if (closeMode.equalsIgnoreCase(closeAppNormaleOK)) {
                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
            } // Normal Timeout
            else if (closeMode.equalsIgnoreCase(closeAppNormaleTimeout)) {
                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
            } // Key deactivation FIRST
            else if (closeMode.equalsIgnoreCase(closeAppDisattivChiaveFIRST)) {
                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
            } // Key deactivation OK
            else if (closeMode.equalsIgnoreCase(closeAppDisattivChiaveOK)) {
                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
            } // Key deactivation Timeout
            else if (closeMode.equalsIgnoreCase(closeAppDisattivChiaveTimeout)) {
                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
            } // Motion sensor OK
            else if (closeMode.equalsIgnoreCase(closeAppMovimentoOK)) {
                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
            } // Post Reset
            else if (closeMode.equalsIgnoreCase(closeAppPostReset)) {
                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
            } // Low battery
            else if (closeMode.equalsIgnoreCase(closeAppBatteriaScarica)) {
                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
            } // Hardware Reset
            else if (closeMode.equalsIgnoreCase(closeAppResetHW)) {
                InfoStato.getInstance().setSTATOexecApp(execPOSTRESET);
            } // Closure due to low battery
            else if (closeMode.equalsIgnoreCase(closeAppBatteriaScarica)) {
                InfoStato.getInstance().setSTATOexecApp(execPOSTRESET);
            } else {
                new LogError("AppMain: ERROR, I can not determine the status of execution of the application!");
            }

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println(InfoStato.getInstance().getSTATOexecApp());
            }

            /*
             *	SET SIM PIN
             */
            ATManager.getInstance().executeCommand("at+cpin=5555\r");

//#ifdef RECORDSTORE
//#                 /*
//#                  * [5] RECOVER DATA FROM FLASH
//#                  * 
//#                  */
//#                 try {
//#                     // Open RecordStore
//#                     RecordStore rs = RecordStore.openRecordStore(recordStoreName, true);
//# 
//#                     /*
//#                      * Record recovey:
//#                      * 1) Last GPRMC valid string
//#                      * 2) <empty> ...
//#                      */
//#                     byte b[] = rs.getRecord(1);
//#                     String str = new String(b, 0, b.length);
//#                     //InfoStato.getInstance().setInfoFileString(LastGPRMCValid, str);
//#                     if (Settings.getInstance().getSetting("mainDebug", false)) {
//#                         System.out.println("RecordStore, record n.1: " + str);
//#                     }
//# 
//#                     // Chiusura RecordStore
//#                     rs.closeRecordStore();
//# 
//#                 } catch (RecordStoreNotOpenException rsnoe) {
//#                     if (Settings.getInstance().getSetting("mainDebug", false)) {
//#                         System.out.println("FlashRecordStore: RecordStoreNotOpenException");
//#                     }
//#                 } catch (RecordStoreFullException rsfe) {
//#                     if (Settings.getInstance().getSetting("mainDebug", false)) {
//#                         System.out.println("FlashRecordStore: RecordStoreFullException");
//#                     }
//#                 } catch (RecordStoreException rse) {
//#                     if (Settings.getInstance().getSetting("mainDebug", false)) {
//#                         System.out.println("FlashRecordStore: RecordStoreException");
//#                     }
//#                 } catch (IllegalArgumentException e) {
//#                     if (Settings.getInstance().getSetting("mainDebug", false)) {
//#                         System.out.println("FlashRecordStore: IllegalArgumentException");
//#                     }
//#                 }
//#endif
                /*
             * [6a] SET AUTOSTART ONLY IF APPLICATION IS IN 
             * 	    STATE 'execFIRST' OR 'execPOSTRESET'
             * 
             */
            if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execFIRST)
                    || InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execPOSTRESET)) {

                if (Settings.getInstance().getSetting("mainDebug", false)) {
                    System.out.println("AppMain: Set AUTOSTART...");
                }
                ATManager.getInstance().executeCommand("at^scfg=\"Userware/Autostart/AppName\",\"\",\"a:/app/"
                        + Settings.getInstance().getSetting("MIDlet-Name", "OwnTracks") + ".jar\"\r");
                ATManager.getInstance().executeCommand("at^scfg=\"Userware/Autostart/Delay\",\"\",10\r");
                ATManager.getInstance().executeCommand("at^scfg=\"Userware/Autostart\",\"\",\"1\"\r");
                if (Settings.getInstance().getSetting("usbDebug", false)) {
                    ATManager.getInstance().executeCommand("at^scfg=\"Userware/StdOut\",USB\r");
                } else {
                    ATManager.getInstance().executeCommand("at^scfg=\"Userware/StdOut\",ASC0\r");
                }

                InfoStato.getInstance().setSTATOexecApp(execNORMALE);

            } //AUTOSTART

            // Set wake up for ALIVE
            ATManager.getInstance().executeCommand("ati\r");
            ATManager.getInstance().executeCommand("at+cclk=\"02/01/01,00:00:00\"\r");
            ATManager.getInstance().executeCommand("at+cala=\"02/01/01,06:00:00\"\r");

            /*
             * [6b] SET AT^SBC TO ADJUST APPLICATION CONSUMPTION
             * 
             */
            ATManager.getInstance().executeCommand("AT^SBC=5000\r");

            /*
             * [6c] SET AT^SJNET FOR GPRS CONNECTION (ALWAYS)
             * 
             */
            ATManager.getInstance().executeCommand("at^sjnet="
                    + "\"" + GPRSConnectOptions.getConnectOptions().getBearerType() + "\","
                    + "\"" + GPRSConnectOptions.getConnectOptions().getAPN() + "\","
                    + "\"" + GPRSConnectOptions.getConnectOptions().getUser() + "\","
                    + "\"" + GPRSConnectOptions.getConnectOptions().getPasswd() + "\","
                    + "\"\"," // DNS
                    + "0\r"); // TIMEOUT

            /*
             * [6d] SET AT+CGSN TO GET MODULE IMEI
             *
             */
            ATManager.getInstance().executeCommand("AT+CGSN\r");
            ATManager.getInstance().executeCommand("AT^SCKS=1\r");
            
            BatteryManager.getInstance();
            GPIOInputManager.getInstance();


            /*
             * At this point I know if device was started
             * due to the motion sensor, the key or + CALA 
             */
            if (InfoStato.getInstance().getTipoRisveglio().equalsIgnoreCase(risveglioMovimento)) {
                /*
                 * If awakening due to motion sensor
                 * go to state 'execMOVIMENTO'
                 */
                if (Settings.getInstance().getSetting("mainDebug", false)) {
                    System.out.println("execMOVIMENTO");
                }
                InfoStato.getInstance().setSTATOexecApp(execMOVIMENTO);
            } //risveglioMovimento

            /*
             * [9] DISABLING MOTION SENSOR
             *
             */
            InfoStato.getInstance().setDisattivaSensore(true);

            watchDogTask = new WatchDogTask();

            /* 
             * [10] RADIO PARTS OR GPS RECEIVER ACTIVATIONS
             *  
             */
            /* 
             * If application is in state 'execNORMALE' or 'execMOVIMENTO'
             * I must go to from AIRPLANE mode to NORMAL mode,
             * activating radio part
             * (case 'execMOVIMENTO' should be covered because it is in fact
             * identical to the normal case, up to this point in the application)
             */
            /* Send 'AT' for safety, per sicurezza, in order to succeed
             * in any case to view the ^SYSSTART URC
             */
            ATManager.getInstance().executeCommand("AT\r");
            ATManager.getInstance().executeCommand("at+crc=1\r");

            // Wait for info about ^SYSSTART type
            while (InfoStato.getInstance().getOpMode() == null) {
                Thread.sleep(whileSleep);
            }

            /* Power on radio parts of the module for safety */
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: SWITCH ON RADIO PART of the module...");
            }
            ATManager.getInstance().executeCommand("AT^SCFG=\"MEopMode/Airplane\",\"off\"\r");
            ATManager.getInstance().executeCommand("AT+CREG=1\r");
            ATManager.getInstance().executeCommand("AT+CGREG=1\r");

            CommGPSThread.getInstance().start();
            processSMSThread.start();
            th9.start();
            SocketGPRSThread.getInstance().start();
            /*
            movSens = new MovSens();
            movSens.addMovListener(this);
            movSens.start();
            movSens.setMovSens(4);
            movSens.movSensOn();
            */

            /* 
             * [11] PREPARING TIMEOUTS ABOUT FIX GPS AND POSITIONING STRINGS SENDING
             * 
             */

            /* Enable CSD */
            InfoStato.getInstance().setEnableCSD(true);

            /* 
             * [12] WAIT FOR MESSAGES FROM THREADS AND MANAGEMENT
             * 
             *	From here AppMain waits for msg from threds and coordinates
             * the operations to be carried out in accordance with the msg
             * received, according to the priorities
             */
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain, actual state of the application: " + InfoStato.getInstance().getSTATOexecApp());
            }

            if (InfoStato.getInstance().getCALA()) {
                System.out.println("AppMain: Module reboot in progress...");
                BatteryManager.getInstance().reboot();
            }

            while (!GOTOstagePwDownSTANDBY) {

                try {
                    ATManager.getInstance().executeCommand("AT+CSQ\r");
                    /*
                     * Read a possible msg present on the Mailbox
                     * 
                     * read() method is BLOCKING, until a message is received
                     * (of some sort) the while loop does not continue
                     */
                    try {

                        msgRicevuto = (String) Mailboxes.getInstance(0).read();

                        //System.out.println(InfoStato.getInstance().getEnableCSD());
                        //System.out.println(InfoStato.getInstance().getCSDattivo());
                    } catch (Exception e) {
                        System.out.println("ERROR READ mboxMAIN)");
                    }
                    //*** [12a] KEY MANAGEMENT
                        /*
                     * ACTIVATION KEY event
                     * 
                     * (both in the case where the key is the cause of the
                     *  application startup and when the key is activated
                     * while the application is running in another state)
                     */
                    if (Settings.getInstance().getSetting("mainDebug", false)) {
                        System.out.println("AppMain message " + msgRicevuto);
                    }
                    if (msgRicevuto.equalsIgnoreCase(msgChiaveAttivata)) {

                        if (Settings.getInstance().getSetting("keyDebug", false)) {
                            System.out.println("AppMain: KEY activation detected...");
                        }

                        /*
                         * Origin state: 'execMOVIMENTO'
                         * 
                         * Suspend motion tracking, execute tracking with active KEYxxxxxxxxxxxx^
                         * and go to STAND-BY
                         */
                        if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execMOVIMENTO)) {

                            if (Settings.getInstance().getSetting("mainDebug", false)) {
                                System.out.println("AppMain,MOTION: key was activated!");
                            }

                            //InfoStato.getInstance().setSTATOexecApp(execCHIAVEattivata);
                            InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                            //Mailboxes.getInstance(3).write(trackAttivChiave);

                        } //execMOVIMENTO
                        else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEattivata)) {

                            if (Settings.getInstance().getSetting("mainDebug", false)) {
                                System.out.println("AppMain, KEY ACTIVE: already in key active state!");
                            }
                        }

                    } //msgChiaveAttivata

                    /*
                     * KEY DEACTIVATION event
                     * 
                     * (you can get here from the state 'execCHIAVEattivata'
                     *  but also from states 'execFIRST' and 'execPOSTRESET',
                     *  becOrigin stateause in these states I check the FIX only if the KEY
                     *  is activated and I would like to know if it has been disabled in the meantime)		  	
                     */
                    if (msgRicevuto.equalsIgnoreCase(msgChiaveDisattivata)) {
                        //if (msgRicevuto.equalsIgnoreCase(msgChiaveDisattivata)){	
                        if (Settings.getInstance().getSetting("keyDebug", false)) {
                            System.out.println("AppMain: KEY deactivation detected");
                        }

                        /*
                         * Origin state: 'execCHIAVEattivata'
                         * 
                         * App sends last valid GPS position (you don't need GPS timer,
                         * but GPRS timer yes!) and goes to power down
                         */
                        if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEattivata) || InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execNORMALE)) {

                            // Block everything before closing
                            //Mailboxes.getInstance(3).write(exitTrack);
                            GOTOstagePwDownSTANDBY = true;

                            // Set new application state
                            InfoStato.getInstance().setSTATOexecApp(execCHIAVEdisattivata);
                        } /*
                         * Case 'execMOVIMENTO' -> do nothing
                         */ else {
                            if (Settings.getInstance().getSetting("mainDebug", false)) {
                                System.out.println("AppMain,CHIAVEdisattivata: nothing to do, key already deactivated!");
                            }
                            if (timeToOff == 50 || timeToOff == 70) {
                                //Mailboxes.getInstance(3).write(trackMovimento);
                            } else {
                                if (timeToOff >= 100) {
                                    // Block everything before closing
                                    //Mailboxes.getInstance(3).write(exitTrack);

                                    GOTOstagePwDownSTANDBY = true;

                                    // Set new application state
                                    InfoStato.getInstance().setSTATOexecApp(execCHIAVEdisattivata);
                                }
                            }
                            timeToOff++;
                            //System.out.println("timeToOff" + timeToOff);
                        }

                    } //msgChiaveDisattivata		

                    if (Settings.getInstance().getSetting("mainDebug", false)) {
                        System.out.println(msgRicevuto);
                    }

                    if (msgRicevuto.equalsIgnoreCase(msgALIVE) && InfoStato.getInstance().getCSDattivo() == false
                            && Settings.getInstance().getSetting("tracking", false)) {

                        if (Settings.getInstance().getSetting("mainDebug", false)) {
                            System.out.println("AppMain: Alive message");
                        }

                        ATManager.getInstance().executeCommand("at+cclk=\"02/01/01,00:00:00\"\r");
                        ATManager.getInstance().executeCommand("at+cala=\"02/01/01,06:00:00\"\r");

                        // If KEY is activated, repeat tracking
                        if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEattivata) || InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execNORMALE)) {

                            InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                            //Mailboxes.getInstance(3).write(trackAlive);
                            trackTimerAlive = true;
                            /*
                             if (InfoStato.getInstance().getInfoFileInt(UartNumTent) > 0) {
                             InfoStato.getInstance().setInfoFileInt(UartNumTent, "0");
                             FlashFile.getInstance().setImpostazione(UartNumTent, "0");
                             InfoStato.getFile();
                             FlashFile.getInstance().writeSettings();
                             InfoStato.freeFile();
                             Mailboxes.getInstance(3).write(trackUrcSim);
                             }
                             */
                        }

                    } //msgAlive

                    if (msgRicevuto.equalsIgnoreCase(msgALR1) && InfoStato.getInstance().getCSDattivo() == false
                            && Settings.getInstance().getSetting("tracking", false)) {

                        if (Settings.getInstance().getSetting("mainDebug", false)) {
                            System.out.println("AppMain: rilevato ALLARME INPUT 1");
                        }

                        // If KEY is activated, repeat tracking
                        if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEattivata) || InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execNORMALE)) {

                            InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                            //Mailboxes.getInstance(3).write(trackAlarmIn1);
                            trackTimerAlive = true;
                        }

                    } //msgALR1

                    if (msgRicevuto.equalsIgnoreCase(msgALR2) && InfoStato.getInstance().getCSDattivo() == false
                            && Settings.getInstance().getSetting("tracking", false)) {

                        if (Settings.getInstance().getSetting("mainDebug", false)) {
                            System.out.println("AppMain: rilevato ALLARME INPUT 2");
                        }

                        // If KEY is activated, repeat tracking
                        if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEattivata) || InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execNORMALE)) {

                            InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                            //Mailboxes.getInstance(3).write(trackAlarmIn2);
                            trackTimerAlive = true;
                        }

                    } //msgALR1

                    //*** [12f] LOW BATTERY CONTROL
                        /*
                     * 'msgBattScarica' received
                     * Put module in power down, without enabling motion sensor
                     */
                    if (msgRicevuto.equalsIgnoreCase(msgBattScarica) && elabBATT == false && PowDown == false) {

                        elabBATT = true;

                        if (Settings.getInstance().getSetting("mainDebug", false)) {
                            System.out.println("AppMain: low battery signal received");
                        }

                        // Set new application state
                        InfoStato.getInstance().setSTATOexecApp(execBATTSCARICA);

                    } //msgBattScarica				

                    //*** [12g] APPLICATION CLOSURE / STAND-BY
                    if (GOTOstagePwDownSTANDBY == true) {
                        /* 
                         * When I finish to send positions through GPRS,
                         * I prevent from happening more than once this step 
                         */
                        GOTOstagePwDownSTANDBY = false;
                        PowDown = true;

                        /* 
                         * Inhibit key usage,
                         * device must go to power down mode
                         */
                        InfoStato.getInstance().setEnableCSD(false);

                        /*
                         *  If battery low, call GoToPowerDown without
                         *  activate motion sensor and without set awakening,
                         *  writing on file the closure reason (battery low)
                         */
                        if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execBATTSCARICA)) {
                            if (Settings.getInstance().getSetting("mainDebug", false)) {
                                System.out.println("AppMain, low battery: motion sensor DISABLED!");
                            }
                        } // If key deactivated, enable motion sensor and call 'GoToPowerDown'
                        else {
                            if (movsens) {
                                movSens.movSensOn();
                            }
                        }

                    } //GOTOstagePwDownSTANDBY

                    //*** [12i] RING EVENT, FOR CSD CALL (IF POSSIBLE)
                        /*
                     * 'msgRING' received and CSD procedure enabled
                     * I can activate CSD procedure and disable every other operation
                     * until CSD procedure will be finished
                     */
                    if (msgRicevuto.equalsIgnoreCase(msgRING) && InfoStato.getInstance().getEnableCSD() == true && InfoStato.getInstance().getCSDattivo() == false) {

                        if (Settings.getInstance().getSetting("mainDebug", false)) {
                            System.out.println("AppMain: received instruction to start CSD procedure");
                        }

                        // Init and start UpdateCSD
                        th7 = new UpdateCSD();
                        th7.setPriority(5);
                        th7.start();

                        // Wait for UpdateCSD answers to incoming call
                        Thread.sleep(2000);

                    } //msgRING

                    if (InfoStato.getInstance().getReboot()) {
                        System.out.println("Reboot for GPIO");
                        /*
                         if (InfoStato.getInstance().getInfoFileInt(UartNumTent) > 0) {
                         FlashFile.getInstance().setImpostazione(UartNumTent, "1");
                         InfoStato.getFile();
                         FlashFile.getInstance().writeSettings();
                         InfoStato.freeFile();
                         }
                         */
                        restart = true;
                        //break;
                    }

                    Thread.sleep(10);

                } catch (InterruptedException ie) {
                    new LogError("Interrupted Exception AppMain");
                    restart = true;
                } catch (Exception ioe) {
                    new LogError("Generic Exception AppMain");
                    restart = true;
                }
                if (restart == true) {
                    restart = false;
                    if (Settings.getInstance().getSetting("mainDebug", false)) {
                        System.out.println("AppMain: Reboot module in progress...");
                    }
                    System.out.println("Reboot for GPIO");
                    new LogError("Reboot for GPIO");
                    BatteryManager.getInstance().reboot();
                    break;
                }

                if (Settings.getInstance().getSetting("mainDebug", false)) {
                    System.out.println("AppMain: sleep in loop...");
                }

                Thread.sleep(1000);
            } //while(true)

            /* 
             * [13] POWER OFF MODULE AND CLOSE APPLICATION
             *
             * Please note:
             *      AT^SMSO cause call of destroyApp(true),
             * 		therefore AT^SMSO should not be called inside detroyApp()
             */
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("AppMain: Power off module in progress...");
            }

            CommGPSThread.getInstance().terminate = true;
            CommGPSThread.getInstance().join();

            InfoStato.getInstance().closeUDPSocketTask();
            InfoStato.getInstance().closeTCPSocketTask();

            SocketGPRSThread.getInstance().terminate = true;
            SocketGPRSThread.getInstance().join();

            powerDown();

            ATManager.getInstance().executeCommand("AT^SPIO=0\r");

            BatteryManager.getInstance().lowPowerMode();
            Thread.sleep(5000);

            ATManager.getInstance().executeCommand("AT^SMSO\r");

            /*try {
             if(Settings.getInstance().getSetting("mainDebug", false){
             System.out.println("XE DRIO STUARSE");
             }
             destroyApp(true);
             } catch (MIDletStateChangeException me) { 
             if(Settings.getInstance().getSetting("mainDebug", false){
             System.out.println("AppMain: MIDletStateChangeException"); 
             }
             }*/
        } catch (InterruptedException ie) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("Interrupted Exception AppMain");
            }
            new LogError("Interrupted Exception AppMain");
        } catch (Exception ioe) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("Generic Exception AppMain");
            }
            new LogError("Generic Exception AppMain");
        }
    } //startApp

    /**
     * Contains code to run the application when is in PAUSE.
     */
    protected void pauseApp() {
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("Application in pause...");
        }
        try {
            Thread.sleep(2000);
            destroyApp(true);
        } catch (InterruptedException ie) {
        } catch (MIDletStateChangeException me) {
        }
    } //pauseApp

    /**
     * Contains code to execute before destroy application.
     *
     * @throws	MIDletStateChangeException
     */
    protected void destroyApp(boolean cond) throws MIDletStateChangeException {
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("Close application in progress...");
        }

        // Destroy application
        notifyDestroyed();
    } //destroyApp

    private void powerDown() {
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("Th*GoToPowerDown: STARTED");
        }

        Date date = LocationManager.getInstance().dateLastFix();
        if (date != null) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("PowerDown @ last fix time " + date.toString());
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            String rtc = "at+cclk=\""
                    + (cal.get(Calendar.YEAR) - 2000) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DAY_OF_MONTH)
                    + ","
                    + cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND)
                    + "\"\r";

            ATManager.getInstance().executeCommand(rtc);
        }

        date = new Date();
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("PowerDown @ " + date.toString());
        }

        if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execBATTSCARICA)) {
            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("PowerDown on low battery, no wakeup call");
            }

        } else {
            date.setTime(date.getTime() + Settings.getInstance().getSetting("sleep", 6 * 3600) * 1000L);

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("PowerDown: setting wakeup call for " + date.toString());
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            String rtc = "at+cala=\""
                    + (cal.get(Calendar.YEAR) - 2000) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DAY_OF_MONTH)
                    + ","
                    + cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND)
                    + "\"\r";

            ATManager.getInstance().executeCommand(rtc);
        }

        if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execFIRST)) {

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execFIRST);
            }

            // Key deactivation FIRST
            Settings.getInstance().setSetting("closeMode", closeAppDisattivChiaveFIRST);
            //FlashFile.getInstance().setImpostazione(CloseMode, closeAppDisattivChiaveFIRST);
            // pay attention, MODIFY FOR DEBUG
            //FlashFile.getInstance().setImpostazione(CloseMode, closeAppFactory);

        } // NORMAL EXECUTION
        else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execNORMALE)) {

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execNORMALE);
            }

            // Normal OK
            if (LocationManager.getInstance().isFix()) {
                Settings.getInstance().setSetting("closeMode", closeAppNormaleOK);
            } else {
                Settings.getInstance().setSetting("closeMode", closeAppNormaleTimeout);
            }

        } // KEY DEACTIVATED
        else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEdisattivata)) {

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execCHIAVEdisattivata);
            }

            if (LocationManager.getInstance().isFix()) {
                Settings.getInstance().setSetting("closeMode", closeAppDisattivChiaveOK);
            } else {
                Settings.getInstance().setSetting("closeMode", closeAppDisattivChiaveTimeout);
            }

        } // MOVEMENT
        else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execMOVIMENTO)) {

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execMOVIMENTO);
            }

            // Movement OK
            Settings.getInstance().setSetting("closeMode", closeAppMovimentoOK);

            //FlashFile.getInstance().setImpostazione(CloseMode, closeAppMovimentoOK);
        } // AFTER RESET
        else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execPOSTRESET)) {

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execPOSTRESET);
            }

            // Key deactivation after RESET
            Settings.getInstance().setSetting("closeMode", closeAppPostReset);

            //FlashFile.getInstance().setImpostazione(CloseMode, closeAppPostReset);
        } // BATTERY LOW
        else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execBATTSCARICA)) {

            if (Settings.getInstance().getSetting("mainDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execBATTSCARICA);
            }

            // Battery Low
            Settings.getInstance().setSetting("closeMode", closeAppBatteriaScarica);
        }
    }

    public void movSensEvent(String Event) {
        if (Settings.getInstance().getSetting("mainDebug", false)) {
            System.out.println("movSensEvent " + Event);
        }

        if (Event.equalsIgnoreCase("^MOVE: 0")) {
            moved = false;
        } else if (Event.equalsIgnoreCase("^MOVE: 1")) {
            moved = true;
        }
    }

} //AppMain

