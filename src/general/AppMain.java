/* Class 	AppMain
 * 
 * This software is developed for Choral devices with Java
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

//#define DEBUGGING

import javax.microedition.midlet.*;
import javax.microedition.rms.*;

import choral.io.CheckUpgrade;
import choral.io.PowerManager;

import java.util.*;

/**
 * Main application class.
 * <BR>
 * startApp() method contains the main body of the application.
 *
 * @version	1.00 <BR> <i>Last update</i>: 28-05-2008
 * @author matteobo
 */
public class AppMain extends MIDlet implements GlobCost {

    /*
     * local variables
     */
    private String text;
    private String msgRicevuto = "";
    private boolean GOTOstagePwDownSTANDBY = false;
    private int countTimerResetGPS = 1;
//	private int	countTimerStartGPS = 1;
    private int countTimerStopGPS = 1;
    private int countTimerResetGPRS = 1;
//	private int	countTimerStartGPRS = 1;
    private int countTimerStopGPRS = 1;
    private boolean elabBATT, elabPWD = false;
//	private int TOgprsMovelength;
//	private boolean trackFIRST = false;
//	private boolean trackSMSenable = false;
//	private boolean checkCHIAVE = false;
    private boolean watchdog = true;
    private boolean PowDown = false;
    private boolean restart = false;
    private boolean puntatori = false;
    PowerManager pwr;
    int timeToOff = 0;

    /*
     * threads
     */
    /**
     * <BR> Thread for GPS receiver management (transparent mode) and position
     * strings creation.
     */
    CommGPStrasparent th1;
    /**
     * <BR> Thread for AT interface management.
     */
    ATsender th2;
    /**
     * <BR> Thread for sending position strings through GPRS connection (TCP or
     * UDP).
     */
    TrackingGPRS th3;
    /**
     * <BR> Thread for set module in POWER DOWN mode.
     */
    GoToPowerDown th4;
    /**
     * <BR> Thread for key management.
     */
    TestChiave th5;
    /**
     * <BR> Thread for GPIO management (excluding key).
     */
    GPIOmanager th6;
    /**
     * <BR> Thread for update configuration parameters through CSD call.
     */
    UpdateCSD th7;
    /**
     * <BR> Thread for (received) SMS management.
     */
    CheckSMS th8;
    /**
     * <BR> Thread for serial (ASC0) management.
     */
    Seriale th9;
    /**
     * <BR> Thread for TCP socket management.
     */
    SocketGPRStask th10;
    /**
     * <BR> Thread for UDP socket management.
     */
    UDPSocketTask th11;
    /**
     * <BR> Thread for crash detect management.
     */
	//AccelerometerTask	th12;

    /*
     * file and recordstore
     */
    /**
     * <BR> Manager for save and take data from configuration file.
     */
    SaveData save;
    
    Settings settings;
    PrioSem semAT;
    InfoStato infoS;
    Mailboxes mailboxes;
    FlashFile flashFile;

    /*
     * timer and tasks about GPS, GPRS e battery control
     */
    /**
     * <BR> Timer for FIX GPS timeout management.
     */
    Timer FIXgpsTimer;
    private boolean gpsTimerAlive = false;
    /**
     * <BR> Timer for GPRS timeout management.
     */
    Timer FIXgprsTimer;
//	private boolean gprsTimerAlive = false;
    /**
     * <BR> Task for execute operations when GPS timeout expires.
     */
    TimeoutTask FIXgpsTimeoutTask;
    /**
     * <BR> Task for execute operations when GPRS timeout expires.
     */
    TimeoutTask FIXgprsTimeoutTask;
    /**
     * <BR> Timer for cyclic monitoring of battery level.
     */
    Timer batteryTimer;
    /**
     * <BR> Task for execute operations about battery level monitoring.
     */
    TimeoutTask batteryTimeoutTask;
    /**
     * <BR> Timer for cyclic monitoring of network registration status.
     */
    Timer regTimer;
    /**
     * <BR> Task for execute operations about network registration status
     * monitoring.
     */
    TimeoutTask regTimeoutTask;
    /**
     * <BR> Timer for cyclic monitoring of WatchDog.
     */
    Timer WatchDogTimer;
    /**
     * <BR> Task for execute operations about WatchDog.
     */
    TimeoutTask WatchDogTimeoutTask;
    /**
     * <BR> Timer for delay tracking start when key is activated.
     */
    Timer trackTimer;
    private boolean trackTimerAlive = false;
    /**
     * <BR> Task for delay tracking start when key is activated.
     */
    TimeoutTask trackTimeoutTask;

