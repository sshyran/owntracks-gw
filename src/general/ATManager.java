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

    public String executeCommandSynchron(String command) {
        return execute(command, null);
    }
    
    public void executeCommand(String command) {
        String response = execute(command, this);
    }
    
    private synchronized String execute(String command, ATCommandResponseListener listener) {
        String response = "";
        
        if (Settings.getInstance().getSetting("gsmDebug", false)) {
            System.out.println("execute Command: " + command);
            System.out.flush();
        }
        try {
            if (listener == null) {
                response = atCommand.send(command);
            } else {
                atCommand.send(command, listener);
            }
        } catch (ATCommandFailedException atcfe) {
            new LogError("ATCommandFailedException send " + command);
        }
        
        return response;
    }
    
    public void ATEvent(String event) {
        if (event == null) return;
        
        if (Settings.getInstance().getSetting("gsmDebug", false)) {
            System.out.println("ATListenerEvents: " + event);
        }

        if (event.indexOf("^SYSSTART AIRPLANE MODE") >= 0) {
            AppMain.getInstance().airplaneMode = true;
        }

        if (event.indexOf("+CGREG") >= 0) {
            try {
                SocketGPRSThread.getInstance().cgreg = Integer.parseInt(
                        event.substring((event.indexOf(": ")) + 2,
                                (event.indexOf(": ")) + 3)
                );
            } catch (NumberFormatException nfe) {
                SocketGPRSThread.getInstance().cgreg = -1;
            }
        }

        if (event.indexOf("+CREG") >= 0) {
            try {
                SocketGPRSThread.getInstance().creg = Integer.parseInt(
                        event.substring((event.indexOf(": ")) + 2,
                        (event.indexOf(": ")) + 3));
            } catch (NumberFormatException nfe) {
                SocketGPRSThread.getInstance().creg = -1;
            }
        }

        if (event.indexOf("^SCPOL: ") >= 0) {
            GPIOInputManager.getInstance().processSCPOL(event);
        }

        /* 
         * RING operations
         */
        if (event.indexOf("RING") >= 0) {
            if (event.indexOf("REL ASYNC") > 0) {
                AppMain.getInstance().ringEvent(event);
            }

        } //RING

        /*
         * Undervoltage // Pay attention: can be a source of problems if there is an initial Undervoltage ?
         */
        if (event.indexOf("^SBC: Undervoltage") >= 0) {
            BatteryManager.getInstance().eventLowBattery();
        }

        if (event.indexOf("^SCKS") >= 0) {
            new LogError(event);
            if (event.indexOf("2") >= 0) {
                AppMain.getInstance().shouldReboot = true;
            }
        }
    }

    public void CONNChanged(boolean SignalState) {
    }

    public void RINGChanged(boolean SignalState) {
    }

    public void DCDChanged(boolean SignalState) {
    }

    public void DSRChanged(boolean SignalState) {
    }

    public void ATResponse(String response) {
        if (Settings.getInstance().getSetting("gsmDebug", false)) {
            System.out.println("commandResponse: " + response);
        }
                
        /*			 
         * Answer to CSD call
         */
        if (response.indexOf("CONNECT 9600/RLP") >= 0) {
            InfoStato.getInstance().setCSDconnect(true);
        } //CONNECT

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
                AppMain.getInstance().shouldReboot = true;
            }
        } //+COPS
    }	
}
