/*	
 * Class 	SocketGPRStask
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import java.io.*;
import javax.microedition.io.*;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import com.cinterion.io.BearerControl;
import com.m2mgo.util.GPRSConnectOptions;
import java.util.Date;

/**
 * Task that tales care of sending strings through a GPRS connection using a TCP
 * socket service
 *
 * @version	1.01 <BR> <i>Last update</i>: 05-10-2007
 * @author alessioza
 *
 */
public class SocketGPRStask extends Thread implements GlobCost {
    /* 
     * local variables
     */

    private double ctrlSpeed = 0;
    private int lettura;
    private boolean close = false;
    private int temp;
    private boolean ram = true;
    /**
     * Full string to send through GPRS
     */
    private String outText;
    private String[] outTextMqtt;
    // socket TCP
    SocketConnection sockConn;
    OutputStream out;
    InputStream in;
    //private String	destAddressTCP;
    private int val_insensibgps;
    private int timeOutSocket = 0;
    private boolean errorSent = false;
    BCListenerCustom list;
    String answer = "";
    int nackCount = 0;
    int errorCount = 0;
    int timer3 = 0;

    private String clientId = "EGS5x-";

    //private String apn = "internetm2m.air.com"; //"internet";
    private boolean firstTime = true;
    SocketGPRStask thisTask;

    /* 
     * constructors
     */
    public SocketGPRStask() {
        if (debug) {
            System.out.println("TT*SocketGPRStask: CREATED");
        }
        thisTask = this;
    }

