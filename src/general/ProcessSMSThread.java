/*	
 * Class 	ProcessSMSThread
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

/**
 * Thread dedicated to SMS reception during application execution.
 *
 * @version	1.04 <BR> <i>Last update</i>: 14-12-2007
 * @author alessioza
 *
 */
public class ProcessSMSThread extends Thread {

    public static void setup() {
        ATManager.getInstance().executeCommandSynchron("AT+CMGF=1\r");
        ATManager.getInstance().executeCommandSynchron("AT+CPMS=\"MT\",\"MT\",\"MT\"\r");
        ATManager.getInstance().executeCommandSynchron("AT+CNMI=1,1\r");
        ProcessSMSThread processSMSThread = new ProcessSMSThread();
        processSMSThread.start();
    }

    public ProcessSMSThread() {
        SLog.log(SLog.Debug, "ProcessSMSThread", "created");
    }

    public void run() {
        String cmgs = ATManager.getInstance().executeCommandSynchron("AT+CPMS?\r");
        SLog.log(SLog.Debug, "ProcessSMSThread", "processCMGS " + cmgs);

        String[] lines = StringSplitter.split(cmgs, "\r\n");
        SLog.log(SLog.Debug, "ProcessSMSThread", "lines.length: " + lines.length);

        if (lines.length < 2) {
            return;
        }

        /*
         * +CPMS memory status
         * example +CPMS: "MT",0,45,"MT",0,45,"MT",0,45
         * example +CPMS: "MT",1,45,"MT",1,45,"MT",1,45
         */
        if (lines[1].startsWith("+CPMS: ")) {
            String[] values = StringSplitter.split(lines[1].substring(7), ",");
            SLog.log(SLog.Debug, "ProcessSMSThread", "values.length: " + values.length);

            if (values.length == 9) {
                int numberOfCurrentMessages;
                int numberOfStorageMessages;

                try {
                    numberOfCurrentMessages = Integer.parseInt(values[1]);
                } catch (NumberFormatException e) {
                    numberOfCurrentMessages = 0;
                }
                try {
                    numberOfStorageMessages = Integer.parseInt(values[2]);
                } catch (NumberFormatException e) {
                    numberOfStorageMessages = 0;
                }
                SLog.log(SLog.Debug, "ProcessSMSThread",
                        "numberOfCurrentMessages: " + numberOfCurrentMessages
                        + " numberOfStorageMessages: " + numberOfStorageMessages);

                if (numberOfCurrentMessages > 0) {
                    for (int i = 1; i <= numberOfStorageMessages; i++) {
                        String cmgr = ATManager.getInstance().executeCommandSynchron("AT+CMGR=" + i + "\r");
                        processCMGR(cmgr);
                    }
                }
            }
        }
    }

    public static void eventSMSArrived(String cmti) {
        SLog.log(SLog.Debug, "ProcessSMSThread", "processCMTI " + cmti);

        String[] lines = StringSplitter.split(cmti, "\r\n");
        SLog.log(SLog.Debug, "ProcessSMSThread", "lines.length: " + lines.length);

        if (lines.length < 2) {
            return;
        }

        /*
         * CMTI event new messages arrived
         * example: +CMTI: "MT",1
         */
        if (lines[1].startsWith("+CMTI: ")) {
            String[] values = StringSplitter.split(lines[1].substring(7), ",");
            if (values.length == 2) {
                int numberOfNewMessages;

                try {
                    numberOfNewMessages = Integer.parseInt(values[1]);
                } catch (NumberFormatException e) {
                    numberOfNewMessages = 0;
                }

                SLog.log(SLog.Debug, "ProcessSMSThread", "" + numberOfNewMessages + " new messages arrived");
                ProcessSMSThread processSMSThread = new ProcessSMSThread();
                processSMSThread.start();
            }
        }
    }

    void processCMGR(String cmgr) {
        SLog.log(SLog.Debug, "ProcessSMSThread", "processCMGR " + cmgr);

        String[] lines = StringSplitter.split(cmgr, "\r\n");
        SLog.log(SLog.Debug, "ProcessSMSThread", "lines.length: " + lines.length);

        if (lines.length < 2) {
            return;
        }

        /*
         * AT+CMGR message read
         * example:  +CMGR: "REC UNREAD","+4915118744526",,"14/08/02,18:10:26+08"
         *            exec reboot
         *
         *           OK
         */
        if (lines[1].startsWith("+CMGR: ")) {
            String[] values = StringSplitter.split(lines[1].substring(7), ",");
            SLog.log(SLog.Debug, "ProcessSMSThread", "values.length: " + values.length);
            if (values.length > 3) {
                int index = 0;
                int pos = lines[0].indexOf('=');
                if (pos > 0 && lines[0].length() > pos + 1) {
                    index = Integer.parseInt(lines[0].substring(pos + 1, lines[0].length() - 1));
                }

                SLog.log(SLog.Debug, "ProcessSMSThread", "index " + index);

                String telephoneNo;
                telephoneNo = values[1];

                SLog.log(SLog.Debug, "ProcessSMSThread", "telephoneNo " + telephoneNo);

                String text;
                text = lines[2];
                for (int i = 3; i < lines.length - 2; i++) {
                    text = text.concat(lines[i]);
                }

                SLog.log(SLog.Debug, "ProcessSMSThread", "text >" + text + "<");

                ATManager.getInstance().executeCommandSynchron("AT+CMGD=" + index + "\r");

                CommandProcessor commandProcessor = CommandProcessor.getInstance();
                String response;
                if (commandProcessor.execute(text)) {
                    response = commandProcessor.message;
                } else {
                    response = "NACK: " + commandProcessor.message;
                }

                SLog.log(SLog.Debug, "ProcessSMSThread", "response (" + response.length() + "): " + response);

                if (response.length() > 0) {
                    // max length SMS 140/160 
                    if (response.length() > 140) {
                        response = response.substring(0, 140);
                    }
                    ATManager.getInstance().executeCommandSynchron("AT+CMGS=" + telephoneNo + "\r", response);
                }
            }
        }
    }
}
