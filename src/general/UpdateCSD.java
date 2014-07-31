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

            // Disable KEY
            InfoStato.getInstance().setInibizioneChiave(true);
            InfoStato.getInstance().setCSDattivo(true);

            // Reserve AT resource
            SemAT.getInstance().getCoin(5);

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
                InfoStato.getInstance().setATexec(true);
                Mailboxes.getInstance(2).write("ATH\r");
                while (InfoStato.getInstance().getATexec()) {
                    if (InfoStato.getInstance().getCSDWatchDog() == false) {
                        break;
                    }
                    Thread.sleep(whileSleep);
                } //while	

            } //if

            // Delete timer and task
            CSDTimer.cancel();
            CSDTimeoutTask.cancel();

        } catch (InterruptedException ie) {
            //System.out.println("UpdateCSD: InterruptedException");
        } //catch

        // Release AT resource
        SemAT.getInstance().putCoin();

        // Enable KEY
        InfoStato.getInstance().setInibizioneChiave(false);
        InfoStato.getInstance().setCSDattivo(false);

        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("Th*UpdateCSD: END");
        }
    }
}

