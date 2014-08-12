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
public class GPIOmanager extends Thread implements GlobCost {

    /* 
     * local variables
     */
    MovSens move;
    Can can;
    PowerManager powerObj;
    private int waitVinCounter = 0;

    /* 
     * constructors
     */
    public GPIOmanager() {
        //System.out.println("Th*GPIOmanager: CREATED");
        move = new MovSens();
        can = new Can();
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
                while (true) {
                    /*
                     * Blink of led related to GPS satellite number ???
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

