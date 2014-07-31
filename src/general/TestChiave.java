/*	
 * Class 	TestChiave
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
public class TestChiave extends Thread implements GlobCost {

    /* 
     * constructors
     */
    public TestChiave() {
        //System.out.println("Th*TestChiave: CREATED");
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

		//System.out.println("Th*TestChiave: STARTED");
        try {

			//System.out.println("Th*TestChiave: monitoring key GPIO");
            /*
             * Enables polling on key GPIO and check the initial value
             */
            // reserve AT resource
            SemAT.getInstance().getCoin(5);

            /*
             * GPIO init
             */
            InfoStato.getInstance().writeATCommand("at^scpin=1,6,0\r");
            /*
             * Activate GPIO n.1 and n.3
             */
            InfoStato.getInstance().writeATCommand("at^scpin=1,0,0\r");
            InfoStato.getInstance().writeATCommand("at^scpin=1,2,0\r");

            /*
             * Activate polling
             */
            InfoStato.getInstance().writeATCommand("at^scpol=1,6\r");

            /*
             * Activate polling for GPIO n.1 and n.3
             */
            InfoStato.getInstance().writeATCommand("at^scpol=1,0\r");
            InfoStato.getInstance().writeATCommand("at^scpol=1,2\r");

            /*
             * Check GPIO initial values
             */
            InfoStato.getInstance().setATexec(true);
            InfoStato.getInstance().setGPIOnumberTEST(7);
            Mailboxes.getInstance(2).write("at^sgio=6\r");
            while (InfoStato.getInstance().getATexec()) {
                Thread.sleep(whileSleep);
            }

            /*
             * Check GPIO n.1 and n.3 initial values
             */
            InfoStato.getInstance().setATexec(true);
            InfoStato.getInstance().setGPIOnumberTEST(1);
            Mailboxes.getInstance(2).write("at^sgio=0\r");		//GPIO1
            while (InfoStato.getInstance().getATexec()) {
                Thread.sleep(whileSleep);
            }

            InfoStato.getInstance().setATexec(true);
            InfoStato.getInstance().setGPIOnumberTEST(3);
            Mailboxes.getInstance(2).write("at^sgio=2\r");		//GPIO3
            while (InfoStato.getInstance().getATexec()) {
                Thread.sleep(whileSleep);
            }

            // Release At resource
            SemAT.getInstance().putCoin();

            // Notify to AppMain that polling is enabled
            InfoStato.getInstance().setPollingAttivo(true);
            Mailboxes.getInstance(0).write(msgALIVE);

            // MAIN LOOP
            while (true) {

                // Pause
                Thread.sleep(whileSleepGPIO);

                SemAT.getInstance().getCoin(5);
                InfoStato.getInstance().setATexec(true);
                InfoStato.getInstance().setGPIOnumberTEST(7);
                Mailboxes.getInstance(2).write("at^sgio=6\r");
                while (InfoStato.getInstance().getATexec()) {
                    Thread.sleep(whileSleep);
                }
                SemAT.getInstance().putCoin();

                /*
                 * KEY ACTIVATED (GPIO = "0")
                 */
                if (InfoStato.getInstance().getGPIOchiave() == 0) {

                    // Send msg to AppMain
                    if (Settings.getInstance().getSetting("keyDebug", false)) {
                        System.out.println("Th*TestChiave: KEY ACTIVATED!!!");
                    }
                    Mailboxes.getInstance(0).write(msgChiaveAttivata);

                    // stop send of other messages
                    InfoStato.getInstance().setGPIOchiave(-1);

                } //KEY ACTIVATED
                /*
                 * KEY DEACTIVATED (GPIO = "1")
                 */ else if (InfoStato.getInstance().getGPIOchiave() == 1) {

                    // Send msg to AppMain
                    Mailboxes.getInstance(0).write(msgChiaveDisattivata);
                    /*SemAT.getInstance().getCoin(5);
                     InfoStato.getInstance().setATexec(true);
                     Mailboxes.getInstance(2).write("at^spio=0\r");
                     while(InfoStato.getInstance().getATexec()) { Thread.sleep(whileSleep); }
                     SemAT.getInstance().putCoin();
                     */
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

