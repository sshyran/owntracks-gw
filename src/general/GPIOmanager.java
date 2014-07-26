/*	
 * Class 	GPIOmanager
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import java.io.IOException;

import choral.io.Can;
import choral.io.MovSens;
import choral.io.PowerManager;
import choral.io.UserLed;

/**
 * GPIO manager	thread (different management than GPIO key) and management of
 * 'satellite LED'.
 *
 * @version	1.02 <BR> <i>Last update</i>: 25-10-2007
 * @author alessioza
 *
 */
public class GPIOmanager extends ThreadCustom implements GlobCost {

    /* 
     * local variables
     */
    MovSens move;
    Can can;
    UserLed led;
    PowerManager powerObj;
    private int wdCount = 0;
    private int waitVinCounter = 0;
    private boolean gpsLedState = false;

    /* 
     * constructors
     */
    public GPIOmanager() {
        //System.out.println("Th*GPIOmanager: CREATED");
        move = new MovSens();
        can = new Can();
        led = new UserLed();
        powerObj = new PowerManager();
    }

    /*
     * methods
     */
    public void run() {

        //System.out.println("Th*GPIOmanager: STARTED");
        while (true) {
            try {

				// It is assumed that the GPIO driver is already active!
                /*
                 * INIT GPIO WATCHDOG
                 */
                SemAT.getInstance().getCoin(5);
				// open GPIO n.6 and init to "0"
                //System.out.println("Th*GPIOmanager: activating GPIO n.6...");
                InfoStato.getInstance().writeATCommand("at^scpin=1,5,1,0\r");
                SemAT.getInstance().putCoin();

                while (true) {
                    if (!InfoStato.getInstance().getGPSLive() && wdCount >= 200) {
                        InfoStato.getInstance().setReboot();
                        wdCount = 0;
                    } else if (InfoStato.getInstance().getGPSLive()) {
                        //System.out.println("Refresh GPS " + wdCount);
                        InfoStato.getInstance().setGPSLive(false);
                        wdCount = 0;
                    }
                    wdCount++;

                    /*
                     * Blink of led related to GPS satellite number
                     */
                    if (InfoStato.getInstance().getAttivaSensore() == true) {
                        while (!InfoStato.getMicroSemaphore()) {
                            Thread.sleep(2);
                        }
                        try {
                            move.movSensOn();
                            move.setMovSens(4);
                        } catch (IOException e) {
                        }
                        InfoStato.freeMicroSemaphore();
                        InfoStato.getInstance().setAttivaSensore(false);

                    } //AttivaSensore
                    /*
                     * DISABLE MOTION SENSOR
                     */ else if (InfoStato.getInstance().getDisattivaSensore() == true) {
                        while (!InfoStato.getMicroSemaphore()) {
                            Thread.sleep(2);
                        }
                        try {
                            move.movSensOff();
                        } catch (IOException e) {
                        }
                        InfoStato.freeMicroSemaphore();
                        InfoStato.getInstance().setDisattivaSensore(false);

                    } //DisattivaSensore

                    if ((InfoStato.getInstance().getInfoFileString(MovState)).indexOf("GPSOFF") < 0) {
                        if (gpsLedState != InfoStato.getInstance().getGpsLed()) {
                            gpsLedState = InfoStato.getInstance().getGpsLed();
                            try {
                                led.setLed(gpsLedState);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }

                    waitVinCounter++;
                    Thread.sleep(whileSleep);
                }

            } catch (InterruptedException ie) {
				//System.out.println("exception: " + ie.getMessage());
                //ie.printStackTrace();
            } //catch
            new LogError("Th*GPIOmanager: Reboot");
            //System.out.println("Th*GPIOmanager: END");
        } //while		
    } //run

} //GPIOmanager

