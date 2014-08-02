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
    /**
     * Full string to send through GPRS
     */
    // socket TCP
    UDPDatagramConnection udpConn;
    Datagram dgram;
    byte[] buff;
    private String destAddressUDP;
    private int val_insensibgps;
    private int countDownException = 0;
    private boolean errorSent = false;
    String answer = "";

    private boolean openGPRS = false;

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
//#ifdef DONE
//#         while (!InfoStato.getInstance().isCloseUDPSocketTask()) {
//# 
//#             //if(false){
//#             if (Settings.getInstance().getSetting("tracking", false)
//#                     && Settings.getInstance().getSetting("protocol", "TCP").equals("UDP")
//#                     && ((InfoStato.getInstance().getTrkIN() != InfoStato.getInstance().getTrkOUT())
//#                     || !InfoStato.getInstance().getDataRAM().equals(""))) {
//# 
//#                 exitTRKON = false;
//# 
//#                 try {
//# 
//#                     // Indicates if GPRS SOCKET is ACTIVE
//#                     //System.out.println("TT*UDPSocketTask: START");
//#                     InfoStato.getInstance().setIfsocketAttivo(true);
//#                     destAddressUDP = "datagram://"
//#                             + Settings.getInstance().getSetting("tcp://host", "localhost")
//#                             + ":" + Settings.getInstance().getSetting("port", 1883);
//# 
//#                     /*
//#                      * Once this task has been started, it is completely
//#                      * finished before proceeding to a re-use, even if the
//#                      * timeout expires (so there may be a FIX GPRS timeout
//#                      * expired!)
//#                      */
//#                     try {
//# 
//#                         String message = (String) InfoStato.getInstance().gpsQ.get();
//#                         if (message != null) {
//#                             processMessage(message);
//#                         } else {
//#                             InfoStato.getInstance().settrasmetti(true);
//#                             InfoStato.getInstance().setChiudiGPRS(false);
//#                         }
//# 
//#                         InfoStato.getInstance().setIfsocketAttivo(false);
//#                         Thread.sleep(100);
//#                         if (errorSent) {
//#                             close = true;
//#                             SemAT.getInstance().putCoin();	// release AT interface
//#                             InfoStato.getInstance().setIfsocketAttivo(false);
//#                             openGPRS = false;
//#                             InfoStato.getInstance().setChiudiGPRS(false);
//#                         }
//#                     } catch (EmptyStackException e) {
//#                         close = true;
//#                         //System.out.println("exception: " + e.getMessage());
//#                         e.printStackTrace();
//# 
//#                         //new LogError("SocketGPRStask EmptyStackException");
//#                         InfoStato.getInstance().setIfsocketAttivo(false);
//# 
//#                         openGPRS = false;
//#                         InfoStato.getInstance().setChiudiGPRS(false);
//# 
//#                     } //catch
//# 
//#                 } catch (Exception e) {
//#                     close = true;
//#                     //new LogError("SocketGPRSTask generic Exception");
//#                     InfoStato.getInstance().setIfsocketAttivo(false);
//# 
//#                     openGPRS = false;
//#                     InfoStato.getInstance().setChiudiGPRS(false);
//#                 }
//# 
//#                 if (close) {
//# 
//#                     try {
//#                         SemAT.getInstance().getCoin(5);
//#                         InfoStato.getInstance().writeATCommand("at^smong\r");
//#                         SemAT.getInstance().putCoin();
//#                     } catch (Exception e) {
//#                     }
//# 
//#                     try {
//#                         //System.out.println("***************CLOSE******************");
//#                         try {
//#                             udpConn.close();
//#                         } catch (NullPointerException e) {
//# 
//#                         }
//#                         //System.out.println("***************CLOSED******************");
//# 
//#                         InfoStato.getInstance().setTRKstate(false);
//#                         InfoStato.getInstance().setEnableCSD(true);
//# 
//#                         SemAT.getInstance().getCoin(5);
//#                         // Close GPRS channel
//#                         //System.out.println("SocketGPRSTask: KILL GPRS");
//#                         InfoStato.getInstance().writeATCommand("at+cgatt=0\r");
//#                         SemAT.getInstance().putCoin();
//# 
//#                         Thread.sleep(5000);
//#                     } catch (InterruptedException e) {
//# 
//#                     } catch (IOException e) {
//# 
//#                     } catch (Exception e) {
//# 
//#                     }
//#                     System.out.println("WAIT - DISCONNECT GPRS");
//#                     for (countDownException = 0; countDownException < 100; countDownException++) {
//#                         if (InfoStato.getInstance().isCloseUDPSocketTask()) {
//#                             break;
//#                         }
//#                         try {
//#                             Thread.sleep(1000);
//#                         } catch (InterruptedException e) {
//#                         }
//#                     }
//#                     openGPRS = false;
//#                 }
//#             } else {
//# 
//#                 try {
//#                     if (!Settings.getInstance().getSetting("tracking", false)) {
//# 
//#                         InfoStato.getInstance().setTRKstate(false);
//#                         InfoStato.getInstance().setEnableCSD(true);
//#                         SemAT.getInstance().putCoin();	// release AT interface
//#                         SemAT.getInstance().getCoin(5);
//#                         // Close GPRS channel
//#                         //System.out.println("SocketGPRSTask: TRK OFF KILL GPRS");
//#                         InfoStato.getInstance().writeATCommand("at+cgatt=0\r");
//#                         SemAT.getInstance().putCoin();
//#                     }
//#                     Thread.sleep(2000);
//#                 } catch (InterruptedException e) {
//#                 }
//#             }
//#         }// while
//#endif
    } //run
