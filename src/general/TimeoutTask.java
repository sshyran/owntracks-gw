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
public class TimeoutTask extends TimerTask {

    public static final String RegTimeout = "RegTimeout";
    public static final String trackTimeout = "TimeoutTrackingCHIAVE";
    
    public static final int whileSleep = 100;	// standard while loops


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

            /*
             * Network registration
             */
            if (timeoutType.equalsIgnoreCase(RegTimeout)) {
           int countReg = 0;
        

                // Send command AT+COPS? 
                String cops = ATManager.getInstance().executeCommandSynchron("AT+COPS?\r");
            if (cops.indexOf(",") >= 0) {
                countReg = 0;
            } else {
                countReg++;
            }
            if (countReg > 10) {
                new LogError("NO NETWORK");
                AppMain.getInstance().network = false;
            }

                //mem1 = r.freeMemory();
                //System.out.println("***Free memory before garbage collection: " + mem1);
                //r.gc();
                //mem1 = r.freeMemory();
                //System.out.println("***Free memory after garbage collection: " + mem1);
            } //RegTimeout

    } //run

} //TimeoutTask