    /*
     * methods
     */
    /**
     * Taske execution code:
     * <BR> ---------- <BR>
     * Performed operations: <br>
     * <ul type="disc">
     * <li> Add alarm to position strings if necessary;
     * <li> Send string;
     * <li> Check on number of strings sent.
     * </ul>
     */
    public void run() {

        GPRSConnectOptions.getConnectOptions().setAPN(InfoStato.getInstance().getInfoFileString(apn));
        GPRSConnectOptions.getConnectOptions().setBearerType("gprs");

        list = new BCListenerCustom();
        BearerControl.addListener(list);

        while (!InfoStato.getInstance().isCloseTCPSocketTask()) {

            if (debug) {
                System.out.println("SOCKET TASK ACTIVE");
                System.out.println("TrkState" + InfoStato.getInstance().getInfoFileString(TrkState));
                System.out.println("GPRSProtocol" + InfoStato.getInstance().getInfoFileString(GPRSProtocol));
                System.out.println("TrkIN" + InfoStato.getInstance().getInfoFileInt(TrkIN));
                System.out.println("TrkOUT" + InfoStato.getInstance().getInfoFileInt(TrkOUT));
                System.out.println("DataRAM" + InfoStato.getInstance().getDataRAM());
            }
            if ((InfoStato.getInstance().getInfoFileString(TrkState).equals("ON")
                    || (InfoStato.getInstance().getInfoFileString(TrkState)).equalsIgnoreCase("ON,FMS"))
                    && InfoStato.getInstance().getInfoFileString(GPRSProtocol).equals("TCP")
                    && ((InfoStato.getInstance().getInfoFileInt(TrkIN) != InfoStato.getInstance().getInfoFileInt(TrkOUT))
                    || !InfoStato.getInstance().getDataRAM().equals(""))) {

                if (debug) {
                    System.out.println("In TRK ON");
                }
                doTrackingOn();
            } else {
                if (debug) {
                    System.out.println("In no TRK ON");
                }
                try {
                    if (InfoStato.getInstance().getInfoFileString(TrkState).equals("OFF")) {

                        if (!firstTime) {
                            SemAT.getInstance().getCoin(5);
                            MQTTHandler.getInstance().disconnect();
                            SemAT.getInstance().putCoin();
                        }
                        if (debug) {
                            System.out.println("In TRK OFF");
                        }

                        InfoStato.getInstance().setTRKstate(false);
                        InfoStato.getInstance().setEnableCSD(true);
                        SemAT.getInstance().putCoin();	// release AT interface
                        SemAT.getInstance().getCoin(5);
                        // Close GPRS channel
                        //System.out.println("SocketGPRSTask: KILL GPRS");
                        InfoStato.getInstance().writeATCommand("at+cgatt=0\r");
                        SemAT.getInstance().putCoin();
                        InfoStato.getInstance().settrasmetti(true); //[MB] 20140530
                        InfoStato.getInstance().setApriGPRS(true);	//[MB] 20140530
                    }
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }

            // Variable to test if task working
            timer3++;
            InfoStato.getInstance().setTask3Timer(timer3);
            InfoStato.getInstance().setTickTask3WD();

        }// while
    } //run
    
    void doTrackingOn() {
        try {

                    // Indication about SOCKET TASK ACTIVE
            //System.out.println("TT*SocketGPRStask: START");
            InfoStato.getInstance().setIfsocketAttivo(true);
					//destAddressTCP = "socket://" + InfoStato.getInstance().getInfoFileString(DestHost) + ":" + InfoStato.getInstance().getInfoFileString(DestPort) + ";" + InfoStato.getInstance().getInfoFileString(ConnProfileGPRS) + ";timeout=0";

            /*
             * Once this task has been started, it is completely
             * finished before proceeding to a re-use, even if the
             * timeout expires (so there may be a FIX GPRS timeout
             * expired!)
             */
            //System.out.println("SOCKET TASK ACTIVE - VERIFY POINTERS");
            try {
                try {
                    // queue semaphore
                    while (!InfoStato.getCoda()) {
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                }

                // Verify if data are in RAM or FLASH	
                if (InfoStato.getInstance().getInfoFileInt(TrkIN) == InfoStato.getInstance().getInfoFileInt(TrkOUT)) {
                    //System.out.println("TT*SocketGPRStask: data from RAM");
                    outText = InfoStato.getInstance().getDataRAM();
							//outTextMqtt = InfoStato.getInstance().getDataMqttRAM();
                    //.out.println("data from RAM");
							/*for(int ind = 0; ind < outTextMqtt.length; ind++)
                     System.out.println(outTextMqtt[ind]);*/
                    //System.out.println("data: "+ outText);
                    ram = true;
                } else {
                    ram = false;
                    temp = InfoStato.getInstance().getInfoFileInt(TrkOUT);
                    //System.out.println("TT*SocketGPRStask: pointer out - " + temp);
                    if ((temp >= codaSize) || temp < 0) {
                        temp = 0;
                    }
                    outText = InfoStato.getInstance().getRecord(temp);
							//outTextMqtt = InfoStato.getInstance().getMqttRecord(temp);
                    //new LogError("TT*SocketGPRSTask: pointer out - " + temp + " " + outText);
                    //System.out.println("TT*SocketGPRStask: data in queue: ");
                    //System.out.println("data from flash");
							/*for(int ind = 0; ind < outTextMqtt.length; ind++)
                     System.out.println(outTextMqtt[ind]);
                     */

                    //System.out.println("TT*SocketGPRStask: Free Coda");
                }

						// Print string to send
                //System.out.println("TT*SocketGPRStask: String to sent through GPRS:\r\n");
						/*for(int ind = 0; ind < outTextMqtt.length; ind++)
                 System.out.println(outTextMqtt[ind]);
                 */
                //new LogError("GPRS string: " + outText);
                ctrlSpeed = InfoStato.getInstance().getSpeedForTrk();
                if (debug_speed) {
                    ctrlSpeed = InfoStato.getInstance().getSpeedGree();
                    //System.out.println("SPEED " + ctrlSpeed);
                }

                try {
                    val_insensibgps = Integer.parseInt(InfoStato.getInstance().getInfoFileString(InsensibilitaGPS));
                } catch (NumberFormatException e) {
                    //new LogError("NumberFormatException");
                    val_insensibgps = 0;
                }
                //new LogError("Velocoita attuale: " + ctrlSpeed + ". Val insens: " + val_insensibgps);

                if (ram) {

                    if ((ctrlSpeed >= val_insensibgps)) {
                        //System.out.println("Speed check ok.");
                        InfoStato.getInstance().settrasmetti(true);
                        if (InfoStato.getInstance().getInvioStop() == true) {
                            InfoStato.getInstance().setApriGPRS(true);
                        }
                        InfoStato.getInstance().setInvioStop(false);

                    } else {
                        if ((outText.indexOf("ALARM") > 0) || (outText.indexOf("ALIVE") > 0) || (outText.indexOf("COD<") > 0) || (outText.indexOf("URC SIM") > 0)) {
                                    //if((outTextMqtt[ALR_IND].indexOf("ALARM")>0) || (outTextMqtt[ALR_IND].indexOf("ALIVE")>0) || (outTextMqtt[ALR_IND].indexOf("COD<")>0) || (outTextMqtt[ALR_IND].indexOf("URC SIM")>0)){
                            //System.out.println("Alarm");
                            InfoStato.getInstance().settrasmetti(true);
                            InfoStato.getInstance().setApriGPRS(true);
                        } else {

                            if ((!InfoStato.getInstance().getPreAlive()) && (ctrlSpeed <= val_insensibgps) && (InfoStato.getInstance().getPreSpeedDFS() > val_insensibgps)) {

                                //System.out.println("Speed check less then insensitivity, previous speed is greater");
                                InfoStato.getInstance().settrasmetti(true);
                                if (InfoStato.getInstance().getInvioStop() == true) {
                                    InfoStato.getInstance().setApriGPRS(true);
                                }
                                InfoStato.getInstance().setInvioStop(false);

                            } else {

                                //System.out.println("Speed check failed.");
                                if (InfoStato.getInstance().getInvioStop() == false) {
                                    //System.out.println("Send stop coordinate.");
                                    InfoStato.getInstance().settrasmetti(true);
                                    InfoStato.getInstance().setInvioStop(true);
                                    InfoStato.getInstance().setChiudiGPRS(true);

                                    //new LogError("Send stop.");
                                }
                            }
                        }
                    }
                    //if(outTextMqtt[ALR_IND].indexOf("ALIVE")>0){
                    if (outText.indexOf("ALARM") > 0) {
                        //System.out.println("ALIVE MESSAGE");
                        InfoStato.getInstance().setPreAlive(true);
                    } else {
                        InfoStato.getInstance().setPreAlive(false);
                        //System.out.println("NO ALIVE MESSAGE");
                    }
                } else {
                    //new LogError("from store.");

                    InfoStato.getInstance().settrasmetti(true);
                    InfoStato.getInstance().setChiudiGPRS(false);
                }

                        //new LogError("Transmission status: " + InfoStato.getInstance().gettrasmetti());
                //System.out.println("Transmission status: " + InfoStato.getInstance().gettrasmetti());
                if (InfoStato.getInstance().gettrasmetti() == true) {

                    InfoStato.getInstance().settrasmetti(false);

                    if (InfoStato.getInstance().getApriGPRS() == true) {
                        close = false;
                        InfoStato.getInstance().setTRKstate(true);
                        try {
                            SemAT.getInstance().getCoin(5);
                            InfoStato.getInstance().writeATCommand("at^smong\r");
                            InfoStato.getInstance().writeATCommand("at+cgatt=1\r");
                            SemAT.getInstance().putCoin();
                        } catch (Exception e) {
                        }

                                // Open GPRS Channel
                        // connect to MQTT broker
                        Settings settings = Settings.getInstance();

                        SemAT.getInstance().getCoin(5);

                        if (!MQTTHandler.getInstance().isConnected()) {
                            Date date = new Date();
                            MQTTHandler.getInstance().init(
                                    settings.getSetting("clientID", InfoStato.getInstance().getIMEI()),
                                    InfoStato.getInstance().getInfoFileString(DestHost) + ":" + InfoStato.getInstance().getInfoFileString(DestPort),
                                    settings.getSetting("user", null),
                                    settings.getSetting("password", null),
                                    settings.getSetting("willTopic",
                                            settings.getSetting("publish", "owntracks/gw/")
                                            + settings.getSetting("clientID", InfoStato.getInstance().getIMEI())),
                                    settings.getSetting("will", "{\"_type\":\"lwt\",\"tst\":\""
                                            + date.getTime() / 1000 + "\"}").getBytes(),
                                    settings.getSetting("willQos", 1),
                                    settings.getSetting("willRetain", false),
                                    settings.getSetting("keepAlive", 60),
                                    settings.getSetting("cleanSession", true),
                                    settings.getSetting("subscription",
                                            settings.getSetting("publish", "owntracks/gw/")
                                            + settings.getSetting("clientID", InfoStato.getInstance().getIMEI()) + "/cmd"),
                                    settings.getSetting("subscriptionQos", 1)
                            );
                        }
                        try {
                            MQTTHandler.getInstance().connectToBroker();
                        } catch (MqttSecurityException mse) {
                            mse.printStackTrace();
                        } catch (MqttException me) {
                            me.printStackTrace();
                        }
                        SemAT.getInstance().putCoin();

                        InfoStato.getInstance().setApriGPRS(false);
                    }
                    //System.out.println("TT*SocketGPRSTask: INVIO DATO:");

                    Settings settings = Settings.getInstance();

                    if (settings.getSetting("raw", true)) {
                        SemAT.getInstance().getCoin(5);
                        try {
                            MQTTHandler.getInstance().publish(settings.getSetting("publish", "owntracks/gw/")
                                    + settings.getSetting("clientID", InfoStato.getInstance().getIMEI())
                                    + "/raw",
                                    settings.getSetting("qos", 1),
                                    settings.getSetting("retain", true),
                                    outText.getBytes());
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        SemAT.getInstance().putCoin();
                    }

                    LocationManager locationManager = LocationManager.getInstance();
                    locationManager.setMinDistance(settings.getSetting("minDistance", 500));
                    locationManager.setMaxInterval(settings.getSetting("maxInterval", 180));

                    if (locationManager.handleNMEAString(outText)) {
                        String[] fields = StringSplitter.split(
                                settings.getSetting("fields", "course,speed,altitude,distance,battery"), ",");
                        String json = locationManager.getJSONString(fields);
                        if (json != null) {
                            SemAT.getInstance().getCoin(5);
                            try {
                                MQTTHandler.getInstance().publish(settings.getSetting("publish", "owntracks/gw/")
                                        + settings.getSetting("clientID", InfoStato.getInstance().getIMEI()),
                                        settings.getSetting("qos", 1),
                                        settings.getSetting("retain", true),
                                        json.getBytes("UTF-8"));
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                            SemAT.getInstance().putCoin();
                        }
                    }

                    if (InfoStato.getInstance().getChiudiGPRS() == true) {

                        InfoStato.getInstance().setTRKstate(false);
                        System.out.println("CLOSE GPRS");
                        SemAT.getInstance().getCoin(5);
                        MQTTHandler.getInstance().disconnect();
                        SemAT.getInstance().putCoin();
                        InfoStato.getInstance().setChiudiGPRS(false);
                    }

                }
                // If BearerListener different from BEARER_STATE_UP, I do not have network coverage
                if (debug) {
                    System.out.println("BEARER: " + InfoStato.getInstance().getGprsState());
                }
                if (!InfoStato.getInstance().getGprsState()) {
                    errorSent = true;
                    //System.out.println("BEARER ERROR");
                    new LogError("BEARER ERROR");
                }

                if (ram) {
                    if (!errorSent) {
                        InfoStato.getInstance().setDataRAM("");
                                //System.out.println("DELETE DATA FROM RAM");
                        //InfoStato.getInstance().setDataMqttRAM(null);
                    }
                } else {
                    if (!errorSent) {
                        temp++;
                        if (temp >= codaSize || temp < 0) {
                            temp = 0;
                        }
                        InfoStato.getInstance().setInfoFileInt(TrkOUT, "" + temp);
                        FlashFile.getInstance().setImpostazione(TrkOUT, "" + temp);
                        InfoStato.getFile();
                        FlashFile.getInstance().writeSettings();
                        InfoStato.freeFile();
                    }
                    errorSent = false;
                }
                InfoStato.freeCoda();

                InfoStato.getInstance().setIfsocketAttivo(false);
                Thread.sleep(100);
                if (errorSent) {
                    close = true;
                    SemAT.getInstance().putCoin();	// release AT interface
                    InfoStato.getInstance().setIfsocketAttivo(false);
                    InfoStato.getInstance().setApriGPRS(false);
                    InfoStato.getInstance().setChiudiGPRS(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                close = true;
                String msgExcept = e.getMessage();
                //System.out.println("TT*SocketGPRStask, exception: "+msgExcept);

                InfoStato.getInstance().setIfsocketAttivo(false);

                InfoStato.getInstance().setApriGPRS(false);
                InfoStato.getInstance().setChiudiGPRS(false);

            } //catch

        } catch (Exception e) {
            e.printStackTrace();
            close = true;
            //new LogError("SocketGPRSTask generic Exception");
            InfoStato.getInstance().setIfsocketAttivo(false);

            InfoStato.getInstance().setApriGPRS(false);
            InfoStato.getInstance().setChiudiGPRS(false);
        }
        if (debug) {
            System.out.println("TT*SocketGPRStask, close: " + close);
        }
        if (close) {

            try {
                SemAT.getInstance().getCoin(5);
                InfoStato.getInstance().writeATCommand("at^smong\r");
                SemAT.getInstance().putCoin();
            } catch (Exception e) {
            }

            try {

                SemAT.getInstance().getCoin(5);
                MQTTHandler.getInstance().disconnect();
                SemAT.getInstance().putCoin();

                InfoStato.getInstance().setTRKstate(false);
                InfoStato.getInstance().setEnableCSD(true);

                SemAT.getInstance().getCoin(5);
                        // Close GPRS channel
                //System.out.println("SocketGPRSTask: KILL GPRS");
                InfoStato.getInstance().writeATCommand("at+cgatt=0\r");
                SemAT.getInstance().putCoin();

                Thread.sleep(5000);
            } catch (InterruptedException e) {
            } catch (Exception e) {
            }

            for (timeOutSocket = 0; timeOutSocket < 100; timeOutSocket++) {
                if (InfoStato.getInstance().isCloseTCPSocketTask()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
            InfoStato.getInstance().setApriGPRS(true);
        }
    }
} //SocketGPRStask

