/*	
 * Class 	ModemManager
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import java.io.*;
import com.cinterion.io.*;

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
public class ModemManager extends Thread implements GlobCost, ATCommandListener, ATCommandResponseListener {

    /* 
     * local variables
     */
    private ATCommand ATCMD;
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

    int countReg;

    /* 
     * constructors
     */
    public ModemManager() {
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

                // init object ATCommand and pass listener
                ATCMD = new ATCommand(true);
                ATCMD.addListener(this);

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
                        //System.out.println("Th*ModemManager: ATexec = " + InfoStato.getInstance().getATexec());
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
                        //System.out.println("Th*ModemManager: ATexec = " + InfoStato.getInstance().getATexec());

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
                                //        System.out.println("Th*ModemManager: no authentication required because PWD is null");
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
                                ATCMD.send(comandoAT, this);
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

    public void ATEvent(String event) {

        if (Settings.getInstance().getSetting("gsmDebug", false)) {
            System.out.println("ATListenerEvents: " + event);
        }

        /* 
         * Operations on ^SYSSTART
         */
        if (event.indexOf("^SYSSTART") >= 0) {

        } //^SYSSTART

        if (event.indexOf("^SYSSTART AIRPLANE MODE") >= 0) {
            // AIRPLANE MODE to activate radio parts of the moduleo
            InfoStato.getInstance().setOpMode("AIRPLANE");
            InfoStato.getInstance().setTipoRisveglio(risveglioCala);
            // AIRPLANE MODE leaves out normal software procedure and implies module reboot
            InfoStato.getInstance().setCALA(true);
        } //^SYSSTART AIRPLANE MODE

        /*
         * +CALA management
         */
        if (event.indexOf("+CALA") >= 0) {
            //System.out.println("ATListenerEvents: +CALA event ignored");
            Mailboxes.getInstance(0).write(msgALIVE);
        } //+CALA

        if (event.indexOf("+CGREG") >= 0) {
            InfoStato.getInstance().setCGREG(event.substring((event.indexOf(": ")) + 2, (event.indexOf(": ")) + 3));
        }

        if (event.indexOf("+CREG") >= 0) {
            InfoStato.getInstance().setCREG(event.substring((event.indexOf(": ")) + 2, (event.indexOf(": ")) + 3));
        }

        /*
         * Analyze answer for polling on key GPIO
         */
        if (event.indexOf("^SCPOL: 6") >= 0) {
            int SCPOLvalue = Integer.parseInt(event.substring(event.indexOf(",") + 1, event.indexOf(",") + 2));
            InfoStato.getInstance().setGPIOchiave(SCPOLvalue);
            InfoStato.getInstance().setDigitalIN(SCPOLvalue, 0);
        } //GPIO7

        /*
         * Analyze answer for polling on GPIO digital inputs
         */
        // digital input 1 (GPIO1)
        if (event.indexOf("^SCPOL: 0") >= 0) {
            int SCPOLvalue = Integer.parseInt(event.substring(event.indexOf(",") + 1, event.indexOf(",") + 2));
            InfoStato.getInstance().setDigitalIN(SCPOLvalue, 1);
            //Mailboxes.getInstance(0).write(msgALR1);
        } //GPIO1

        // digital input 2 (GPIO3)
        if (event.indexOf("^SCPOL: 2") >= 0) {
            int SCPOLvalue = Integer.parseInt(event.substring(event.indexOf(",") + 1, event.indexOf(",") + 2));
            InfoStato.getInstance().setDigitalIN(SCPOLvalue, 2);
            //Mailboxes.getInstance(0).write(msgALR2);
        } //GPIO3

        /* 
         * RING operations
         */
        if (event.indexOf("RING") >= 0) {
            //System.out.println("ATListenerEvents: incoming call waiting for answer...");
            // send msg to Mailboxes.getInstance(0)
            //InfoStato.getInstance().setCSDattivo(true);
            if (event.indexOf("REL ASYNC") > 0) {
                Mailboxes.getInstance(0).write(msgRING);
            }

        } //RING

        /*
         * Undervoltage // Pay attention: can be a source of problems if there is an initial Undervoltage ?
         */
        if (event.indexOf("^SBC: Undervoltage") >= 0) {
            // send msg to AppMain about low battery
            Mailboxes.getInstance(0).write(msgBattScarica);
        } //^SBC: Undervoltage

        /* 
         * +CMTI operations (new SMS received)
         */
        if (event.startsWith("+CMTI: ")) {
            InfoStato.getInstance().smsQ.put(event);
        }

        if (event.indexOf("^SCKS") >= 0) {
            //System.out.println(event);
            new LogError(event);
            if (event.indexOf("2") >= 0) {
                InfoStato.getInstance().setReboot();
                //InfoStato.getInstance().setInfoFileInt(UartNumTent, "1");
            }
        }

    } //ATEvent

    public void CONNChanged(boolean SignalState) {
    }

    public void RINGChanged(boolean SignalState) {
    }

    public void DCDChanged(boolean SignalState) {
    }

    public void DSRChanged(boolean SignalState) {
    }

    public void ATResponse(String response) {
        /* 
         * callback method for passing the response to a call
         * of the NON-blocking version of the ATCommand.send()
         */
        
        boolean leaveAT;
        
        if (Settings.getInstance().getSetting("gsmDebug", false)) {
            System.out.println("ATResponse: " + response);
        }
        leaveAT = false;
        /* Release XT65
         * 
         */
        if (response.indexOf("REVISION ") >= 0) {
            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
            response = response.substring(response.indexOf("REVISION ") + "REVISION ".length());
            InfoStato.getInstance().setREV(response.substring(0, response.indexOf("\r")));
        }

        /*
         * CSQ
         */
        if (response.indexOf("+CSQ") >= 0) {
            //System.out.println("ATListenerStd: AT+CSQ");
            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
            InfoStato.getInstance().setCSQ(response.substring(response.indexOf("+CSQ: ") + "+CSQ: ".length(), response.indexOf(",")));
        } //+CSQ

        if (response.indexOf("^SCFG: \"MEopMode/Airplane\",\"off\"") >= 0) {
            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
            //System.out.println("^SCFG: \"MEopMode/Airplane\",\"off\"");
            Settings.getInstance().setSetting("closeMode", closeAIR);
            //FlashFile.getInstance().setImpostazione(CloseMode, closeAIR);
            // Write to FlashFile.getInstance()
            //InfoStato.getFile();
            //FlashFile.getInstance().writeSettings();
            //InfoStato.freeFile();
            Mailboxes.getInstance(0).write(msgREBOOT);

        }

        /*			 
         * Answer to CSD call
         */
        if (response.indexOf("CONNECT 9600/RLP") >= 0) {
            //System.out.println("ATListenerStd: CSD connection established!");
            //InfoStato.getInstance().setATexec(false);
            InfoStato.getInstance().setCSDconnect(true);
        } //CONNECT

        /*
         * Answer to IMEI command
         */
        if (response.indexOf("AT+CGSN") >= 0) {
            //InfoStato.getInstance().setATexec(false);
            InfoStato.getInstance().setIMEI(response.substring(response.indexOf("+CGSN\r\r\n") + "+CGSN\r\r\n".length(), response.indexOf("OK") - 4));
        } //IMEI

        /*
         * Answer to read of GPIO key
         */
        if (response.indexOf("^SGIO") >= 0) {
            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
            int SGIOvalue = Integer.parseInt(response.substring(response.indexOf("^SGIO") + 7, response.indexOf("^SGIO") + 8));

            /*
             * GPIO key
             */
            // GPIO n.7
            if (InfoStato.getInstance().getGPIOnumberTEST() == 7) {

                // if SGIOvalue = "0" -> key active -> set value
                if (SGIOvalue == 0) {
                    InfoStato.getInstance().setGPIOchiave(0);
                    InfoStato.getInstance().setDigitalIN(0, 0);
                    InfoStato.getInstance().setTipoRisveglio(risveglioChiave);
                    //System.out.println("ATListenerStd: power up due to key activation!!");
                } // if SGIOvalue = "1" -> key not active -> no set value -> set '-1'
                else {
                    InfoStato.getInstance().setGPIOchiave(1);
                    InfoStato.getInstance().setDigitalIN(1, 0);
                }

            } //GPIO7

            /*
             * Digital input
             */
            // Input 1 = GPIO n.1
            if (InfoStato.getInstance().getGPIOnumberTEST() == 1) {
                InfoStato.getInstance().setDigitalIN(SGIOvalue, 1);
            } //Input 1
            // Input 2 = GPIO n.3
            else if (InfoStato.getInstance().getGPIOnumberTEST() == 3) {
                InfoStato.getInstance().setDigitalIN(SGIOvalue, 2);
            } //Input 2

        } //^SGIO

        /* 
         * Operation on ^SBV (battery control)
         */
        if (response.indexOf("^SBV") >= 0) {
            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
            //System.out.print("ATListenerStd: check battery voltage...");
            // extract info about battery voltage
            response = response.substring(response.indexOf("^SBV: "));
            //System.out.println("response:" + response);
            String Vbatt = response.substring("^SBV: ".length(), response.indexOf("\r\n"));
            if (Settings.getInstance().getSetting("gsmDebug", false)) {
                System.out.println("Vbatt:" + Vbatt);
            }
            //new LogError("Vbatt:" + Vbatt);
            double supplyVoltage = Double.parseDouble(Vbatt);
            //System.out.println("SupplyVoltage:"+supplyVoltage+"mV");
            // check battery voltage
            if (supplyVoltage <= VbattSoglia) {
                // send msg to AppMain about battery undervoltage
                Mailboxes.getInstance(0).write(msgBattScarica);
                if (Settings.getInstance().getSetting("gsmDebug", false)) {
                    System.out.println("^SBC: UnderVoltage: " + Vbatt);
                }
            }
            // insert battery info into FlashFile.getInstance()
            Vbatt = Vbatt.substring(0, 1) + "." + Vbatt.substring(1, 2) + "V";
            InfoStato.getInstance().setBatteryVoltage(Vbatt);
            //System.out.println("ATListenerStd, Battery Voltage: " + Vbatt);
        } //^SBV

        /* 
         * Operations on +CPMS (SMS memory status)
         */
        if (response.indexOf("+CPMS: \"MT\"") >= 0) {
            InfoStato.getInstance().smsQ.put(response);
        }

        /* 
         * Operations on +CMGL (SMS list)
         */
        if (response.indexOf("+CMGL") >= 0) {

            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
            try {
                // Extract strng '+CMGL.....'
                String temp = response.substring(response.indexOf("+CMGL: "));
                //System.out.println(temp);
                // Extract string '**,*'
                temp = temp.substring(temp.indexOf("+CMGL: ") + "+CMGL: ".length(), temp.indexOf(","));
                //System.out.println(temp);
                InfoStato.getInstance().setCodSMS(Integer.parseInt(temp));
            } catch (StringIndexOutOfBoundsException ex) {
                if (Settings.getInstance().getSetting("gsmDebug", false)) {
                    System.out.println("ATListenerStd, +CMGL: StringIndexOutOfBoundsException");
                }
                InfoStato.getInstance().setCodSMS(-2);
            } //catch

            //System.out.println("ATListenerStd, SMS code: " + InfoStato.getInstance().getCodSMS());
        } //+CMGL

        /* 
         * Operations on +CMGR
         */
        if (response.indexOf("+CMGR") >= 0) {
            InfoStato.getInstance().smsQ.put(response);
        }
            /*

        /*
         * Send SMS
         */
        if (response.indexOf(">") >= 0) {
            leaveAT = true;
        } //>

        if (response.indexOf("+CMGS") >= 0) {
            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
            //System.out.println(response);
        }

        /* 
         * Operations on +CCLK
         */
        if (response.indexOf("+CCLK") >= 0) {
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			/* 
             * Extract strings oraGPRMC and dataGPRMC
             */
            //System.out.println("ATListenerStd, +CCLK:: received answer is " + response);
            String dataGPRMC = response.substring(response.indexOf("\"") + 1, response.indexOf(","));
            response = response.substring(response.indexOf(","));
            String oraGPRMC = response.substring(response.indexOf(",") + 1, response.indexOf("\""));
            InfoStato.getInstance().setDataOraGPRMC(dataGPRMC, oraGPRMC);

        } //+CCLK

        /* 
         * Operations on +COPS (SIM network registration)
         */
        if (response.indexOf("^SMONG") >= 0 || response.indexOf("^smong") >= 0) {
            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
            //System.out.println(response);
            //new LogError(response);
        } //^SMONG

        /* 
         * Operations on +COPS (SIM network registration)
         */
        if (response.indexOf("+COPS:") >= 0) {
            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
            if (response.indexOf(",") >= 0) {
                countReg = 0;
            } else {
                countReg++;
            }
            if (countReg > 10) {
                new LogError("NO NETWORK");
                //System.out.println("NO NETWORK");
                InfoStato.getInstance().setReboot();
            }
        } //+COPS

        /* 
         * I wait for AT command answer before free AT resource for a new operation
         */
        // Execution OK
        if (response.indexOf("OK") >= 0) {
            //System.out.println("ATListenerStd, AT command result 'OK'");
            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
        } //OK

        // Execution ERROR
        if (response.indexOf("ERROR") >= 0) {
            if (Settings.getInstance().getSetting("gsmDebug", false)) {
                System.out.println("ATListenerStd, AT command result 'ERROR'");
            }
            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
        } //ERROR
        // Execution NO CARRIER
        if (response.indexOf("NO CARRIER") >= 0) {

            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
        } //NO CARRIER

        // Execution BUSY
        if (response.indexOf("BUSY") >= 0) {

            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
        } //BUSY

        // Execution NO DIALTONE
        if (response.indexOf("NO DIALTONE") >= 0) {

            //InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
        } //NO DIALTONE

        //if(!leaveAT)
        InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
        //System.out.println("EXIT LISTENER");

    } //ATResponse	

}
