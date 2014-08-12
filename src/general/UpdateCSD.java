/*	
 * Class 	Update CSD
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import java.util.Timer;

/**
 * Thread to control the update of configuration file settings through a CSD
 * call.
 *
 * @version	1.02 <BR> <i>Last update</i>: 25-10-2007
 * @author alessioza
 *
 */
public class UpdateCSD extends Thread implements GlobCost {

    /* 
     * local variables
     */
    private boolean closeSession = false;
    /**
     * <BR> Timer for WatchDog on CSD call.
     */
    Timer CSDTimer;
    /**
     * <BR> Task to execute WatchDog control on CSD call.
     */
    TimeoutTask CSDTimeoutTask;

    /* 
     * constructors
     */
    public UpdateCSD() {
        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("Th*UpdateCSD: CREATED");
        }
    }

    /*
     * methods
     */
    /**
     * Contains thread execution code.
     * <BR> ---------- <BR>
     * Performed operations: <br>
     * <ul type="disc">
     * <li> Disabling key;
     * <li> Open CSD channel;
     * <li> Pass control to ATsender for parameters configuration;
     * <li> Enable key, when CSD connection is closed.
     * </ul>
     */
    public void run() {

        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("Th*UpdateCSD: STARTED");
        }

        InfoStato.getInstance().setCSDWatchDog(true);
        closeSession = false;

        // Create and start 'CSDTimeout' timer
        CSDTimer = new Timer();
        CSDTimeoutTask = new TimeoutTask(CSDtimeout);
        CSDTimer.schedule(CSDTimeoutTask, CSDTOvalue * 1000);

        try {

            InfoStato.getInstance().setCSDattivo(true);

            if (InfoStato.getInstance().getCSDWatchDog() == false) { // If timeout expired -> exit
                closeSession = true;
            }

            if (InfoStato.getInstance().getCSDWatchDog() == true && closeSession == false) {

                // Timer and timeout reset and new start
                CSDTimer.cancel();
                CSDTimeoutTask.cancel();
                CSDTimer = new Timer();
                CSDTimeoutTask = new TimeoutTask(CSDtimeout);
                CSDTimer.schedule(CSDTimeoutTask, CSDTOvalue * 1000);

                /*
                 * Answer to CSD call
                 */
                if (Settings.getInstance().getSetting("generalDebug", false)) {
                    System.out.println("Th*UpdateCSD: I'm responding to the CSD call");
                }
                InfoStato.getInstance().setATexec(true);
                Mailboxes.getInstance(2).write("ATA\r");

                // Wait until CSD connection is established
                while (InfoStato.getInstance().getCSDconnect() == false) {
                    if (InfoStato.getInstance().getCSDWatchDog() == false) { // if timeout expired -> exit
                        closeSession = true;
                        break;
                    }
                    Thread.sleep(whileSleep);
                } //while
            } //if

            if (InfoStato.getInstance().getCSDWatchDog() == true && closeSession == false) {

                // Timer and timeout reset and new start
                CSDTimer.cancel();
                CSDTimeoutTask.cancel();

                // Until only streams are used, set ATexec = true
                InfoStato.getInstance().setATexec(true);

                // With CSD connection, start input and output streams
                if (Settings.getInstance().getSetting("generalDebug", false)) {
                    System.out.println("Th*UpdateCSD: Open CSD stream");
                }
                Mailboxes.getInstance(2).write(csdOpen);

                // Write on output stream
                if (Settings.getInstance().getSetting("generalDebug", false)) {
                    System.out.println("Th*UpdateCSD: Write on output stream");
                }
                Mailboxes.getInstance(2).write(csdWrite + "\n\rGreenwich Connected\n\r");

                // Read from input stream
                if (Settings.getInstance().getSetting("generalDebug", false)) {
                    System.out.println("Th*UpdateCSD: Read from input stream");
                }
                Mailboxes.getInstance(2).write(csdRead);

		    	// ... authentication and commands ...
                // Wait until CSD connection is used
                while (InfoStato.getInstance().getCSDconnect() == true) {
                    Thread.sleep(whileSleep);
                }

            } //if

            if (InfoStato.getInstance().getCSDWatchDog() == true && closeSession == false) {

                // Restart timer
                CSDTimer = new Timer();
                CSDTimeoutTask = new TimeoutTask(CSDtimeout);
                CSDTimer.schedule(CSDTimeoutTask, CSDTOvalue * 1000);

                // Close CSD call
                if (Settings.getInstance().getSetting("generalDebug", false)) {
                    System.out.println("Th*UpdateCSD: I'm releasing CSD call");
                }
                ATManager.getInstance().executeCommand("ATH\r");
                while (InfoStato.getInstance().getCSDWatchDog()) {
                    Thread.sleep(whileSleep);
                }	

            } //if

            // Delete timer and task
            CSDTimer.cancel();
            CSDTimeoutTask.cancel();

        } catch (InterruptedException ie) {
            //System.out.println("UpdateCSD: InterruptedException");
        } //catch

        // Enable KEY
        InfoStato.getInstance().setCSDattivo(false);

        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("Th*UpdateCSD: END");
        }
    }
