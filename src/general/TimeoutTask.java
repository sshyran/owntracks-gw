/*	
 * Class 	TimeoutTask
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import java.util.TimerTask;

/**
 * Operations to execute when timeout expires, depending on the type of timeout.
 *
 * @version	1.02 <BR> <i>Last update</i>: 25-10-2007
 * @author alessioza
 *
 */
public class TimeoutTask extends TimerTask implements GlobCost {

    /* 
     * local variables
     */
    private String timeoutType;
    //Runtime r;
    //long mem1, mem2;

    /* 
     * constructors
     */
    public TimeoutTask(String type) {
        //System.out.println("TimeoutTask: CREATED");
        timeoutType = type;
        //r = Runtime.getRuntime();
    }

    /*
     * methods
     */
    /**
     * Task execution code.
     * <BR> ---------- <BR>
     * Performed operations: <br>
     * <ul type="disc">
     * <li> Check timeout type, based on value passed through constructor;
     * <li> Send message or sets parameters needed to notify about timeout
     * expiration.
     * </ul>
     */
    public void run() {
        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("TimeoutTask: " + timeoutType);
        }

        try {

            /*
             * FIXtimeout 
             */
            if (timeoutType.equalsIgnoreCase(FIXgpsTimeout)) {

                //System.out.println("TimeoutTask, FIXgpsTimeout: STARTED");
				/*
                 * If there is a valid FIX do nothing at timeout expiration
                 */
                if (InfoStato.getInstance().getValidFIX() == true) {

                    if (Settings.getInstance().getSetting("generalDebug", false)) {
                        System.out.println("TimeoutTask, FIXgpsTimeout: FIXtimeout EXPIRED but FIX found");
                    }

                } else {

                    if (Settings.getInstance().getSetting("generalDebug", false)) {
                        System.out.println("TimeoutTask, FIXgpsTimeout: FIXtimeout EXPIRED");
                    }

                    // Set that 'FIXtimeout' is expired
                    InfoStato.getInstance().setIfIsFIXtimeoutExpired(true);

                } //if

            } //FIXtimeout

            /*
             * FIXgprsTimeout 
             */
            if (timeoutType.equalsIgnoreCase(FIXgprsTimeout)) {

                if (Settings.getInstance().getSetting("generalDebug", false)) {
                    System.out.println("TimeoutTask, FIXgprsTimeout: FIXgprsTimeout EXPIRED");
                }

                // Set that 'FIXgprsTimeout' is expired
                InfoStato.getInstance().setIfIsFIXgprsTimeoutExpired(true);

                // Send msg to TrackingGPRS mailbox
                Mailboxes.getInstance(3).write(timeoutExpired);

            } //FIXgprsTimeout

            /*
             * CHIAVEtimeout
             */
            if (timeoutType.equalsIgnoreCase(CHIAVEtimeout)) {

                //System.out.println("TimeoutTask, CHIAVEtimeout: STARTED");
				/*
                 * At timeout expiration, ENABLE again the key usage
                 */
                InfoStato.getInstance().setInibizioneChiave(false);

            } //CHIAVEtimeout

            /*
             * BatteryTimeout
             */
            if (timeoutType.equalsIgnoreCase(BatteryTimeout)) {

                //System.out.println("TimeoutTask, BatteryTimeout: STARTED");
                // Send command AT^SBV 
                SemAT.getInstance().getCoin(5);
                //System.out.println("***   TimeoutTask, BatteryTimeout: EXEC COMMAND AT^SBV");
                InfoStato.getInstance().writeATCommand("AT^SBV\r");
                SemAT.getInstance().putCoin();

            } //BatteryTimeout

            /*
             * Network registration
             */
            if (timeoutType.equalsIgnoreCase(RegTimeout)) {

                // Send command AT+COPS? 
                SemAT.getInstance().getCoin(5);
                InfoStato.getInstance().writeATCommand("AT+COPS?\r");
                SemAT.getInstance().putCoin();

                //mem1 = r.freeMemory();
                //System.out.println("***Free memory before garbage collection: " + mem1);
                //r.gc();
                //mem1 = r.freeMemory();
                //System.out.println("***Free memory after garbage collection: " + mem1);
            } //RegTimeout


            /*
             * CSDtimeout
             */
            if (timeoutType.equalsIgnoreCase(CSDtimeout)) {

                //System.out.println("TimeoutTask, CSDtimeout: STARTED");
				/*
                 * At timeout expiration, disable CSD call
                 */
                InfoStato.getInstance().setCSDWatchDog(false);

            } //CSDtimeout

            /*
             * FIXgprsTimeout 
             */
            if (timeoutType.equalsIgnoreCase(trackTimeout)) {

                /*
                 * If you are in a CSD call, wait until it's in progress
                 */
                while (InfoStato.getInstance().getCSDattivo() == true) {
                    Thread.sleep(whileSleep);
                }

                //System.out.println("TimeoutTask, trackTimeout: STARTED");
                Mailboxes.getInstance(0).write(msgChiaveAttivata);

            } //trackTimeout

        } catch (InterruptedException ie) {
            //System.out.println("exception: " + ie.getMessage());
            //ie.printStackTrace();
        } //catch

    } //run

} //TimeoutTask

