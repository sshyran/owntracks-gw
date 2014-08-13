/*	
 * Class 	ATManager
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
public class ATManager implements GlobCost, ATCommandListener, ATCommandResponseListener {

    private ATCommand atCommand;
    private int countReg;
    
    public ATManager() {
        try {
                atCommand = new ATCommand(true);
                atCommand.addListener(this);
        } catch (ATCommandFailedException atcfe) {
            System.err.println("ATCommandFailedException new ATCommand");
        }
    }
    
        public static ATManager getInstance() {
        return ModemManagerHolder.INSTANCE;
    }

    private static class ModemManagerHolder {

        private static final ATManager INSTANCE = new ATManager();
    }



    public synchronized void executeCommand(String command) {
        if (Settings.getInstance().getSetting("atDebug", false)) {
            System.out.println("executeCommand " + command);
        }
        try {
            atCommand.send(command, this);
        } catch (ATCommandFailedException atcfe) {
            new LogError("ATCommandFailedException send " + command);
        }
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
        if (event.indexOf("^SCPOL: ") >= 0) {
            GPIOInputManager.getInstance().processSCPOL(event);
        }

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
            BatteryManager.getInstance().eventLowBattery();
        }

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

        if (response.indexOf("^SGIO") >= 0) {
            GPIOInputManager.getInstance().processSGIO(response);
        }
 
        /* 
         * Operation on ^SBV (battery control)
         * example: ^SBV: 4400 
         */
        final String SBV = "^SBV: ";
        if (response.indexOf(SBV) >= 0) {
            try {
                int start = response.indexOf(SBV) + SBV.length();
                int end = response.substring(start).indexOf("\r");
                double voltage = Double.parseDouble(response.substring(
                        start, start + end));
                BatteryManager.getInstance().setBatteryVoltage(voltage / 1000);
            } catch (NumberFormatException nfe) {
                System.err.println("NumberFormatException " + response);
            }
        }

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
