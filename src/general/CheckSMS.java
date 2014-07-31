/*	
 * Class 	CheckSMS
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import choral.io.InfoMicro;
import java.io.IOException;

/**
 * Thread dedicated to SMS reception during application execution.
 *
 * @version	1.04 <BR> <i>Last update</i>: 14-12-2007
 * @author alessioza
 *
 */
public class CheckSMS extends Thread implements GlobCost {

    private String text;
    int num = 0;
    InfoMicro infoGW;
    String release = "";
    int tempTimer1 = 10000;
    int countTimer1 = 0;

    public CheckSMS() {
    }

    /**
     * Contains thread execution code.
     * <BR>
     * Notify to AppMain reception of a new SMS for tracking.
     */
    public void run() {
        try {
            try {
                infoGW = new InfoMicro();
                release = infoGW.getRelease();
            } catch (IOException ie) {
                ie.printStackTrace();
            }
            /*
             * Config module for SMS reception
             */
            SemAT.getInstance().getCoin(5);
            InfoStato.getInstance().writeATCommand("AT+CMGF=1\r");
            InfoStato.getInstance().writeATCommand("AT+CPMS=\"MT\",\"MT\",\"MT\"\r");
            InfoStato.getInstance().writeATCommand("AT+CNMI=1,1\r");

            /*
             * Analyze SMS presence on ME and delete
             */
            while (true) {
                InfoStato.getInstance().writeATCommand("AT+CPMS?\r");

                /*
                 * Extract how many SMS are already read in the memory.
                 * Exit from loop if no SMS are found.
                 */
                if (InfoStato.getInstance().getNumSMS() > 0) {

                    if (num > InfoStato.getInstance().getMaxNumSMS()) {
                        num = 0;
                    }
                    num++;

                    // Delete message
                    InfoStato.getInstance().writeATCommand("AT+CMGD=" + num + "\r");
                    if (Settings.getInstance().getSetting("generalDebug", false)) {
                        System.out.println("Th*CheckSMS: deleted msg n. " + num + " from ME");
                    }

                } else {
                    num = 0;
                    break;
                }

                // Reset 'numSMS' and 'CodSMS'
                InfoStato.getInstance().setNumSMS(0);
                InfoStato.getInstance().setCodSMS(-1);

            } //while

            SemAT.getInstance().putCoin();

            if (Settings.getInstance().getSetting("generalDebug", false)) {
                System.out.println("Th*CheckSMS: config OK");
                System.out.println("Th*CheckSMS: wait for new SMS");
            }

            // Reset 'numSMS' and 'CodSMS'
            InfoStato.getInstance().setNumSMS(0);
            InfoStato.getInstance().setCodSMS(-1);

        } catch (StringIndexOutOfBoundsException sie) {
            new LogError("CheckSMS StringIndexOutOfBoundsException (1st part)");

        } catch (Exception e) {
            new LogError("CheckSMS generic Exception (1st part)");
        }

        // MAIN LOOP, wait always for new SMS
        while (true) {

            // Check Crash Alarm, if so send alarm to operator
            if (InfoStato.getInstance().getAlarmCrash()) {
                InfoStato.getInstance().setAlarmCrash(false);
                String messaggio = InfoStato.getInstance().getCoordinate();
                InfoStato.getInstance().writeATCommand("AT+CMGS=\"" + Settings.getInstance().getSetting("operator", "") + "\"\r");
                InfoStato.getInstance().writeATCommand("AT+CMGS=\"" + InfoStato.getInstance().getCoordinate() + "\032");
            }

            try {
                SemAT.getInstance().getCoin(5);
                InfoStato.getInstance().writeATCommand("AT+CPMS?\r");
                SemAT.getInstance().putCoin();

                /*
                 * If messages are present in memory, list all and read li e leggili one at time,
                 * consider and execute only the last
                 */
                if (InfoStato.getInstance().getNumSMS() > 0) {

                    num++;
                    if (num > InfoStato.getInstance().getMaxNumSMS()) {
                        num = 1;
                    }

                    if (Settings.getInstance().getSetting("generalDebug", false)) {
                        System.out.println("Th*CheckSMS: NumSMS = " + InfoStato.getInstance().getNumSMS());
                    }

                    SemAT.getInstance().getCoin(5);

                    try {

                        // Read message
                        InfoStato.getInstance().writeATCommand("AT+CMGR=" + num + "\r");

                        // extract telephone number to answer, with InfoStato.getInstance().getNumTelSMS()
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            //System.out.println("CheckSMS: InterruptedException (SMSsleep)");
                            new LogError("CheckSMS InterruptedException (SMSsleep)");
                        }
                        System.out.println("Th*CheckSMS: " + InfoStato.getInstance().getNumTelSMS()
                                + " command: " + InfoStato.getInstance().getSMSCommand());
                        try {
                            if (InfoStato.getInstance().getNumTelSMS().indexOf("+") >= 0) {

                                if (CommandProcessor.getInstance().execute(InfoStato.getInstance().getSMSCommand(), false)) {
                                    InfoStato.getInstance().writeATCommand("AT+CMGS=\"" + InfoStato.getInstance().getNumTelSMS() + "\"\r");
                                    InfoStato.getInstance().writeATCommand(CommandProcessor.getInstance().message + "\032");
                                }
                            }
                        } catch (NumberFormatException nfe) {
                            new LogError("CheckSMS NumberFormatException");
                        }

                        // Delete message
                        InfoStato.getInstance().setATexec(true);
                        Mailboxes.getInstance(2).write("AT+CMGD=" + num + "\r");
                        while (InfoStato.getInstance().getATexec()) {
                            Thread.sleep(whileSleep);
                        }

                    } catch (InterruptedException ie) {
                        new LogError("CheckSMS InterruptedException (2nd part)");

                    } catch (StringIndexOutOfBoundsException sie) {
                        new LogError("CheckSMS StringIndexOutOfBoundsException (2nd part)");

                    } catch (Exception e) {
                        new LogError("CheckSMS generic Exception (2nd part)");
                    }

                    SemAT.getInstance().putCoin();

                    InfoStato.getInstance().setNumSMS(0);
                    InfoStato.getInstance().setCodSMS(-1);

                } else {
                    num = 0;
                    try {
                        Thread.sleep(SMSsleep);
                    } catch (InterruptedException ie) {
                        new LogError("CheckSMS InterruptedException (SMSsleep)");
                    }
                } //else

                if (countTimer1 > 5) {
                    countTimer1 = 0;
                    if (tempTimer1 == InfoStato.getInstance().getTask1Timer()) {
                        InfoStato.getInstance().setReboot();
                    }
                    tempTimer1 = InfoStato.getInstance().getTask1Timer();
                }
                countTimer1++;
                Thread.sleep(2000);
            } catch (Exception e) {
                new LogError("Exception SMS");
            }
        } //while(true)

    } //run

} //ChechSMS
