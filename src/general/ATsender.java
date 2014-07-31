/*	
 * Class 	ATsender
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import java.io.*;

import javax.microedition.io.Connector;

import com.cinterion.io.*;
import com.cinterion.io.file.FileConnection;

/**
 * AT commands sending to module with mutual exclusion.
 * <BR>
 * Semaphore 'SemAT.getInstance()' regulate access to AT resource, AT resource
 * must be requested calling 'SemAT.getInstance().getCoin(prio)' and released
 * calling 'SemAT.getInstance().putCoin()' when finish to use.
 * <br>
 * When a thread can use AT resource, it must wait for complete execution of ANY
 * PREVIOUS AT COMMAND before send another to module. To do this, set
 * 'ATexec=true' before sending ANY AT COMMAND to 'Mailboxes.getInstance(2)' and
 * wait for command execution using this code:
 * <pre>
 * System.out.println("Wait for free AT resource...");
 * while(ATexec) {}	// until AT resource is busy
 * </pre>
 *
 * @version	1.02 <BR> <i>Last update</i>: 25-10-2007
 * @author alessioza
 *
 */
public class ATsender extends Thread implements GlobCost {

    /* 
     * local variables
     */
    private ATCommand ATCMD;
    protected ATListenerStd ATListSTD;
    protected ATListenerEvents ATListEV;
    private String comandoAT;
    /**
     * indication about release of AT resource
     */
    public boolean stopAT = false;
    // CSD
    OutputStream dataOut;
    InputStream dataIn;
    private String comCSD;
    private int rcv;
    private boolean auth = false;
    private boolean confirmPWD = false;
    private String newPWD, strCHpwd, info, transportType;
    private int infoInt;

    /* 
     * constructors
     */
    public ATsender() {
    }

