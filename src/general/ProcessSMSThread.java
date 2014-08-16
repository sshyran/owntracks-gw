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
public class ProcessSMSThread extends Thread implements GlobCost {
    final private int loopSleep = 5000;
    
    int numberOfCurrentMessages;
    int numberOfStorageMessages;
    int numberOfProcessedMessages;

    
    public ProcessSMSThread() {
        if (Settings.getInstance().getSetting("smsDebug", false)) {
            System.out.println("ProcessSMSThread created");
        }
    }

    public void run() {
        open();
                
        while (true) {
            processMessage(ATManager.getInstance().executeCommandSynchron("AT+CPMS?\r"));

            String message;
            do {
                message = (String)InfoStato.getInstance().smsQ.get();
                if (message != null) {
                    processMessage(message);
                }
            } while (message != null);
            
            try {
                Thread.sleep(loopSleep);
            } catch (InterruptedException e) {
            }
        }
    }
    
    void open() {
        ATManager.getInstance().executeCommandSynchron("AT+CMGF=1\r");
        processMessage(ATManager.getInstance().executeCommandSynchron("AT+CPMS=\"MT\",\"MT\",\"MT\"\r"));
    }

    void processMessage(String message) {
        if (Settings.getInstance().getSetting("smsDebug", false)) {
            System.out.println("Received from smsQ: " + message);
        }
        
        String[] lines = StringSplitter.split(message, "\r\n");
        if (Settings.getInstance().getSetting("smsDebug", false)) {
            System.out.println("lines.length: " + lines.length);
        }

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

                if (Settings.getInstance().getSetting("smsDebug", false)) {
                    System.out.println("" + numberOfNewMessages + " new messages arrived");
                }
            }
        }


        /*
        * +CPMS memory status
        * example +CPMS: "MT",0,45,"MT",0,45,"MT",0,45
        * example +CPMS: "MT",1,45,"MT",1,45,"MT",1,45
        */
        if (lines[1].startsWith("+CPMS: ")) {
            String[] values = StringSplitter.split(lines[1].substring(7), ",");
            if (Settings.getInstance().getSetting("smsDebug", false)) {
                System.out.println("values.length: " + values.length);
            }

            if (values.length == 9) {
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
                if (Settings.getInstance().getSetting("smsDebug", false)) {
                    System.out.println("numberOfCurrentMessages: " + numberOfCurrentMessages
                        + " numberOfStorageMessages: " + numberOfStorageMessages);
                }

                if (numberOfCurrentMessages > 0) {
                    for (int i = 1; i <= numberOfStorageMessages; i++) {
                        processMessage(ATManager.getInstance().executeCommandSynchron("AT+CMGR=" + i + "\r"));
                    }
                }
            }
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
            if (Settings.getInstance().getSetting("smsDebug", false)) {
                System.out.println("values.length: " + values.length);
            }
            if (values.length > 3) {
                int index = 0;
                int pos = lines[0].indexOf('=');
                if (pos > 0 && lines[0].length() > pos + 1) {
                    index = Integer.parseInt(lines[0].substring(pos + 1, lines[0].length() -1 ));
                }
                
                if (Settings.getInstance().getSetting("smsDebug", false)) {
                    System.out.println("index " + index);
                }
                
                String telephoneNo;
                telephoneNo = values[1];
                
                if (Settings.getInstance().getSetting("smsDebug", false)) {
                    System.out.println("telephoneNo " + telephoneNo);
                }
                
                String text;
                text = lines[2];
                for (int i = 3; i < lines.length - 2; i++) {
                    text = text.concat(lines[i]);
                }
                
                if (Settings.getInstance().getSetting("smsDebug", false)) {
                    System.out.println("text >" + text + "<");
                }               
                                
                ATManager.getInstance().executeCommandSynchron("AT+CMGD=" + index + "\r");

                CommandProcessor commandProcessor = CommandProcessor.getInstance();
                String response;
                if (commandProcessor.execute(text, false)) {
                    response = commandProcessor.message;               
                } else {
                    response = "NACK: " + commandProcessor.message;
                }
                
                if (Settings.getInstance().getSetting("smsDebug", false)) {
                    System.out.println("response (" + response.length() + "): " + response);
                }               

                // max length SMS 140/160 
                if (response.length() > 140) {
                    response = response.substring(0, 140);
                }

                ATManager.getInstance().executeCommandSynchron("AT+CMGS=" + telephoneNo + "\r" + response + "\032");
            }
        }                
    }   
}

