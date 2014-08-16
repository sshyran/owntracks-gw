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
             * Network registration
             */
            if (timeoutType.equalsIgnoreCase(RegTimeout)) {

                // Send command AT+COPS? 
                ATManager.getInstance().executeCommand("AT+COPS?\r");

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

            if (timeoutType.equalsIgnoreCase(trackTimeout)) {

                /*
                 * If you are in a CSD call, wait until it's in progress
                 */
                while (InfoStato.getInstance().getCSDattivo() == true) {
                    Thread.sleep(whileSleep);
                }

                //System.out.println("TimeoutTask, trackTimeout: STARTED");
                //Mailboxes.getInstance(0).write(msgChiaveAttivata);

            } //trackTimeout

        } catch (InterruptedException ie) {
            //System.out.println("exception: " + ie.getMessage());
            //ie.printStackTrace();
        } //catch

    } //run

} //TimeoutTask