    /*
     * methods
     */
    public void run() {
        if (Settings.getInstance().getSetting("gsmDebug", false)) {
            System.out.println("Th*ATsender: STARTED");
        }

        while (true) {
            try {
                /* 
                 * Init listeners and ATCommand object
                 */
                ATListSTD = new ATListenerStd();
                ATListEV = new ATListenerEvents();

                // init object ATCommand and pass listener
                ATCMD = new ATCommand(true);
                ATCMD.addListener(ATListEV);

                /* 
                 * AT resource not available,
                 * because SemAT has been initialized to 0.
                 * I free resource for use by other threds using putCoin()
                 */
                if (Settings.getInstance().getSetting("gsmDebug", false)) {
                    System.out.println("SemAT: initial value = " + SemAT.getInstance().getValue());
                }

                SemAT.getInstance().putCoin();	// now AT resource is available  				
				/* 
                 * Main loop for AT commands execution,
                 * all AT commands read from mailbox 'Mailboxes.getInstance(2)' are executed.
                 * DURING AT command execution, AT resource remains BUSY.
                 * Two cases:
                 * 		a) Read commands
                 * 		b) Write & Execution commands
                 */
                while (true) {
                    /* 
                     * Wait for new msg (with AT commands to execute) from mailbox
                     */
                    if (Mailboxes.getInstance(2).numMsg() > 0) {
                        /*
                         * If new msg present in the Mailbox, I check flag 'ATexec':
                         * 	if 'ATexec=true'  -->  command execution
                         * 	if 'ATexec=false' -->  wait
                         */
                        //System.out.println("Th*ATsender: ATexec = " + InfoStato.getInstance().getATexec());
                        if (InfoStato.getInstance().getATexec() == false) {
							// if 'ATexec=false' wait for resource
                            if (Settings.getInstance().getSetting("gsmDebug", false)) {
                                System.out.println("Th*ATsender: wait for 'ATexec=true'...");
                            }
                            
                            while (InfoStato.getInstance().getATexec() == false) {
                            if (Settings.getInstance().getSetting("gsmDebug", false)) {
                                System.out.println("Th*ATsender: sleeping ...");
                            }
                            
                                Thread.sleep(whileSleep);
                            }
                            //System.out.println("OK");
                        } //if flagS.getATexec()
                        //System.out.println("Th*ATsender: ATexec = " + InfoStato.getInstance().getATexec());

                        // Read msg
                        comandoAT = (String) Mailboxes.getInstance(2).read();

                        /*
                         * Operations on ATCommand for CSD protocol
                         */
                        // Open IN/OUT stream for CSD
                        if (comandoAT.indexOf(csdOpen) >= 0) {
                            dataOut = ATCMD.getDataOutputStream();
                            dataIn = ATCMD.getDataInputStream();
                            if (Settings.getInstance().getSetting("gsmDebug", false)) {
                                System.out.println("Th*ATsender: Stream CSD aperto");
                            }
                        } //csdOpen
                        // Write on CSD output channel
                        else if (comandoAT.indexOf(csdWrite) >= 0) {
                            try {
                                dataOut.write((comandoAT.substring(comandoAT.indexOf(csdWrite) + csdWrite.length())).getBytes());
                            } catch (IOException ioe) {
                                System.out.println("Th*ATsender: IOException");
                            }
                        } //csdWrite
                        // Read from CSD input channel
                        else if (comandoAT.indexOf(csdRead) >= 0) {

                            /*
                             * Start CSD read cycle
                             * (to do: stop application until CSD call is closed)
                             */
                            try {

                                // If CSD PWD is null, no authentication required
                                //if (InfoStato.getInstance().getInfoFileString(PasswordCSD).equalsIgnoreCase("")) {
                                //    auth = true;
                                //    if (Settings.getInstance().getSetting("generalDebug", false) {
                                //        System.out.println("Th*ATsender: no authentication required because PWD is null");
                                //    }
                                //}

                                while (true) {

                                    try {

                                        /*
                                         * Read command
                                         */
                                        rcv = 0;
                                        comCSD = "";
                                        do {
                                            rcv = dataIn.read();
                                            if (rcv != '\n') {
                                                if (Settings.getInstance().getSetting("generalDebug", false)) {
                                                    if (rcv >= 0) {
                                                        System.out.print((char) rcv);
                                                    }
                                                }
                                                // update string read from CSD
                                                if ((byte) rcv != '\r') {
                                                    dataOut.write((byte) rcv);
                                                    comCSD = comCSD + (char) rcv;
                                                } else {
                                                    dataOut.write("\r\n".getBytes());
                                                }
                                            }
                                        } while ((char) rcv != '\r');
                                        // If '\r' received, process command	
                                        if (Settings.getInstance().getSetting("generalDebug", false)) {
                                            System.out.println("Th*ATsender, CSD command received: " + comCSD + "***");
                                        }

                                        /*
                                         * Command processing
                                         */
						    			//** Messages accepted with or without authentication **//	
                                         if (comCSD.startsWith("$")) {
                                            CommandProcessor commandProcessor = CommandProcessor.getInstance();
                                            try {
                                                if (commandProcessor.execute(comCSD.substring(1), true)) {
                                                    dataOut.write(("ACK: " + commandProcessor.message + "\r\n").getBytes());
                                                } else {
                                                    dataOut.write(("NACK: " + commandProcessor.message + "\r\n").getBytes());
                                                }
                                            } catch (IOException ioe) {
                                                if (Settings.getInstance().getSetting("generalDebug", false)) {
                                                    System.out.println("Th*ATsender: IOException");
                                                }

                                            }
                                        }

                                        if (Settings.getInstance().getSetting("display", false)) {
                                            dataOut.write((InfoStato.getInstance().getRMCTrasp()).getBytes());
                                            dataOut.write((InfoStato.getInstance().getGGATrasp()).getBytes());
                                        }

                                    } catch (StringIndexOutOfBoundsException siobe) {
                                        if (Settings.getInstance().getSetting("generalDebug", false)) {
                                            System.out.println("Th*ATsender: CSD exception");
                                        }
                                        dataOut.write("Command ERROR\n\r".getBytes());
                                    }

                                } //while(true)

                            } catch (IOException ioe) {
                                if (Settings.getInstance().getSetting("generalDebug", false)) {
                                    System.out.println("Th*ATsender: IOException");
                                }
                            }

                            // At the end of stream use, set ATexec = false
                            InfoStato.getInstance().setATexec(false);

                            // Indicates that CSD connection isn't in use yet, to close UpdateSCD
                            InfoStato.getInstance().setCSDconnect(false);

                            // Authentication non più valida
                            auth = false;

                        } //csdRead			
                        /*
                         * WRITE OR EXECUTING AT COMMANDS
                         */ else {
                            /* 
                             * Waiting the end of AT command execution is demanded
                             * to 'ATListenerStd', that must be used also to process
                             * the response to an AT command						
                             */
                            try {
                                ATCMD.send(comandoAT, ATListSTD);
                            } catch (ATCommandFailedException ATex) {
                                if (Settings.getInstance().getSetting("generalDebug", false)) {
                                    System.out.println("Th*ATsender: ATCommandFailedException");
                                }
                                new LogError("Th*ATsender: ATCommandFailedException");
                            } catch (IllegalStateException e) {
                                if (Settings.getInstance().getSetting("generalDebug", false)) {
                                    System.out.println("Th*ATsender: IllegalStateException");
                                }
                                new LogError("Th*ATsender: IllegalStateException");
                            } catch (IllegalArgumentException e) {
                                if (Settings.getInstance().getSetting("generalDebug", false)) {
                                    System.out.println("Th*ATsender: IllegalArgumentException");
                                }
                                new LogError("Th*ATsender: IllegalArgumentException");
                            }
                            if (comandoAT.indexOf("ATA") >= 0) {
                                Thread.sleep(30000);
                            }

                        }

                        // Wait before repeat cycle
                        Thread.sleep(whileSleep);

                    } //if Mailboxes.getInstance(2).numMsg

                    // break condition
                    if (stopAT) {
                        break;
                    }

                } //while	

                /* 
                 * Release object ATCommand 
                 * could be done only by setting stopAT=false
                 */
                ATCMD.release();

            } catch (ATCommandFailedException ATex) {
                if (Settings.getInstance().getSetting("generalDebug", false)) {
                    System.out.println("Th*ATsender: ATCommandFailedException");
                }
                new LogError("Th*ATsender: ATCommandFailedException");
            } catch (InterruptedException ie) {
                if (Settings.getInstance().getSetting("generalDebug", false)) {
                    System.out.println("Th*ATsender: InterruptedException");
                }
                new LogError("Th*ATsender: InterruptedException");
            } catch (Exception e) {
                System.out.println("Th*ATsender: Exception2");
                e.printStackTrace();
            }//catch
            new LogError("Reboot ATsender");
        } //while

    } //run

    /**
     * public boolean checkComma(int comma, String text)
     *
     * Method that check how comma contain the text
     *
     * @param	comma: commas number to check
     * @param	text: text where search commas
     * @return	true if number is OK, false otherwise
     */
    public boolean checkComma(int comma, String text) {

        int count = 0;
        int s = 0;
        for (int i = 0; i < comma; i++) {
            try {
                if ((s = text.indexOf(",")) != -1) {
                    count++;
                    text = text.substring(s + 1);
                } else {
                    return false;
                }
            } catch (NullPointerException e) {
                return false;
            } catch (IndexOutOfBoundsException e) {
                return false;
            }

        }
        if (count == comma) {
            return true;
        }
        return false;
    }
}
