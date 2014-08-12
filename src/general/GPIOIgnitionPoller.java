/*	
 * Class 	GPIOIgnitionPoller
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

/**
 * Thread that controls during application execution if occurs an event of key
 * activation or deactivation.
 *
 * @version	1.00 <BR> <i>Last update</i>: 04-11-2008
 * @author matteobo
 *
 */
public class GPIOIgnitionPoller extends Thread implements GlobCost {

    public boolean ignition;
    /* 
     * constructors
     */
    public GPIOIgnitionPoller() {
        //System.out.println("Th*GPIOIgnitionPoller: CREATED");
    }

    /*
     * methods
     */
    /**
     * Contains thread execution code.
     * <BR>
     * Notify to AppMain an event of activation or deactivation of the key.
     */
    public void run() {

		//System.out.println("Th*GPIOIgnitionPoller: STARTED");
        try {

			//System.out.println("Th*GPIOIgnitionPoller: monitoring key GPIO");
            /*
             * Enables polling on key GPIO and check the initial value
             */

            /*
             * GPIO init
             */
            ATManager.getInstance().executeCommand("at^scpin=1,6,0\r");
            /*
             * Activate GPIO n.1 and n.3
             */
            ATManager.getInstance().executeCommand("at^scpin=1,0,0\r");
            ATManager.getInstance().executeCommand("at^scpin=1,2,0\r");

            /*
             * Activate polling
             */
            ATManager.getInstance().executeCommand("at^scpol=1,6\r");

            /*
             * Activate polling for GPIO n.1 and n.3
             */
            ATManager.getInstance().executeCommand("at^scpol=1,0\r");
            ATManager.getInstance().executeCommand("at^scpol=1,2\r");

            /*
             * Check GPIO initial values
             */
            InfoStato.getInstance().setGPIOnumberTEST(7);
            ATManager.getInstance().executeCommand("at^sgio=6\r");
            /*
             * Check GPIO n.1 and n.3 initial values
             */
            InfoStato.getInstance().setGPIOnumberTEST(1);
            ATManager.getInstance().executeCommand("at^sgio=0\r");		//GPIO1

            InfoStato.getInstance().setGPIOnumberTEST(3);
            ATManager.getInstance().executeCommand("at^sgio=2\r");		//GPIO3

            // Notify to AppMain that polling is enabled
            InfoStato.getInstance().setPollingAttivo(true);
            Mailboxes.getInstance(0).write(msgALIVE);

            // MAIN LOOP
            while (true) {

                // Pause
                Thread.sleep(whileSleepGPIO);

                InfoStato.getInstance().setGPIOnumberTEST(7);
                ATManager.getInstance().executeCommand("at^sgio=6\r");

                /*
                 * KEY ACTIVATED (GPIO = "0")
                 */
                if (InfoStato.getInstance().getGPIOchiave() == 0) {

                    // Send msg to AppMain
                    if (Settings.getInstance().getSetting("keyDebug", false)) {
                        System.out.println("Th*TestChiave: KEY ACTIVATED!!!");
                    }
                    ignition = true;

                    // stop send of other messages
                    InfoStato.getInstance().setGPIOchiave(-1);

                } //KEY ACTIVATED
                /*
                 * KEY DEACTIVATED (GPIO = "1")
                 */ else if (InfoStato.getInstance().getGPIOchiave() == 1) {

                    // Send msg to AppMain
                     ignition = false;
                    if (Settings.getInstance().getSetting("keyDebug", false)) {
                        System.out.println("Th*TestChiave: KEY DEACTIVATED!!!");
                    }

                    // blocco invio di ulteriori messaggi
                    InfoStato.getInstance().setGPIOchiave(-1);
					//break;

                } //KEY DEACTIVATED

            } //while(true)

        } catch (InterruptedException ie) {
			//System.out.println("exception: " + ie.getMessage());
            //ie.printStackTrace();
        } //catch

    } //run

} //TestChiave