    /*
     * constructors 
     */
    public AppMain() {

        CheckUpgrade fw = new CheckUpgrade("");

        if (debug) {
            System.out.println("AppMain: starting...");
        }

        
    //#ifdef DEBUGGING
        System.out.println("Running Greenwich OwnTracks Edition");
    //#endif

        
        settings = Settings.getInstance();
        settings.setfileURL("file:///a:/file/OwnTracks.properties");     
        semAT = SemAT.getInstance();
        infoS = InfoStato.getInstance();
        mailboxes = Mailboxes.getInstance();
        flashFile = FlashFile.getInstance();

		// Status info strcture creation
        // Threads creation
        th1 = new CommGPStrasparent();
        th2 = new ATsender();
        th3 = new TrackingGPRS();
        th4 = new GoToPowerDown();
        th5 = new TestChiave();
        th6 = new GPIOmanager();
        th8 = new CheckSMS();
        th9 = new Seriale();
        th10 = new SocketGPRStask();
        th11 = new UDPSocketTask();
		//th12 = new AccelerometerTask();
//		NMEAQueue = new Coda(dsNMEA, 100);
        // file and recordStre
        save = new SaveData();
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
        try {

            /*
             * standard application execution
             */
            if (!TESTenv) {

                /* 
                 * [1] INITIALIZATION AND START OF THREADS
                 * 
                 */
                // Set threads priority (default value=5, min=1, max=10)
                th1.setPriority(5);
                th2.setPriority(5);
                th3.setPriority(5);
                th4.setPriority(5);
                th5.setPriority(5);
                th6.setPriority(5);
                th8.setPriority(5);
                th9.setPriority(5);
                th10.setPriority(5);
                th11.setPriority(5);
				//th12.setPriority(5);

                puntatori = save.loadLog();
                if (!puntatori) {
                    InfoStato.getInstance().setInfoFileInt(TrkIN, "0");
                    InfoStato.getInstance().setInfoFileInt(TrkOUT, "0");
                }

                // Start ATsender thread 
                th2.start();

                /*
                 * [2] RECOVER SSYSTEM SETTINGS FROM CONFIGURATION FILE
                 * 
                 */
                if (debug) {
                    System.out.println("AppMain: Recover system settings in progress...");
                }
                String rsp = FlashFile.getInstance().loadSettings();
                /* 
                 * If configuration file not found, close application
                 *  
                 */
                if (rsp.equalsIgnoreCase("AppMain: FileNotFound")) {
                    destroyApp(true);
                }

                /*
                 * Recover settings
                 */
				//FlashFile.getInstance().setImpostazione(TrkIN, "0");
                //FlashFile.getInstance().setImpostazione(TrkOUT, "0");
                //temp
                InfoStato.getInstance().setInfoFileString(IDtraker, FlashFile.getInstance().getImpostazione(IDtraker));
                InfoStato.getInstance().setInfoFileString(PasswordCSD, FlashFile.getInstance().getImpostazione(PasswordCSD));
                InfoStato.getInstance().setInfoFileString(AppName, FlashFile.getInstance().getImpostazione(AppName));
                InfoStato.getInstance().setInfoFileString(CloseMode, FlashFile.getInstance().getImpostazione(CloseMode));
                if (debug) {
                    System.out.println("AppMain: Last closing of application: " + InfoStato.getInstance().getInfoFileString(CloseMode));
                }
                InfoStato.getInstance().setInfoFileString(LastGPSValid, FlashFile.getInstance().getImpostazione(LastGPSValid));
                if (debug) {
                    System.out.println("AppMain: Last valid string: " + InfoStato.getInstance().getInfoFileString(LastGPSValid));
                }
                InfoStato.getInstance().setInfoFileString(TrackingInterv, FlashFile.getInstance().getImpostazione(TrackingInterv));
                InfoStato.getInstance().setInfoFileString(Operatore, FlashFile.getInstance().getImpostazione(Operatore));
                InfoStato.getInstance().setInfoFileString(TrackingType, FlashFile.getInstance().getImpostazione(TrackingType));
                InfoStato.getInstance().setInfoFileString(TrackingProt, FlashFile.getInstance().getImpostazione(TrackingProt));
                InfoStato.getInstance().setInfoFileString(Header, FlashFile.getInstance().getImpostazione(Header));
                InfoStato.getInstance().setInfoFileString(Ackn, FlashFile.getInstance().getImpostazione(Ackn));
                InfoStato.getInstance().setInfoFileString(GprsOnTime, FlashFile.getInstance().getImpostazione(GprsOnTime));
                InfoStato.getInstance().setInfoFileString(TrkState, FlashFile.getInstance().getImpostazione(TrkState));
                InfoStato.getInstance().setInfoFileString(PublishTopic, FlashFile.getInstance().getImpostazione(PublishTopic));
                InfoStato.getInstance().setInfoFileString(SlpState, FlashFile.getInstance().getImpostazione(SlpState));
                InfoStato.getInstance().setInfoFileInt(StillTime, FlashFile.getInstance().getImpostazione(StillTime));
                InfoStato.getInstance().setInfoFileString(MovState, FlashFile.getInstance().getImpostazione(MovState));
                InfoStato.getInstance().setInfoFileString(IgnState, FlashFile.getInstance().getImpostazione(IgnState));
                InfoStato.getInstance().setInfoFileInt(UartSpeed, FlashFile.getInstance().getImpostazione(UartSpeed));
                InfoStato.getInstance().setInfoFileString(UartGateway, FlashFile.getInstance().getImpostazione(UartGateway));
                InfoStato.getInstance().setInfoFileString(UartHeaderRS, FlashFile.getInstance().getImpostazione(UartHeaderRS));
                InfoStato.getInstance().setInfoFileString(UartEndOfMessage, FlashFile.getInstance().getImpostazione(UartEndOfMessage));
                InfoStato.getInstance().setInfoFileInt(UartAnswerTimeOut, FlashFile.getInstance().getImpostazione(UartAnswerTimeOut));
                InfoStato.getInstance().setInfoFileInt(UartNumTent, FlashFile.getInstance().getImpostazione(UartNumTent));
                InfoStato.getInstance().setInfoFileString(UartEndOfMessageIP, FlashFile.getInstance().getImpostazione(UartEndOfMessageIP));
                InfoStato.getInstance().setInfoFileString(UartIDdisp, FlashFile.getInstance().getImpostazione(UartIDdisp));
                InfoStato.getInstance().setInfoFileInt(UartTXtimeOut, FlashFile.getInstance().getImpostazione(UartTXtimeOut));
                InfoStato.getInstance().setInfoFileInt(OrePowerDownOK, FlashFile.getInstance().getImpostazione(OrePowerDownOK));
                InfoStato.getInstance().setInfoFileInt(MinPowerDownOK, FlashFile.getInstance().getImpostazione(MinPowerDownOK));
                InfoStato.getInstance().setInfoFileInt(OrePowerDownTOexpired, FlashFile.getInstance().getImpostazione(OrePowerDownTOexpired));
                InfoStato.getInstance().setInfoFileInt(MinPowerDownTOexpired, FlashFile.getInstance().getImpostazione(MinPowerDownTOexpired));
                InfoStato.getInstance().setInfoFileString(DestHost, FlashFile.getInstance().getImpostazione(DestHost));
                if (debug) {
                    System.out.println("AppMain: destination host: " + InfoStato.getInstance().getInfoFileString(DestHost));
                }
                InfoStato.getInstance().setInfoFileString(DestPort, FlashFile.getInstance().getImpostazione(DestPort));
                InfoStato.getInstance().setInfoFileString(ConnProfileGPRS, FlashFile.getInstance().getImpostazione(ConnProfileGPRS));
                InfoStato.getInstance().setInfoFileString(apn, FlashFile.getInstance().getImpostazione(apn));
                InfoStato.getInstance().setInfoFileString(GPRSProtocol, FlashFile.getInstance().getImpostazione(GPRSProtocol));
                if (puntatori) {
                    InfoStato.getInstance().setInfoFileInt(TrkIN, FlashFile.getInstance().getImpostazione(TrkIN));
                    InfoStato.getInstance().setInfoFileInt(TrkOUT, FlashFile.getInstance().getImpostazione(TrkOUT));
                }
				//InfoStato.getInstance().setInfoFileInt(TrkIN, "0");
                //InfoStato.getInstance().setInfoFileInt(TrkOUT, "0");
                InfoStato.getInstance().setInfoFileString(InsensibilitaGPS, FlashFile.getInstance().getImpostazione(InsensibilitaGPS));
                /*
                 * Prepare settings file for any possible hardware reset or close of application
                 * due to shutdown or unexpected
                 */
                FlashFile.getInstance().setImpostazione(CloseMode, closeAppResetHW);
                FlashFile.getInstance().writeSettings();

                /*
                 * [3] DETERMINATION OF THE STATE WITH WHICH START THE APPLICATION
                 * 
                 */
                if (debug) {
                    System.out.println("GPS insensibility: " + InfoStato.getInstance().getInfoFileString(InsensibilitaGPS));
                    System.out.println("AppMain: Last CloseMode: " + InfoStato.getInstance().getInfoFileString(CloseMode));
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
                if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAppFactory)) {
                    InfoStato.getInstance().setSTATOexecApp(execFIRST);
                } // Reboot after AIRPLANE MODE
                else if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAIR)) {
                    InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                    // Set AIRPLANE MODE to deactivate radio parts of the module 
                    InfoStato.getInstance().setOpMode("AIRPLANE");
                    InfoStato.getInstance().setTipoRisveglio(risveglioCala);
                } // Normal OK
                else if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAppNormaleOK)) {
                    InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                } // Normal Timeout
                else if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAppNormaleTimeout)) {
                    InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                } // Key deactivation FIRST
                else if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAppDisattivChiaveFIRST)) {
                    InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                } // Key deactivation OK
                else if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAppDisattivChiaveOK)) {
                    InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                } // Key deactivation Timeout
                else if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAppDisattivChiaveTimeout)) {
                    InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                } // Motion sensor OK
                else if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAppMovimentoOK)) {
                    InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                } // Post Reset
                else if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAppPostReset)) {
                    InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                } // Low battery
                else if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAppBatteriaScarica)) {
                    InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                } // Hardware Reset
                else if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAppResetHW)) {
                    InfoStato.getInstance().setSTATOexecApp(execPOSTRESET);
                } // Closure due to low battery
                else if (InfoStato.getInstance().getInfoFileString(CloseMode).equalsIgnoreCase(closeAppBatteriaScarica)) {
                    InfoStato.getInstance().setSTATOexecApp(execPOSTRESET);
                } else {
                    if (debug) {
                        System.out.println("AppMain: ERROR, I can not determine the status of execution of the application!");
                    }
                    new LogError("AppMain: ERROR, I can not determine the status of execution of the application!");
                }

                if (debug) {
                    System.out.println(InfoStato.getInstance().getSTATOexecApp());
                }

                /*
                 *	SET SIM PIN
                 */
                SemAT.getInstance().getCoin(5);
                InfoStato.getInstance().writeATCommand("at+cpin=5555\r");
                SemAT.getInstance().putCoin();

                /*
                 * [4] START OF BATTERY LEVEL CONTROL
                 * 
                 */
                batteryTimer = new Timer();
                batteryTimeoutTask = new TimeoutTask(BatteryTimeout);
                batteryTimer.scheduleAtFixedRate(batteryTimeoutTask, 0, batteryTOvalue * 1000);

                regTimer = new Timer();
                regTimeoutTask = new TimeoutTask(RegTimeout);
                regTimer.scheduleAtFixedRate(regTimeoutTask, 0, regTOvalue * 1000);

                /*
                 * [5] RECOVER DATA FROM FLASH
                 * 
                 */
                try {
                    // Open RecordStore
                    RecordStore rs = RecordStore.openRecordStore(recordStoreName, true);

                    /*
                     * Record recovey:
                     * 1) Last GPRMC valid string
                     * 2) <empty> ...
                     */
                    byte b[] = rs.getRecord(1);
                    String str = new String(b, 0, b.length);
                    InfoStato.getInstance().setInfoFileString(LastGPRMCValid, str);
                    if (debug) {
                        System.out.println("RecordStore, record n.1: " + str);
                    }

                    // Chiusura RecordStore
                    rs.closeRecordStore();

                } catch (RecordStoreNotOpenException rsnoe) {
                    if (debug) {
                        System.out.println("FlashRecordStore: RecordStoreNotOpenException");
                    }
                } catch (RecordStoreFullException rsfe) {
                    if (debug) {
                        System.out.println("FlashRecordStore: RecordStoreFullException");
                    }
                } catch (RecordStoreException rse) {
                    if (debug) {
                        System.out.println("FlashRecordStore: RecordStoreException");
                    }
                } catch (IllegalArgumentException e) {
                    if (debug) {
                        System.out.println("FlashRecordStore: IllegalArgumentException");
                    }
                }

                /*
                 * [6a] SET AUTOSTART ONLY IF APPLICATION IS IN 
                 * 	    STATE 'execFIRST' OR 'execPOSTRESET'
                 * 
                 */
                if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execFIRST)
                        || InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execPOSTRESET)) {

                    SemAT.getInstance().getCoin(5);
                    if (debug) {
                        System.out.println("AppMain: Set AUTOSTART...");
                    }
                    InfoStato.getInstance().writeATCommand("at^scfg=\"Userware/Autostart/AppName\",\"\",\"a:/app/" + InfoStato.getInstance().getInfoFileString(AppName) + "\"\r");
                    InfoStato.getInstance().writeATCommand("at^scfg=\"Userware/Autostart/Delay\",\"\",10\r");
                    InfoStato.getInstance().writeATCommand("at^scfg=\"Userware/Autostart\",\"\",\"1\"\r");
                    if (usbDebug) {
                        InfoStato.getInstance().writeATCommand("at^scfg=\"Userware/StdOut\",USB\r");
                    } else {
                        InfoStato.getInstance().writeATCommand("at^scfg=\"Userware/StdOut\",ASC0\r");
                    }
                    SemAT.getInstance().putCoin();

                    InfoStato.getInstance().setSTATOexecApp(execNORMALE);

                } //AUTOSTART

                // Set wake up for ALIVE
                SemAT.getInstance().getCoin(5);
                InfoStato.getInstance().writeATCommand("ati\r");
                if (debug) {
                    System.out.println("AppMain: Set AT+CCLK");
                }
                InfoStato.getInstance().writeATCommand("at+cclk=\"02/01/01,00:00:00\"\r");
                InfoStato.getInstance().writeATCommand("at+cala=\"02/01/01,06:00:00\"\r");
                SemAT.getInstance().putCoin();

                /*
                 * [6b] SET AT^SBC TO ADJUST APPLICATION CONSUMPTION
                 * 
                 */
                SemAT.getInstance().getCoin(5);
                if (debug) {
                    System.out.println("AppMain: Set AT^SBC...");
                }
                InfoStato.getInstance().writeATCommand("AT^SBC=5000\r");
                SemAT.getInstance().putCoin();

                /*
                 * [6c] SET AT^SJNET FOR UDP CONNECTION (ALWAYS)
                 * 
                 */
                SemAT.getInstance().getCoin(5);
                if (debug) {
                    System.out.println("AppMain: Set AT^SJNET...");
                }
                InfoStato.getInstance().writeATCommand("at^sjnet=\"GPRS\",\"" + InfoStato.getInstance().getInfoFileString(apn) + "\",\"\",\"\",\"\",0\r");
                SemAT.getInstance().putCoin();

                /*
                 * [6d] SET AT+CGSN TO GET MODULE IMEI
                 *
                 */
                SemAT.getInstance().getCoin(5);
                if (debug) {
                    System.out.println("AppMain: Read IMEI...");
                }
                InfoStato.getInstance().writeATCommand("AT+CGSN\r");
                InfoStato.getInstance().writeATCommand("AT^SCKS=1\r");
                SemAT.getInstance().putCoin();


                /*
                 * [7] GPIO DRIVER ACTIVATION (FOR BOTH KEY AND MOTION SENSOR)
                 * 
                 */
                SemAT.getInstance().getCoin(5);
                if (debug) {
                    System.out.println("Th*GPIOmanager: Open GPIO driver...");
                }
                InfoStato.getInstance().writeATCommand("at^spio=1\r");
                SemAT.getInstance().putCoin();

                /*
                 * [8] CONTROL KEY ACTIVATED (GPIO n.7)
                 * key control must be done before the motion sensor!
                 */
                // Start thread TestChiave
                th5.start();

                // Wait until polling is enabled on GPIO key
                while (InfoStato.getInstance().getPollingAttivo() == false) {
                    Thread.sleep(whileSleep);
                }
                if (debug) {
                    System.out.println("AppMain: Polling is enabled on GPIO key!");
                }

                /*
                 * At this point I know if device was started
                 * due to the motion sensor, the key or + CALA 
                 */
                if (InfoStato.getInstance().getTipoRisveglio().equalsIgnoreCase(risveglioMovimento)) {
                    /*
                     * If awakening due to motion sensor
                     * go to state 'execMOVIMENTO'
                     */
                    if (debug) {
                        System.out.println("execMOVIMENTO");
                    }
                    InfoStato.getInstance().setSTATOexecApp(execMOVIMENTO);
                } //risveglioMovimento

                /*
                 * [9] DISABLING MOTION SENSOR
                 *
                 */
                InfoStato.getInstance().setDisattivaSensore(true);

                // Start GPIO Manager, motion sensor and WatchDog
                th6.start();
				//th12.start();

                // Start WatchDog timer			
                if (watchdog == true) {
                    WatchDogTimer = new Timer();
                    WatchDogTimeoutTask = new TimeoutTask(WatchDogTimeout);
                    WatchDogTimer.scheduleAtFixedRate(WatchDogTimeoutTask, 1000 * 60, WatchDogTOvalue * 1000);
                }

                // Wait until motion sensor is disabled
                if (debug) {
                    System.out.println("AppMain: waiting for disabling motion sensor...");
                }
                while (InfoStato.getInstance().getDisattivaSensore() == true) {
                    Thread.sleep(whileSleep);
                }
                if (debug) {
                    System.out.println("AppMain: Motion sensor disabled!");
                }

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
                SemAT.getInstance().getCoin(1);
                InfoStato.getInstance().writeATCommand("AT\r");
                InfoStato.getInstance().writeATCommand("at+crc=1\r");
                SemAT.getInstance().putCoin();

                // Wait for info about ^SYSSTART type
                while (InfoStato.getInstance().getOpMode() == null) {
                    Thread.sleep(whileSleep);
                }

                /* Power on radio parts of the module for safety */
                if (debug) {
                    System.out.println("AppMain: SWITCH ON RADIO PART of the module...");
                }
                SemAT.getInstance().getCoin(1);
                InfoStato.getInstance().setATexec(true);
                Mailboxes.getInstance(2).write("AT^SCFG=\"MEopMode/Airplane\",\"off\"\r");
                while (InfoStato.getInstance().getATexec()) {
                    Thread.sleep(whileSleep);
                }
                SemAT.getInstance().putCoin();

                SemAT.getInstance().getCoin(1);
                InfoStato.getInstance().setATexec(true);
                Mailboxes.getInstance(2).write("AT+CREG=1\r");
                while (InfoStato.getInstance().getATexec()) {
                    Thread.sleep(whileSleep);
                }
                SemAT.getInstance().putCoin();

                SemAT.getInstance().getCoin(1);
                InfoStato.getInstance().setATexec(true);
                Mailboxes.getInstance(2).write("AT+CGREG=1\r");
                while (InfoStato.getInstance().getATexec()) {
                    Thread.sleep(whileSleep);
                }
                SemAT.getInstance().putCoin();

				// Radio activation
                // Starting threads CommGPStrasparent, TrackingGPRS, CheckSMS e Seriale
                th1.start();
                th3.start();
                th8.start();
                th9.start();
                th10.start();
                th11.start();


                /* 
                 * [11] PREPARING TIMEOUTS ABOUT FIX GPS AND POSITIONING STRINGS SENDING
                 * 
                 */
                // GPS
                FIXgpsTimer = new Timer();
                if (debug) {
                    System.out.println("AppMain: GPS timer RESET n." + countTimerResetGPS);
                }
                countTimerResetGPS++;
                // GPS task and resources
                FIXgpsTimeoutTask = new TimeoutTask(FIXgpsTimeout);

                // GPRS
                FIXgprsTimer = new Timer();
                if (debug) {
                    System.out.println("AppMain: GPRS timer RESET n." + countTimerResetGPRS);
                }
                countTimerResetGPRS++;
                // GPRS task and resources
                FIXgprsTimeoutTask = new TimeoutTask(FIXgprsTimeout);

                /* Enable CSD */
                InfoStato.getInstance().setEnableCSD(true);

                /* 
                 * [12] WAIT FOR MESSAGES FROM THREADS AND MANAGEMENT
                 * 
                 *	From here AppMain waits for msg from threds and coordinates
                 * the operations to be carried out in accordance with the msg
                 * received, according to the priorities
                 */
                if (debug) {
                    System.out.println("AppMain, actual state of the application: " + InfoStato.getInstance().getSTATOexecApp());
                }

                if (InfoStato.getInstance().getCALA()) {
                    SemAT.getInstance().getCoin(1);
                    System.out.println("AppMain: Module reboot in progress...");
                    InfoStato.getInstance().setATexec(true);
                    Mailboxes.getInstance(2).write("AT+CFUN=1,1\r");
                    while (InfoStato.getInstance().getATexec()) {
                        Thread.sleep(whileSleep);
                    }
                    SemAT.getInstance().putCoin();
                }

                while (true) {

                    try {

                        SemAT.getInstance().getCoin(1);
                        InfoStato.getInstance().setATexec(true);
                        Mailboxes.getInstance(2).write("AT+CSQ\r");
                        while (InfoStato.getInstance().getATexec()) {
                            Thread.sleep(whileSleep);
                        }
                        SemAT.getInstance().putCoin();

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

                        if (msgRicevuto.equalsIgnoreCase(msgCloseGPRS)) {
                            //while(!th3.isAlive()){ Thread.sleep(whileSleep); }
                            th3.start();
                        }

							//*** [12a] KEY MANAGEMENT
                        /*
                         * ACTIVATION KEY event
                         * 
                         * (both in the case where the key is the cause of the
                         *  application startup and when the key is activated
                         * while the application is running in another state)
                         */
                        if (debug_main) {
                            System.out.println("msg? " + msgRicevuto);
                        }
                        if (msgRicevuto.equalsIgnoreCase(msgChiaveAttivata) && InfoStato.getInstance().getInibizioneChiave() == false) {

                            if (debug_chiave) {
                                System.out.println("AppMain: KEY activation detected...");
                            }

                            /*
                             * Origin state: 'execMOVIMENTO'
                             * 
                             * Suspend motion tracking, execute tracking with active KEY
                             * and go to STAND-BY
                             */
                            if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execMOVIMENTO)) {

                                if (debug) {
                                    System.out.println("AppMain,MOTION: key was activated!");
                                }

                                //InfoStato.getInstance().setSTATOexecApp(execCHIAVEattivata);
                                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                                Mailboxes.getInstance(3).write(trackAttivChiave);

                            } //execMOVIMENTO
                            else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEattivata)) {

                                if (debug) {
                                    System.out.println("AppMain, KEY ACTIVE: already in key active state!");
                                }
                                /*
                                 * Start tracking with active key, in there is a FIX
                                 */
                                //if (InfoStato.getInstance().getValidFIX() == true || gpsTimerAlive == false) {
                                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                                if (InfoStato.getInstance().getEnableGPRS() == true) {
                                    Mailboxes.getInstance(3).write(trackAttivChiave);
                                    if (debug) {
                                        System.out.println("AppMain: sent messageo: " + trackAttivChiave + " to tasksocket");
                                    }
                                } else if (debug) {
                                    System.out.println("AppMain: GPRS disabled");
                                }
									//}

                            }

                        } //msgChiaveAttivata

                        /*
                         *  TIMEOUT GPRS
                         *
                         *	if 'gprsTimeout' and 'gprsTimeoutAck'
                         * 
                         */
                        if (msgRicevuto.equalsIgnoreCase(gprsTimeoutStart)) {

                            //System.out.println("START GPRS TIMEOUT");
                            InfoStato.getInstance().setIfIsFIXgprsTimeoutExpired(false);
                            FIXgprsTimer = new Timer();
                            FIXgprsTimeoutTask = new TimeoutTask(FIXgprsTimeout);
                            FIXgprsTimer.schedule(FIXgprsTimeoutTask, 30 * 1000);

                        }

                        if (msgRicevuto.equalsIgnoreCase(gprsTimeoutStop)) {

                            //System.out.println("STOP GPRS TIMEOUT");
                            FIXgprsTimer.cancel();
                            FIXgprsTimeoutTask.cancel();

                        }

                        /*
                         * KEY DEACTIVATION event
                         * 
                         * (you can get here from the state 'execCHIAVEattivata'
                         *  but also from states 'execFIRST' and 'execPOSTRESET',
                         *  becOrigin stateause in these states I check the FIX only if the KEY
                         *  is activated and I would like to know if it has been disabled in the meantime)		  	
                         */
                        if (msgRicevuto.equalsIgnoreCase(msgChiaveDisattivata) && InfoStato.getInstance().getInibizioneChiave() == false) {
                            //if (msgRicevuto.equalsIgnoreCase(msgChiaveDisattivata)){	
                            if (debug_chiave) {
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
                                Mailboxes.getInstance(3).write(exitTrack);

                                // Wait for closure
                                while (InfoStato.getInstance().getTrackingAttivo() == true) {
                                    Thread.sleep(whileSleep);
                                }

                                GOTOstagePwDownSTANDBY = true;

                                // Set new application state
                                InfoStato.getInstance().setSTATOexecApp(execCHIAVEdisattivata);
                            } /*
                             * Case 'execMOVIMENTO' -> do nothing
                             */ else {
                                if (debug) {
                                    System.out.println("AppMain,CHIAVEdisattivata: nothing to do, key already deactivated!");
                                }
                                if (timeToOff == 50 || timeToOff == 70) {
                                    Mailboxes.getInstance(3).write(trackMovimento);
                                } else {
                                    if (timeToOff >= 100) {
                                        // Block everything before closing
                                        Mailboxes.getInstance(3).write(exitTrack);

                                        // Wait for closure
                                        while (InfoStato.getInstance().getTrackingAttivo() == true) {
                                            Thread.sleep(whileSleep);
                                        }

                                        GOTOstagePwDownSTANDBY = true;

                                        // Set new application state
                                        InfoStato.getInstance().setSTATOexecApp(execCHIAVEdisattivata);
                                    }
                                }
                                timeToOff++;
                                //System.out.println("timeToOff" + timeToOff);
                            }

                        } //msgChiaveDisattivata		

							//*** [12c] WAIT FOR VALID GPS FIX / 'FIXtimeout' EXPIRED
                        if (InfoStato.getInstance().getValidFIX() == false && InfoStato.getInstance().isFIXtimeoutExpired() == false) {
                            // No valid FIX and 'FIXtimeout' not expired
                            if (debug) {
                                System.out.println("AppMain: waiting for 'validFIX'...");
                            }
                        }

							//*** [12e]	WAIT FOR SENDING A VALID GPS FIX WITH SUCCESS THROUGH GPRS
                        //***  		OR 'FIXgprsTimeout' EXPIRED
                        if (debug_main) {
                            System.out.println(msgRicevuto);
                        }
                        /*
                         * 'msgFIXgprs' received (within 'FIXgprsTimeout')
                         * I have at least a valid GPS FIX,
                         * go to application closure procedure
                         */
                        if (msgRicevuto.equalsIgnoreCase(msgFIXgprs) && InfoStato.getInstance().getCSDattivo() == false && ((InfoStato.getInstance().getInfoFileString(TrkState)).equalsIgnoreCase("ON") || (InfoStato.getInstance().getInfoFileString(TrkState)).equalsIgnoreCase("ON,FMS"))) {

                            if (debug) {
                                System.out.println("AppMain: valid GPS FIX sent through GPRS connection with success");
                            }
                            InfoStato.getInstance().setValidFIXgprs(true);

                            // If KEY is activated, repeat tracking
                            if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEattivata) || InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execNORMALE)) {

                                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                                Mailboxes.getInstance(3).write(trackNormale);
                                trackTimerAlive = true;
                            } else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execMOVIMENTO)) {

                                /*
                                 * Stop timer and reset for a new execution
                                 */
                                FIXgprsTimer.cancel();
                                //gprsTimerAlive = false;
                                FIXgprsTimeoutTask.cancel();
                                if (debug) {
                                    System.out.println("AppMain: GPRS timer STOP n." + countTimerStopGPRS);
                                }
                                countTimerStopGPRS++;

                                FIXgprsTimer = new Timer();
                                if (debug) {
                                    System.out.println("AppMain: GPRS timer RESET n." + countTimerResetGPRS);
                                }
                                countTimerResetGPRS++;
                                FIXgprsTimeoutTask = new TimeoutTask(FIXgprsTimeout);

									// invokes GoToPowerDown
                                //GOTOstagePwDownSTANDBY = true;
                            }

								// other cases (including low battery)
                            //else GOTOstagePwDownSTANDBY = true;
                        } //msgFIXgprs

                        if (msgRicevuto.equalsIgnoreCase(msgALIVE) && InfoStato.getInstance().getCSDattivo() == false && ((InfoStato.getInstance().getInfoFileString(TrkState)).equalsIgnoreCase("ON") || (InfoStato.getInstance().getInfoFileString(TrkState)).equalsIgnoreCase("ON,FMS"))) {

                            if (debug) {
                                System.out.println("AppMain: Alive message");
                            }
                            InfoStato.getInstance().setValidFIXgprs(true);

                            SemAT.getInstance().getCoin(5);
                            if (debug) {
                                System.out.println("AppMain: Set AT+CCLK");
                            }
                            InfoStato.getInstance().writeATCommand("at+cclk=\"02/01/01,00:00:00\"\r");
                            InfoStato.getInstance().writeATCommand("at+cala=\"02/01/01,06:00:00\"\r");
                            SemAT.getInstance().putCoin();

                            // If KEY is activated, repeat tracking
                            if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEattivata) || InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execNORMALE)) {

                                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                                Mailboxes.getInstance(3).write(trackAlive);
                                trackTimerAlive = true;
                                if (InfoStato.getInstance().getInfoFileInt(UartNumTent) > 0) {
                                    InfoStato.getInstance().setInfoFileInt(UartNumTent, "0");
                                    FlashFile.getInstance().setImpostazione(UartNumTent, "0");
                                    InfoStato.getFile();
                                    FlashFile.getInstance().writeSettings();
                                    InfoStato.freeFile();
                                    Mailboxes.getInstance(3).write(trackUrcSim);
                                }
                            }

                        } //msgAlive

                        if (msgRicevuto.equalsIgnoreCase(msgALR1) && InfoStato.getInstance().getCSDattivo() == false && ((InfoStato.getInstance().getInfoFileString(TrkState)).equalsIgnoreCase("ON") || (InfoStato.getInstance().getInfoFileString(TrkState)).equalsIgnoreCase("ON,FMS"))) {

                            if (debug) {
                                System.out.println("AppMain: rilevato ALLARME INPUT 1");
                            }
                            InfoStato.getInstance().setValidFIXgprs(true);

                            // If KEY is activated, repeat tracking
                            if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEattivata) || InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execNORMALE)) {

                                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                                Mailboxes.getInstance(3).write(trackAlarmIn1);
                                trackTimerAlive = true;
                            }

                        } //msgALR1

                        if (msgRicevuto.equalsIgnoreCase(msgALR2) && InfoStato.getInstance().getCSDattivo() == false && ((InfoStato.getInstance().getInfoFileString(TrkState)).equalsIgnoreCase("ON") || (InfoStato.getInstance().getInfoFileString(TrkState)).equalsIgnoreCase("ON,FMS"))) {

                            if (debug) {
                                System.out.println("AppMain: rilevato ALLARME INPUT 2");
                            }
                            InfoStato.getInstance().setValidFIXgprs(true);

                            // If KEY is activated, repeat tracking
                            if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEattivata) || InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execNORMALE)) {

                                InfoStato.getInstance().setSTATOexecApp(execNORMALE);
                                Mailboxes.getInstance(3).write(trackAlarmIn2);
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

                            if (debug) {
                                System.out.println("AppMain: low battery signal received");
                            }

                            /*
                             * Se è attivo il timer GPS -> STOP e RESET
                             */
                            if (gpsTimerAlive == true) {

                                FIXgpsTimer.cancel();
                                gpsTimerAlive = false;
                                FIXgpsTimeoutTask.cancel();
                                if (debug) {
                                    System.out.println("AppMain: GPS timer STOP n." + countTimerStopGPS);
                                }
                                countTimerStopGPS++;

                                FIXgpsTimer = new Timer();
                                if (debug) {
                                    System.out.println("AppMain: GPS timer RESET n." + countTimerResetGPS);
                                }
                                countTimerResetGPS++;
                                FIXgpsTimeoutTask = new TimeoutTask(FIXgpsTimeout);
                            }

                            /*
                             * Block GPRS timer in state 'execMOVIMENTO'
                             */
                            if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execMOVIMENTO)) {

                                /*
                                 * Only GPRS timer is running, reset it
                                 */
                                FIXgprsTimer.cancel();
                                //gprsTimerAlive = false;
                                FIXgprsTimeoutTask.cancel();
                                if (debug) {
                                    System.out.println("AppMain: GPRS timer STOP n." + countTimerStopGPRS);
                                }
                                countTimerStopGPRS++;

                                FIXgprsTimer = new Timer();
                                if (debug) {
                                    System.out.println("AppMain: GPRS timer RESET n." + countTimerResetGPRS);
                                }
                                countTimerResetGPRS++;
                                FIXgprsTimeoutTask = new TimeoutTask(FIXgprsTimeout);
                            }

                            // Set new application state
                            InfoStato.getInstance().setSTATOexecApp(execBATTSCARICA);

                            /*
                             * Start tracking low battery
                             */
                            if (InfoStato.getInstance().getEnableGPRS() == true) {
//									InfoStato.getInstance().setNumTrak(InfoStato.getInstance().getInfoFileInt(NumTrakNormal));
                                Mailboxes.getInstance(3).write(trackBatteria);
                                if (debug) {
                                    System.out.println("AppMain: sent message: " + trackBatteria);
                                }
                            } else /*
                             * Close as low battery
                             */ {
                                FlashFile.getInstance().setImpostazione(CloseMode, closeAppBatteriaScarica);
                            }
                            InfoStato.getFile();
                            FlashFile.getInstance().writeSettings();
                            InfoStato.freeFile();
                            /*
                             * Don't start GPRS timeout, try to send strings until battery is fully discharged
                             */

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
                            InfoStato.getInstance().setInibizioneChiave(true);
                            InfoStato.getInstance().setEnableCSD(false);

                            /*
                             *  If battery low, call GoToPowerDown without
                             *  activate motion sensor and without set awakening,
                             *  writing on file the closure reason (battery low)
                             */
                            if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execBATTSCARICA)) {
                                if (debug) {
                                    System.out.println("AppMain, low battery: motion sensor DISABLED!");
                                }
                            } // If key deactivated, enable motion sensor and call 'GoToPowerDown'
                            else {
                                if (movsens) {
                                    /*
                                     * ENABLING MOTION SENSOR
                                     */
                                    // Enable motion sensor
                                    InfoStato.getInstance().setAttivaSensore(true);

                                    // Wait until motion sensor is running
                                    while (InfoStato.getInstance().getAttivaSensore() == true) {
                                        Thread.sleep(whileSleep);
                                    }
                                    if (debug) {
                                        System.out.println("AppMain: motion sensor ENABLED!");
                                    }
                                }
                            }

                            /*
                             * Start thread 'GoToPowerDown' to get ready module for Power Down
                             */
                            if (elabPWD == false) {
                                elabPWD = true;
                                th4.start();
                            }

                        } //GOTOstagePwDownSTANDBY

							//*** [12h] EXIT LOOP TO CLOSE APPLICATION
                        /*
                         * Receive 'msgClose'
                         * I can put module in Power Down mode
                         */
                        if (msgRicevuto.equalsIgnoreCase(msgClose)) {
                            if (debug) {
                                System.out.println("AppMain: received instruction to close application");
                            }
                            break;	//break while loop
                        } //msgClose

							//*** [12i] RING EVENT, FOR CSD CALL (IF POSSIBLE)
                        /*
                         * 'msgRING' received and CSD procedure enabled
                         * I can activate CSD procedure and disable every other operation
                         * until CSD procedure will be finished
                         */
                        if (msgRicevuto.equalsIgnoreCase(msgRING) && InfoStato.getInstance().getEnableCSD() == true && InfoStato.getInstance().getCSDattivo() == false) {

                            if (debug) {
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
                            if (InfoStato.getInstance().getInfoFileInt(UartNumTent) > 0) {
                                FlashFile.getInstance().setImpostazione(UartNumTent, "1");
                                InfoStato.getFile();
                                FlashFile.getInstance().writeSettings();
                                InfoStato.freeFile();
                            }
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
                        InfoStato.getFile();
                        save.writeLog();
                        InfoStato.freeFile();
                        restart = false;
                        SemAT.getInstance().getCoin(1);
                        if (debug) {
                            System.out.println("AppMain: Reboot module in progress...");
                        }
                        System.out.println("Reboot for GPIO");
                        new LogError("Reboot for GPIO");
                        pwr = new PowerManager();
                        pwr.setReboot();
                        //Mailboxes.getInstance(2).write("AT+CFUN=1,1\r");
                        while (InfoStato.getInstance().getATexec()) {
                            Thread.sleep(whileSleep);
                        }
                        SemAT.getInstance().putCoin();
                        break;
                    }

                } //while(true)

                /* 
                 * [13] POWER OFF MODULE AND CLOSE APPLICATION
                 *
                 * Please note:
                 *      AT^SMSO cause call of destroyApp(true),
                 * 		therefore AT^SMSO should not be called inside detroyApp()
                 */
                SemAT.getInstance().getCoin(1);
                if (debug) {
                    System.out.println("AppMain: Power off module in progress...");
                }
                InfoStato.getInstance().closeTrackingGPRS();
                InfoStato.getInstance().closeUDPSocketTask();
                InfoStato.getInstance().closeTCPSocketTask();
                th3.join();
                th10.join();
                th11.join();
                InfoStato.getFile();
                save.writeLog();
                InfoStato.freeFile();
                InfoStato.getInstance().setATexec(true);
                Mailboxes.getInstance(2).write("AT^SPIO=0\r");
                while (InfoStato.getInstance().getATexec()) {
                    Thread.sleep(whileSleep);
                }
                pwr = new PowerManager();
                pwr.setLowPwrMode();
                Thread.sleep(5000);
                InfoStato.getInstance().setATexec(true);
                Mailboxes.getInstance(2).write("AT^SMSO\r");
                while (InfoStato.getInstance().getATexec()) {
                    Thread.sleep(whileSleep);
                }
                SemAT.getInstance().putCoin();

                /*try {
                 if(debug){
                 System.out.println("XE DRIO STUARSE");
                 }
                 destroyApp(true);
                 } catch (MIDletStateChangeException me) { 
                 if(debug){
                 System.out.println("AppMain: MIDletStateChangeException"); 
                 }
                 }*/
            } /*
             * APPLICATION TEST ENVIRONMENT
             */ else if (TESTenv) {
                if (debug) {
                    System.out.println("Application test environment");
                }
            } //TEST ENVIRONMENT

        } catch (InterruptedException ie) {
            if (debug) {
                System.out.println("Interrupted Exception AppMain");
            }
            new LogError("Interrupted Exception AppMain");
        } catch (Exception ioe) {
            if (debug) {
                System.out.println("Generic Exception AppMain");
            }
            new LogError("Generic Exception AppMain");
        }
    } //startApp

    /**
     * Contains code to run the application when is in PAUSE.
     */
    protected void pauseApp() {
        if (debug) {
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
        if (debug) {
            System.out.println("Close application in progress...");
        }

        // Destroy application
        notifyDestroyed();
    } //destroyApp

} //AppMain

