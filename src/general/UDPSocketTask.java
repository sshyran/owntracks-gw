/*	
 * Class 	UDPSocketTask
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import java.io.*;
import java.util.*;

import javax.microedition.io.*;

import com.cinterion.io.BearerControl;

/**
 * Task that executes send of strings through GPRS connection using UDP sockets.
 *
 * @version	1.00 <BR> <i>Last update</i>: 24-06-2009
 * @author matteobo
 *
 */
public class UDPSocketTask extends Thread implements GlobCost {
    /* 
     * local variables
     */

    private double ctrlSpeed = 0;
    private boolean close = false;
    private boolean exitTRKON = true;
    private int temp;
    private boolean ram = true;
    /**
     * Full string to send through GPRS
     */
    private String outText;
    // socket TCP
    UDPDatagramConnection udpConn;
    Datagram dgram;
    byte[] buff;
    private String destAddressUDP;
    private int val_insensibgps;
    private int countDownException = 0;
    private boolean errorSent = false;
    BCListenerCustom list;
    String answer = "";

    /* 
     * constructors
     */
    public UDPSocketTask() {
        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("TT*UDPSocketTask: CREATED");
        }
    }

    /*
     * methods
     */
    /**
     * Task execution code:
     * <BR> ---------- <BR>
     * Performed operations: <br>
     * <ul type="disc">
     * <li> Add alarm to position strings, if necessary;
     * <li> Send string;
     * <li> Check the number of sent strings.
     * </ul>
     */
    public void run() {

        list = new BCListenerCustom();
        BearerControl.addListener(list);

        while (!InfoStato.getInstance().isCloseUDPSocketTask()) {

            //if(false){
            if (Settings.getInstance().getSetting("tracking", false) &&
                    Settings.getInstance().getSetting("protocol", "TCP").equals("UDP") &&
                    ((InfoStato.getInstance().getTrkIN() != InfoStato.getInstance().getTrkOUT()) ||
                    !InfoStato.getInstance().getDataRAM().equals(""))) {

                exitTRKON = false;

                try {

					// Indicates if GPRS SOCKET is ACTIVE
                    //System.out.println("TT*UDPSocketTask: START");
                    InfoStato.getInstance().setIfsocketAttivo(true);
                    destAddressUDP = "datagram://"
                            + Settings.getInstance().getSetting("tcp://host", "localhost")
                            + ":" + Settings.getInstance().getSetting("port", 1883);

                    /*
                     * Once this task has been started, it is completely
                     * finished before proceeding to a re-use, even if the
                     * timeout expires (so there may be a FIX GPRS timeout
                     * expired!)
                     */
                    try {
                        try {
                            while (!InfoStato.getCoda()) {
                                Thread.sleep(1L);
                            }
                        } catch (InterruptedException e) {
                        }

                        if (InfoStato.getInstance().getTrkIN() == InfoStato.getInstance().getTrkOUT()) {
                            outText = InfoStato.getInstance().getDataRAM();
                            ram = true;
                        } else {
                            ram = false;
                            temp = InfoStato.getInstance().getTrkOUT();
                            System.out.println("TT*UDPSocketTask: pointer out - " + temp);
                            if ((temp >= codaSize) || (temp < 0)) {
                                temp = 0;
                            }
                            outText = InfoStato.getInstance().getRecord(temp);
                            new LogError("TT*UDPSocketTask: pointer out - " + temp + " " + outText);
                            System.out.println("TT*UDPSocketTask: data in queue: " + outText);
                        }

                        System.out.println("TT*UDPSocketTask: string to send through GPRS:\r\n" + this.outText);

                        ctrlSpeed = InfoStato.getInstance().getSpeedForTrk();
                        if (Settings.getInstance().getSetting("speedDebug", false)) {
                            ctrlSpeed = InfoStato.getInstance().getSpeedGree();
                            System.out.println("SPEED " + ctrlSpeed);
                        }
                            val_insensibgps = Settings.getInstance().getSetting("minSpeed", 0);

                        if (ram) {

							//System.out.println("ACTUAL SPEED: " + this.ctrlSpeed);
                            //System.out.println("outText.indexOf(ALARM) " + (this.outText.indexOf("ALARM") > 0));
                            //System.out.println("outText.indexOf(ALIVE) " + (this.outText.indexOf("ALIVE") > 0));
                            //System.out.println("SPEED LIMIT: " + this.val_insensibgps);
                            //System.out.println("PREVIOUS MESSAGE IS ALIVE: " + this.InfoStato.getInstance().getPreAlive());
                            //System.out.println("SPEED LIMIT: " + this.val_insensibgps);
                            //System.out.println("PREVIOUS SPEED: " + this.InfoStato.getInstance().getPreSpeedDFS());
                            if (this.ctrlSpeed > this.val_insensibgps) {
                                System.out.println("Speed check ok.");
                                InfoStato.getInstance().settrasmetti(true);
                                if (InfoStato.getInstance().getInvioStop()) {
                                    InfoStato.getInstance().setApriGPRS(true);
                                }
                                InfoStato.getInstance().setInvioStop(false);
                            } else {
                                if ((outText.indexOf("ALARM") > 0) || (outText.indexOf("ALIVE") > 0)) {
                                    System.out.println("Alarm");
                                    InfoStato.getInstance().settrasmetti(true);
                                    InfoStato.getInstance().setApriGPRS(true);
                                } else {

                                    if ((!InfoStato.getInstance().getPreAlive()) && (ctrlSpeed <= val_insensibgps) && (InfoStato.getInstance().getPreSpeedDFS() > val_insensibgps)) {

                                        System.out.println("Speed check less then insensitivity, previous speed is greater");
                                        InfoStato.getInstance().settrasmetti(true);
                                        if (InfoStato.getInstance().getInvioStop() == true) {
                                            InfoStato.getInstance().setApriGPRS(true);
                                        }
                                        InfoStato.getInstance().setInvioStop(false);

                                    } else {

                                        System.out.println("Speed check failed.");
                                        if (InfoStato.getInstance().getInvioStop() == false) {
                                            System.out.println("Send stop coordinate.");
                                            InfoStato.getInstance().settrasmetti(true);
                                            InfoStato.getInstance().setInvioStop(true);
                                            InfoStato.getInstance().setChiudiGPRS(true);

                                            //new LogError("Send stop.");
                                        }
                                    }
                                }
                            }
                            if (this.outText.indexOf("ALIVE") > 0) {
                                System.out.println("ALIVE MESSAGE");
                                InfoStato.getInstance().setPreAlive(true);
                            } else {
                                InfoStato.getInstance().setPreAlive(false);
                                System.out.println("NO ALIVE MESSAGE");
                            }
                        } else {
							//new LogError("From store.");

                            InfoStato.getInstance().settrasmetti(true);

                            InfoStato.getInstance().setChiudiGPRS(false);

                        }

						//new LogError("Transmission status: " + InfoStato.getInstance().gettrasmetti());
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
                                try {
                                    udpConn = (UDPDatagramConnection) Connector.open(destAddressUDP);
                                } catch (Exception e) {
                                    System.out.println("TT*UDPSocketTask: Connector.open");
                                }
                                InfoStato.getInstance().setApriGPRS(false);
                            }

                            try {
								//mem2 = r.freeMemory();
                                //System.out.println("Free memory after allocation: " + mem2);
                                if ((outText == null) || (outText.indexOf("null") >= 0)) {
                                    outText = Settings.getInstance().getSetting("header", "$")
                                            + "," + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI())
                                            + defaultGPS + ",<ERROR>*00";
                                    buff = outText.getBytes();
                                }
                                System.out.println("OPEN DATAGRAM");
                                System.out.println(outText);
                                dgram = udpConn.newDatagram(outText.length());
                                buff = new byte[outText.length()];
                                System.out.println("SEND DATAGRAM");
                                buff = outText.getBytes();
                                new LogError("outText = " + outText);
                                dgram.setData(buff, 0, buff.length);
                                udpConn.send(dgram);
                                /*
                                int gprsCount = 0;
                                answer = "";
                                String ack = InfoStato.getInstance().getInfoFileString(Ackn);
                                if (!InfoStato.getInstance().getInfoFileString(Ackn).equals("")) {
                                    while (true) {
                                        dgram.reset();
                                        dgram.setLength(InfoStato.getInstance().getInfoFileString(Ackn).length() + 1);
                                        udpConn.receive(dgram);
                                        byte[] data = dgram.getData();
                                        answer = new String(data);
                                        answer = answer.substring(0, ack.length());
                                        if (Settings.getInstance().getSetting("generalDebug", false) {
                                            System.out.println("ACK: " + answer);
                                        }
                                        if (answer.equals(ack)) {
                                            new LogError("ACK");
                                            if (debug) {
                                                System.out.println("ACK RECEIVED");
                                            }
                                            break;
                                        } else {
                                            if (debug) {
                                                System.out.println("WAITING ACK");
                                            }
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                            }
                                            gprsCount++;
                                        }
                                        if (gprsCount > 15) {
                                            new LogError("NACK");
                                            InfoStato.getInstance().setReboot();
                                            errorSent = true;
                                            break;
                                        }
                                    }
                                }
*/
                            } catch (Exception err) {
                                System.out.println("TT*UDPSocketTask: Exception err");
                                new LogError("TT*UDPSocketTask: Exception during out text" + err.getMessage());
                                InfoStato.getInstance().setReboot();
                                errorSent = true;
                                break;
                            }
                            //new LogError(outText);
                            if (Settings.getInstance().getSetting("generalDebug", false)) {
                                System.out.println(outText);
                            }

                            if (InfoStato.getInstance().getChiudiGPRS() == true) {

                                InfoStato.getInstance().setTRKstate(false);
                                try {
                                    System.out.println("TT*UDPSocketTask: close UDP");
                                    udpConn.close();
                                } catch (NullPointerException e) {
                                    InfoStato.getInstance().setChiudiGPRS(false);
                                }
                                InfoStato.getInstance().setChiudiGPRS(false);
                            }
                        }

                        System.out.println("BEARER: " + InfoStato.getInstance().getGprsState());
                        if (!InfoStato.getInstance().getGprsState()) {
                            errorSent = true;
                            System.out.println("BEARER ERROR");
                            new LogError("BEARER ERROR");
                        }

                        if (ram) {
                            if (!errorSent) {
                                InfoStato.getInstance().setDataRAM("");
                            }
                        } else {
                            if (!errorSent) {
                                temp++;
                                if (temp >= codaSize || temp < 0) {
                                    temp = 0;
                                }
                                InfoStato.getInstance().setTrkOUT(temp);
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
						 //r.gc(); // request garbage collection

						 //mem2 = r.freeMemory();
                        //System.out.println("Free memory after collecting" + " discarded Integers: " + mem2);
                    } catch (IOException e) {
                        close = true;
                        String msgExcept = e.getMessage();
                        System.out.println("TT*UDPSocketTask: exception: " + msgExcept);

                        //new LogError("SocketGPRStask IOException: " + e);
                        InfoStato.getInstance().setIfsocketAttivo(false);

                        InfoStato.getInstance().setApriGPRS(false);
                        InfoStato.getInstance().setChiudiGPRS(false);

                    } catch (EmptyStackException e) {
                        close = true;
                        //System.out.println("exception: " + e.getMessage());
                        e.printStackTrace();

                        //new LogError("SocketGPRStask EmptyStackException");
                        InfoStato.getInstance().setIfsocketAttivo(false);

                        InfoStato.getInstance().setApriGPRS(false);
                        InfoStato.getInstance().setChiudiGPRS(false);

                    } //catch

                } catch (Exception e) {
                    close = true;
                    //new LogError("SocketGPRSTask generic Exception");
                    InfoStato.getInstance().setIfsocketAttivo(false);

                    InfoStato.getInstance().setApriGPRS(false);
                    InfoStato.getInstance().setChiudiGPRS(false);
                }

                if (close) {

                    try {
                        SemAT.getInstance().getCoin(5);
                        InfoStato.getInstance().writeATCommand("at^smong\r");
                        SemAT.getInstance().putCoin();
                    } catch (Exception e) {
                    }

                    try {
                        //System.out.println("***************CLOSE******************");
                        try {
                            udpConn.close();
                        } catch (NullPointerException e) {

                        }
						//System.out.println("***************CLOSED******************");

                        InfoStato.getInstance().setTRKstate(false);
                        InfoStato.getInstance().setEnableCSD(true);

                        SemAT.getInstance().getCoin(5);
							// Close GPRS channel
                        //System.out.println("SocketGPRSTask: KILL GPRS");
                        InfoStato.getInstance().writeATCommand("at+cgatt=0\r");
                        SemAT.getInstance().putCoin();

                        Thread.sleep(5000);
                    } catch (InterruptedException e) {

                    } catch (IOException e) {

                    } catch (Exception e) {

                    }
                    System.out.println("WAIT - DISCONNECT GPRS");
                    for (countDownException = 0; countDownException < 100; countDownException++) {
                        if (InfoStato.getInstance().isCloseUDPSocketTask()) {
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                    InfoStato.getInstance().setApriGPRS(true);
                }
            } else {

                try {
                    if (!Settings.getInstance().getSetting("tracking", false)) {

                        InfoStato.getInstance().setTRKstate(false);
                        InfoStato.getInstance().setEnableCSD(true);
                        SemAT.getInstance().putCoin();	// release AT interface
                        SemAT.getInstance().getCoin(5);
			    // Close GPRS channel
                        //System.out.println("SocketGPRSTask: TRK OFF KILL GPRS");
                        InfoStato.getInstance().writeATCommand("at+cgatt=0\r");
                        SemAT.getInstance().putCoin();
                    }
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
        }// while
    } //run
} //UDPSocketTask
