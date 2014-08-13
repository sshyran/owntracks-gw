/*	
 * Class 	ATManager
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import com.cinterion.io.*;

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
        if (Settings.getInstance().getSetting("gsmDebug", false)) {
            System.out.println("execute Command: " + command);
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

        if (event.indexOf("^SYSSTART") >= 0) {
        }

        if (event.indexOf("^SYSSTART AIRPLANE MODE") >= 0) {
            AppMain.getInstance().airplaneMode = true;
        }

        if (event.indexOf("+CALA") >= 0) {
        }

        if (event.indexOf("+CGREG") >= 0) {
            InfoStato.getInstance().setCGREG(event.substring((event.indexOf(": ")) + 2, (event.indexOf(": ")) + 3));
        }

        if (event.indexOf("+CREG") >= 0) {
            InfoStato.getInstance().setCREG(event.substring((event.indexOf(": ")) + 2, (event.indexOf(": ")) + 3));
        }

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
            System.out.println("commandResponse: " + response);
        }
        leaveAT = false;
        /* Release XT65
         * 
         */
        if (response.indexOf("REVISION ") >= 0) {
            response = response.substring(response.indexOf("REVISION ") + "REVISION ".length());
            InfoStato.getInstance().setREV(response.substring(0, response.indexOf("\r")));
        }

        if (response.indexOf("+CSQ") >= 0) {
        }

        if (response.indexOf("^SCFG: \"MEopMode/Airplane\",\"off\"") >= 0) {
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
            InfoStato.getInstance().setCSDconnect(true);
        } //CONNECT

        /*
         * Answer to IMEI command
         */
        if (response.indexOf("AT+CGSN") >= 0) {
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
        }

        if (response.indexOf("+CCLK") >= 0) {
        }
        /* 
         * Operations on +COPS (SIM network registration)
         */
        if (response.indexOf("^SMONG") >= 0 || response.indexOf("^smong") >= 0) {
        } //^SMONG

        /* 
         * Operations on +COPS (SIM network registration)
         */
        if (response.indexOf("+COPS:") >= 0) {
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
    }	
}
