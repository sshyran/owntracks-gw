/*	
 * Class 	ATManager
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import com.cinterion.io.*;

public class ATManager implements ATCommandListener, ATCommandResponseListener {

    private ATCommand atCommand;

    public ATManager() {
        try {
            atCommand = new ATCommand(true);
            atCommand.addListener(this);
        } catch (ATCommandFailedException atcfe) {
            SLog.log(SLog.Critical, "ATManager", "ATCommandFailedException new ATCommand");
        }
    }

    public static ATManager getInstance() {
        return ModemManagerHolder.INSTANCE;
    }

    private static class ModemManagerHolder {

        private static final ATManager INSTANCE = new ATManager();
    }

    public String executeCommandSynchron(String command) {
        return execute(command, null, null);
    }

    public String executeCommandSynchron(String command, String text) {
        return execute(command, text, null);
    }

    public void executeCommand(String command) {
        String response = execute(command, null, this);
    }

    private synchronized String execute(String command, String text, ATCommandResponseListener listener) {
        String response = "";
        try {
            if (listener == null) {
                response = atCommand.send(command);
                SLog.log(SLog.Debug, "ATManager", "commandResponse: " + response);
                if (text != null) {
                    response = response + atCommand.send(text + "\032");
                }
            } else {
                atCommand.send(command, listener);
            }
        } catch (ATCommandFailedException atcfe) {
            SLog.log(SLog.Alert, "ATManager", "ATCommandFailedException send " + command);
        }
        if (response.indexOf("ERROR") >= 0) {
            SLog.log(SLog.Warning, "ATManager", response);
        }
        return response;
    }

    public void ATEvent(String event) {
        if (event == null) {
            return;
        }

        SLog.log(SLog.Debug, "ATManager", "ATListenerEvents: (" + event.length() + ") " + event);
        
        if (event.indexOf("^SYSSTART AIRPLANE MODE") >= 0) {
            AppMain.getInstance().airplaneMode = true;
        } else if (event.indexOf("+CALA:") >= 0) {
            AppMain.getInstance().alarm = true;
        } else if (event.indexOf("+CGREG") >= 0) {
            try {
                SocketGPRSThread.getInstance().cgreg = Integer.parseInt(
                        event.substring((event.indexOf(": ")) + 2,
                                (event.indexOf(": ")) + 3)
                );
            } catch (NumberFormatException nfe) {
                SocketGPRSThread.getInstance().cgreg = -1;
            }
        } else if (event.indexOf("+CREG") >= 0) {
            try {
                SocketGPRSThread.getInstance().creg = Integer.parseInt(
                        event.substring((event.indexOf(": ")) + 2,
                                (event.indexOf(": ")) + 3));
            } catch (NumberFormatException nfe) {
                SocketGPRSThread.getInstance().creg = -1;
            }
        } else if (event.indexOf("^SCPOL: ") >= 0) {
            GPIOInputManager.getInstance().eventGPIOValueChanged(event);
        } else if (event.indexOf("+CMTI: ") >= 0) {
            ProcessSMSThread.eventSMSArrived(event);
        } else if (event.indexOf("^SBC: Undervoltage") >= 0) {
            BatteryManager.getInstance().eventLowBattery();
        } else if (event.indexOf("^SCKS") >= 0) {
            if (event.indexOf("2") >= 0) {
                AppMain.getInstance().invalidSIM = true;
                SLog.log(SLog.Alert, "ATManager", event);
            }
        } else {
            SLog.log(SLog.Debug, "ATManager", "ATListenerEvents: nothing to do");
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
        SLog.log(SLog.Debug, "ATManager", "commandResponse: " + response);
        if (response.indexOf("ERROR") >= 0) {
            SLog.log(SLog.Error, "ATManager", response);
        }
    }
}