//#ifdef TODO
//#                             /*
//#                          * Operations on ATCommand for CSD protocol
//#                          */
//#                         // Open IN/OUT stream for CSD
//#                         if (comandoAT.indexOf(csdOpen) >= 0) {
//#                             dataOut = ATCMD.getDataOutputStream();
//#                             dataIn = ATCMD.getDataInputStream();
//#                             if (Settings.getInstance().getSetting("gsmDebug", false)) {
//#                                 System.out.println("Th*ATsender: Stream CSD aperto");
//#                             }
//#                         } //csdOpen
//#                         // Write on CSD output channel
//#                         else if (comandoAT.indexOf(csdWrite) >= 0) {
//#                             try {
//#                                 dataOut.write((comandoAT.substring(comandoAT.indexOf(csdWrite) + csdWrite.length())).getBytes());
//#                             } catch (IOException ioe) {
//#                                 System.out.println("Th*ATsender: IOException");
//#                             }
//#                         } //csdWrite
//#                         // Read from CSD input channel
//#                         else if (comandoAT.indexOf(csdRead) >= 0) {
//# 
//#                             /*
//#                              * Start CSD read cycle
//#                              * (to do: stop application until CSD call is closed)
//#                              */
//#                             try {
//# 
//#                                 // If CSD PWD is null, no authentication required
//#                                 //if (InfoStato.getInstance().getInfoFileString(PasswordCSD).equalsIgnoreCase("")) {
//#                                 //    auth = true;
//#                                 //    if (Settings.getInstance().getSetting("generalDebug", false) {
//#                                 //        System.out.println("Th*ATManager: no authentication required because PWD is null");
//#                                 //    }
//#                                 //}
//# 
//#                                 while (true) {
//# 
//#                                     try {
//# 
//#                                         /*
//#                                          * Read command
//#                                          */
//#                                         rcv = 0;
//#                                         comCSD = "";
//#                                         do {
//#                                             rcv = dataIn.read();
//#                                             if (rcv != '\n') {
//#                                                 if (Settings.getInstance().getSetting("generalDebug", false)) {
//#                                                     if (rcv >= 0) {
//#                                                         System.out.print((char) rcv);
//#                                                     }
//#                                                 }
//#                                                 // update string read from CSD
//#                                                 if ((byte) rcv != '\r') {
//#                                                     dataOut.write((byte) rcv);
//#                                                     comCSD = comCSD + (char) rcv;
//#                                                 } else {
//#                                                     dataOut.write("\r\n".getBytes());
//#                                                 }
//#                                             }
//#                                         } while ((char) rcv != '\r');
//#                                         // If '\r' received, process command	
//#                                         if (Settings.getInstance().getSetting("generalDebug", false)) {
//#                                             System.out.println("Th*ATsender, CSD command received: " + comCSD + "***");
//#                                         }
//# 
//#                                         /*
//#                                          * Command processing
//#                                          */
//# 						    			//** Messages accepted with or without authentication **//	
//#                                          if (comCSD.startsWith("$")) {
//#                                             CommandProcessor commandProcessor = CommandProcessor.getInstance();
//#                                             try {
//#                                                 if (commandProcessor.execute(comCSD.substring(1), true)) {
//#                                                     dataOut.write((commandProcessor.message + "\r\n").getBytes());
//#                                                 } else {
//#                                                     dataOut.write(("NACK: " + commandProcessor.message + "\r\n").getBytes());
//#                                                 }
//#                                             } catch (IOException ioe) {
//#                                                 if (Settings.getInstance().getSetting("generalDebug", false)) {
//#                                                     System.out.println("Th*ATsender: IOException");
//#                                                 }
//# 
//#                                             }
//#                                         }
//# 
//#                                         if (Settings.getInstance().getSetting("display", false)) {
//#                                             dataOut.write((InfoStato.getInstance().getRMCTrasp()).getBytes());
//#                                             dataOut.write((InfoStato.getInstance().getGGATrasp()).getBytes());
//#                                         }
//# 
//#                                     } catch (StringIndexOutOfBoundsException siobe) {
//#                                         if (Settings.getInstance().getSetting("generalDebug", false)) {
//#                                             System.out.println("Th*ATsender: CSD exception");
//#                                         }
//#                                         dataOut.write("Command ERROR\n\r".getBytes());
//#                                     }
//# 
//#                                 } //while(true)
//# 
//#                             } catch (IOException ioe) {
//#                                 if (Settings.getInstance().getSetting("generalDebug", false)) {
//#                                     System.out.println("Th*ATsender: IOException");
//#                                 }
//#                             }
//# 
//#                             // At the end of stream use, set ATexec = false
//#                             InfoStato.getInstance().setATexec(false);
//# 
//#                             // Indicates that CSD connection isn't in use yet, to close UpdateSCD
//#                             InfoStato.getInstance().setCSDconnect(false);
//# 
//#                             // Authentication non più valida
//#                             auth = false;
//# 
//#                         } //csdRead			
//# 
//#     
//#endif
}

