/*	
 * Class 	InfoStato
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

/**
 * Contains all informations about thread and task status, to be used and
 * exchanged between application classes.
 *
 * @version	1.04 <BR> <i>Last update</i>: 04-12-2007
 * @author alessioza
 *
 */
public class InfoStato implements GlobCost {

    private int numSMS, maxSMS;
    private int codSMS = -1;
    //private boolean validSMS = false;
    private String numTelSMS = null;
    private String validOP = "no rete";
    private int GPIOvalue;
    private int digInput0 = 0;
    private int digInput1 = 0;
    private int digInput2 = 0;
    private int digInput3 = 0;
    private boolean CSDWatchDog = false;
    private boolean attivazioneSensore = false;
    private boolean disattivazioneSensore = false;
    private boolean attivazionePolling = false;
    private String STATOexecApp;
    private boolean appSTANDBY = false;
    private boolean enableCSD = false;
    private boolean CSDconnect = false;
    private boolean CSDattivo = false;
    private boolean trackingInCorso = false;
    private boolean SMSsent = false;
    private String value1, value2, value3, value4, value5, value6, value7;
    private String temp;
    private int tempInt;
    private String dataGPRMC = null;
    private String oraGPRMC = null;
    private int GPIOnumberTEST;
    private double distance;
    private double DFSSpeed = 0.0;
    private double TrkSpeed = 0.0;
    private double DFSPreSpeed = 0.0;
    private int GreeSpeed = 0;
    private boolean InvioCoordinataStop = true;

    // Settings for configuration file
    private String entryPointUDPfile;
    private String typeTRK;
    private String protTRK;
    private String acknowledge;
    private String opNum;
    private String slp;
    private String movsens;
    private String ign;
    private String uGW;
    private String uHead;
    private String uEOMrs;
    private String uEOMip;
    private String uID;
    private String dataRAM = "";
    private String rmc = "";
    private String gga = "";
    private int stilltm;
    private int uSpeed;
    private int uATO;
    private int uNumT;
    private int uTXto;
    private int trackInterval;
    private boolean preAlive = false;
    private static boolean free_coda = true;
    private static boolean free_file = true;
    private String[] record = new String[codaSize];
    private String[][] recordMqtt = new String[codaSize][21];
    private String commandSMS = "";
    private boolean riavvia = false;
    private String codiceTastiera = "";
    private int dataX = 0;
    private int dataY = 0;
    private int dataZ = 0;
    private boolean closeTrack = false;
    private boolean closeUDP = false;
    private boolean closeTCP = false;
    private boolean canbusState = false;
    private boolean alarmNack = false;
    private int t2 = 0;
    private boolean t1WD = false, t2WD = false, t3WD = false;
    private String[] dataMqttRAM = null;
    private int counterIn1 = 0;
    private int counterIn2 = 0;
    private boolean waitAlarmVinSMS = false;
    private boolean gps_state = true;
    private boolean powerSupply = false;
    private boolean powerSupplyOff = false;
    
    public Queue smsQ;

    private InfoStato() {
        smsQ = new Queue(100, "smsQ");
    }

    public static InfoStato getInstance() {
        return InfoStatoHolder.INSTANCE;
    }

    private static class InfoStatoHolder {

        private static final InfoStato INSTANCE = new InfoStato();
    }

    /**
     * To set network operator
     *
     * @param	value	network operator
     */
    public synchronized void setOpRete(String value) {
        validOP = value;
    }

    /**
     * To get network operator
     *
     * @return	network operator
     */
    public synchronized String getOpRete() {
        return validOP;
    }

    /**
     * To set an INPUT value
     *
     * @param	value	an INPUT value
     */
    public synchronized void setDigitalIN(int value, int number) {
        // Digital Input 0 = GPIO7 = KEY
        if (number == 0) {
            if (value == 0) {
                digInput0 = 1;
            }
            if (value == 1) {
                digInput0 = 0;
            }
        }
        // Digital Input 1 = GPIO1	
        if (number == 1) {
            if (value == 0) {
                digInput1 = 1;
            }
            if (value == 1) {
                digInput1 = 0;
            }
        }
        // Digital Input 2 = GPIO3
        if (number == 2) {
            if (value == 0) {
                digInput2 = 1;
            }
            if (value == 1) {
                digInput2 = 0;
            }
        }
        // Digital Input 3 = 0
    }

    /**
     * To get an INPUT value
     *
     * @return	an INPUT value
     */
    public synchronized String getDigitalIN() {
        // calculate minimal string
        tempInt = digInput1 * 1 + digInput2 * 2 + digInput3 * 4 + digInput0 * 8;
        //System.out.println("Number to convert: " + digInput0+":"+digInput1+":"+digInput2+":"+tempInt);
        return ("0" + Integer.toHexString(tempInt)).toUpperCase();
    }

    /**
     * To get an INPUT value
     *
     * @return	an INPUT value
     */
    public synchronized String getDigitalIN(int num) {
        // calculate minimal string
        switch (num) {
            case 1:
                tempInt = 1 * 1 + digInput2 * 2 + digInput3 * 4 + digInput0 * 8;
                break;
            case 2:
                tempInt = digInput1 * 1 + 1 * 2 + digInput3 * 4 + digInput0 * 8;
                break;
            case 3:
                tempInt = 0 * 1 + digInput2 * 2 + digInput3 * 4 + digInput0 * 8;
                break;
            case 4:
                tempInt = digInput1 * 1 + digInput2 * 2 + digInput3 * 4 + digInput0 * 8;
                break;
            default:
                tempInt = digInput1 * 1 + 0 * 2 + digInput3 * 4 + digInput0 * 8;
                break;
        }
        return ("0" + Integer.toHexString(tempInt)).toUpperCase();
    }
    /**
     * To set indication about CSD activation
     *
     * @param	value	indication about CSD activation
     */
    public synchronized void setCSDattivo(boolean value) {
        CSDattivo = value;
    }

    /**
     * To get indication about CSD activation
     *
     * @return	indication about CSD activation
     */
    public synchronized boolean getCSDattivo() {
        return CSDattivo;
    }

    /**
     * To set CSD enabled
     *
     * @param	value	'true' if enabled, 'false' otherwise
     */
    public synchronized void setEnableCSD(boolean value) {
        enableCSD = value;
    }

    /**
     * To get CSD enabled indication
     *
     * @return	'true' if enabled, 'false' otherwise
     */
    public synchronized boolean getEnableCSD() {
        return enableCSD;
    }

    /**
     * To set if made a CSD connection
     *
     * @param	value	'true' if made a CSD connection, 'false' otherwise
     */
    public synchronized void setCSDconnect(boolean value) {
        CSDconnect = value;
    }

    /**
     * To get if made a CSD connection
     *
     * @return	'true' if made a CSD connection, 'false' otherwise
     */
    public synchronized boolean getCSDconnect() {
        return CSDconnect;
    }

    /**
     * To set indication about WatchDog activation on CSD call
     *
     * @param	value	indication about WatchDog activation on CSD call
     */
    public synchronized void setCSDWatchDog(boolean value) {
        CSDWatchDog = value;
    }

    /**
     * To get indication about WatchDog activation on CSD call
     *
     * @return	indication about WatchDog activation on CSD call
     */
    public synchronized boolean getCSDWatchDog() {
        return CSDWatchDog;
    }

}
