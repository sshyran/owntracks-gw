/*	
 * Class 	GoToPowerDown
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import java.util.Calendar;
import java.util.Date;
import java.util.EmptyStackException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotOpenException;

/**
 * Sets the application to put the module in Power Down, setting date and time
 * of awakening of the module and writing to FILE all data and system settings
 * to recover at next start of the application.
 *
 * @version	1.02 <BR> <i>Last update</i>: 25-10-2007
 * @author alessioza
 *
 */
public class GoToPowerDown extends Thread implements GlobCost {

    /*
     * local variables 
     */
    protected Object objGPRMC;
    protected String GPRMCorig, GPRMCmod;
    protected String dataGPRMC = "00/00/00";
    protected String oraGPRMC = "00:00:00";
    protected String timeSveglia;
    protected int virgIndex;
    protected boolean fixFlag = false;
    // Array for GPS position strings
    private String[] arrayGPS;

    /*
     * constructors 
     */
    public GoToPowerDown() {
        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("Th*GoToPowerDown: CREATED");
        }
        // Final GPS string has 19 chars (without checksum, added after)
        arrayGPS = new String[19];
    }

    /*
     * methods
     */
    public void run() {
        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("Th*GoToPowerDown: STARTED");
        }

        Date date = LocationManager.getInstance().dateLastFix();
        if (date != null) {
            if (Settings.getInstance().getSetting("generalDebug", false)) {
                System.out.println("PowerDown @ last fix time " + date.toString());
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            String rtc = "at+cclk=\""
                    + (cal.get(Calendar.YEAR) - 2000) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DAY_OF_MONTH)
                    + ","
                    + cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND)
                    + "\"\r";

            ATManager.getInstance().executeCommand(rtc);
        }

        date = new Date();
        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("PowerDown @ " + date.toString());
        }
        
        if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execBATTSCARICA)) {
            if (Settings.getInstance().getSetting("generalDebug", false)) {
                System.out.println("PowerDown on low battery, no wakeup call");
            }

        } else {
            date.setTime(date.getTime() + Settings.getInstance().getSetting("sleep", 6 * 3600) * 1000L);

            if (Settings.getInstance().getSetting("generalDebug", false)) {
                System.out.println("PowerDown: setting wakeup call for " + date.toString());
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            String rtc = "at+cala=\""
                    + (cal.get(Calendar.YEAR) - 2000) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DAY_OF_MONTH)
                    + ","
                    + cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND)
                    + "\"\r";

            ATManager.getInstance().executeCommand(rtc);
        }

        if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execFIRST)) {

            if (Settings.getInstance().getSetting("generalDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execFIRST);
            }

            // Key deactivation FIRST
            Settings.getInstance().setSetting("closeMode", closeAppDisattivChiaveFIRST);
                //FlashFile.getInstance().setImpostazione(CloseMode, closeAppDisattivChiaveFIRST);
            // pay attention, MODIFY FOR DEBUG
            //FlashFile.getInstance().setImpostazione(CloseMode, closeAppFactory);

        } // NORMAL EXECUTION
        else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execNORMALE)) {

            if (Settings.getInstance().getSetting("generalDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execNORMALE);
            }

            // Normal OK
            if (LocationManager.getInstance().isFix()) {
                Settings.getInstance().setSetting("closeMode", closeAppNormaleOK);
            } else {
                Settings.getInstance().setSetting("closeMode", closeAppNormaleTimeout);
            }

        } // KEY DEACTIVATED
        else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execCHIAVEdisattivata)) {

            if (Settings.getInstance().getSetting("generalDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execCHIAVEdisattivata);
            }

            if (LocationManager.getInstance().isFix()) {
                Settings.getInstance().setSetting("closeMode", closeAppDisattivChiaveOK);
            } else {
                Settings.getInstance().setSetting("closeMode", closeAppDisattivChiaveTimeout);
            }

        } // MOVEMENT
        else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execMOVIMENTO)) {

            if (Settings.getInstance().getSetting("generalDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execMOVIMENTO);
            }

            // Movement OK
            Settings.getInstance().setSetting("closeMode", closeAppMovimentoOK);

            //FlashFile.getInstance().setImpostazione(CloseMode, closeAppMovimentoOK);
        } // AFTER RESET
        else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execPOSTRESET)) {

            if (Settings.getInstance().getSetting("generalDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execPOSTRESET);
            }

            // Key deactivation after RESET
            Settings.getInstance().setSetting("closeMode", closeAppPostReset);

            //FlashFile.getInstance().setImpostazione(CloseMode, closeAppPostReset);
        } // BATTERY LOW
        else if (InfoStato.getInstance().getSTATOexecApp().equalsIgnoreCase(execBATTSCARICA)) {

            if (Settings.getInstance().getSetting("generalDebug", false)) {
                System.out.println("Th*GoToPowerDown, closure from status :" + execBATTSCARICA);
            }

            // Battery Low
            Settings.getInstance().setSetting("closeMode", closeAppBatteriaScarica);
        }

            /* 
         * Notify to AppMain to close application
         */
        Mailboxes.getInstance(0).write(msgClose);
        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("Th*GoToPowerDown: sent message: " + msgClose);
        }


}
}