//#ifdef DONE
//#     void processMessage(String message) {
//#         ctrlSpeed = InfoStato.getInstance().getSpeedForTrk();
//#         if (Settings.getInstance().getSetting("speedDebug", false)) {
//#             ctrlSpeed = InfoStato.getInstance().getSpeedGree();
//#             System.out.println("SPEED " + ctrlSpeed);
//#         }
//#         val_insensibgps = Settings.getInstance().getSetting("minSpeed", 0);
//# 
//#         if (this.ctrlSpeed > this.val_insensibgps) {
//#             System.out.println("Speed check ok.");
//#             InfoStato.getInstance().settrasmetti(true);
//#             if (InfoStato.getInstance().getInvioStop()) {
//#                 openGPRS = true;
//#             }
//#             InfoStato.getInstance().setInvioStop(false);
//#         } else {
//#             if ((message.indexOf("ALARM") > 0) || (message.indexOf("ALIVE") > 0)) {
//#                 System.out.println("Alarm");
//#                 InfoStato.getInstance().settrasmetti(true);
//#                 openGPRS = true;
//#             } else {
//# 
//#                 if ((!InfoStato.getInstance().getPreAlive()) && (ctrlSpeed <= val_insensibgps) && (InfoStato.getInstance().getPreSpeedDFS() > val_insensibgps)) {
//# 
//#                     System.out.println("Speed check less then insensitivity, previous speed is greater");
//#                     InfoStato.getInstance().settrasmetti(true);
//#                     if (InfoStato.getInstance().getInvioStop() == true) {
//#                         openGPRS = true;
//#                     }
//#                     InfoStato.getInstance().setInvioStop(false);
//# 
//#                 } else {
//# 
//#                     System.out.println("Speed check failed.");
//#                     if (InfoStato.getInstance().getInvioStop() == false) {
//#                         System.out.println("Send stop coordinate.");
//#                         InfoStato.getInstance().settrasmetti(true);
//#                         InfoStato.getInstance().setInvioStop(true);
//#                         InfoStato.getInstance().setChiudiGPRS(true);
//# 
//#                         //new LogError("Send stop.");
//#                     }
//#                 }
//#             }
//#         }
//#         if (message.indexOf("ALIVE") > 0) {
//#             System.out.println("ALIVE MESSAGE");
//#             InfoStato.getInstance().setPreAlive(true);
//#         } else {
//#             InfoStato.getInstance().setPreAlive(false);
//#             System.out.println("NO ALIVE MESSAGE");
//#         }
//# 
//#         //new LogError("Transmission status: " + InfoStato.getInstance().gettrasmetti());
//#         if (InfoStato.getInstance()
//#                 .gettrasmetti() == true) {
//# 
//#             InfoStato.getInstance().settrasmetti(false);
//# 
//#             if (openGPRS) {
//# 
//#                 close = false;
//#                 InfoStato.getInstance().setTRKstate(true);
//#                 try {
//#                     SemAT.getInstance().getCoin(5);
//#                     InfoStato.getInstance().writeATCommand("at^smong\r");
//#                     InfoStato.getInstance().writeATCommand("at+cgatt=1\r");
//#                     SemAT.getInstance().putCoin();
//#                 } catch (Exception e) {
//#                 }
//# 
//#                 // Open GPRS Channel
//#                 try {
//#                     udpConn = (UDPDatagramConnection) Connector.open(destAddressUDP);
//#                 } catch (Exception e) {
//#                     System.out.println("TT*UDPSocketTask: Connector.open");
//#                 }
//#                 openGPRS = false;
//#             }
//# 
//#             try {
//# 								//mem2 = r.freeMemory();
//#                 //System.out.println("Free memory after allocation: " + mem2);
//#                 if ((message  == null) || (message.indexOf("null") >= 0)) {
//#                     message = Settings.getInstance().getSetting("header", "$")
//#                             + "," + Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI())
//#                             + defaultGPS + ",<ERROR>*00";
//#                     buff = message.getBytes();
//#                 }
//#                 System.out.println("OPEN DATAGRAM");
//#                 System.out.println(message);
//#                 dgram = udpConn.newDatagram(message.length());
//#                 buff = new byte[message.length()];
//#                 System.out.println("SEND DATAGRAM");
//#                 buff = message.getBytes();
//#                 new LogError("message = " + message);
//#                 dgram.setData(buff, 0, buff.length);
//#                 udpConn.send(dgram);
//#             } catch (Exception err) {
//#                 System.out.println("TT*UDPSocketTask: Exception err");
//#                 new LogError("TT*UDPSocketTask: Exception during out text" + err.getMessage());
//#                 InfoStato.getInstance().setReboot();
//#                 errorSent = true;
//#             }
//#             //new LogError(outText);
//#             if (Settings.getInstance().getSetting("generalDebug", false)) {
//#                 System.out.println(message);
//#             }
//# 
//#             if (InfoStato.getInstance().getChiudiGPRS() == true) {
//# 
//#                 InfoStato.getInstance().setTRKstate(false);
//#                 try {
//#                     System.out.println("TT*UDPSocketTask: close UDP");
//#                     udpConn.close();
//#                 } catch (IOException e) {
//#                     InfoStato.getInstance().setChiudiGPRS(false);
//#                 }
//#                 InfoStato.getInstance().setChiudiGPRS(false);
//#             }
//#         }
//# 
//#         System.out.println(
//#                 "BEARER: " + InfoStato.getInstance().getGprsState());
//#         if (!InfoStato.getInstance()
//#                 .getGprsState()) {
//#             errorSent = true;
//#             System.out.println("BEARER ERROR");
//#             new LogError("BEARER ERROR");
//#         }
//# 
//#     }
//#endif
} //UDPSocketTask
