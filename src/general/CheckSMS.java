/*	
 * Class 	CheckSMS
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import choral.io.InfoMicro;

/**
 * Thread dedicated to SMS reception during application execution.
 *
 * @version	1.04 <BR> <i>Last update</i>: 14-12-2007
 * @author alessioza
 *
 */
public class CheckSMS extends Thread implements GlobCost {

    /* 
     * local variables
     */
    private String text;
    int num = 0;
    InfoMicro infoGW;
    String release = "";
    int tempTimer1 = 10000;
    int countTimer1 = 0;

    /* 
     * constructors
     */
    public CheckSMS() {
        //System.out.println("Th*CheckSMS: CREATED");
    }

    /*
     * methods
     */
    /**
     * Contains thread execution code.
     * <BR>
     * Notify to AppMain reception of a new SMS for tracking.
     */
    public void run() {

		//System.out.println("Th*CheckSMS: STARTED");
        try {

            try {
                infoGW = new InfoMicro();
                release = infoGW.getRelease();
            } catch (Exception e) {
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
                    if (debug) {
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

            if (debug) {
                System.out.println("Th*CheckSMS: config OK");
                System.out.println("Th*CheckSMS: wait for new SMS");
            }

            // Reset 'numSMS' and 'CodSMS'
            InfoStato.getInstance().setNumSMS(0);
            InfoStato.getInstance().setCodSMS(-1);

        } catch (StringIndexOutOfBoundsException sie) {
            //System.out.println("CheckSMS: StringIndexOutOfBoundsException (1st part)");
            new LogError("CheckSMS StringIndexOutOfBoundsException (1st part)");

        } catch (Exception e) {
            //System.out.println("CheckSMS: generic Exception (1st part)");
            new LogError("CheckSMS generic Exception (1st part)");
        }

        // MAIN LOOP, wait always for new SMS
        while (true) {

            // Check Crash Alarm
            if (InfoStato.getInstance().getAlarmCrash()) {
                InfoStato.getInstance().setAlarmCrash(false);
                String messaggio = InfoStato.getInstance().getCoordinate();
                // send alarm to operator
                InfoStato.getInstance().setATexec(true);
                Mailboxes.getInstance(2).write("AT+CMGS=\"" + InfoStato.getInstance().getInfoFileString(Operatore) + "\"\r");
                if (debug) {
                    System.out.println("AT+CMGS=\"" + InfoStato.getInstance().getInfoFileString(Operatore) + "\"\r");
                }
                while (InfoStato.getInstance().getATexec()) {
                    try {
                        Thread.sleep(whileSleep);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                // PAY ATTENTION, answer to wait isn't 'OK' but '>' -> OK
                InfoStato.getInstance().setATexec(true);
                Mailboxes.getInstance(2).write(messaggio + "\032");

                while (InfoStato.getInstance().getATexec()) {
                    try {
                        Thread.sleep(whileSleep);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }// end check crash alarm

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

                    if (debug) {
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
                        System.out.println("Th*CheckSMS: " + InfoStato.getInstance().getNumTelSMS() + " VALID? " + InfoStato.getInstance().getValidSMS() + " KEY: " + InfoStato.getInstance().getSMSCommand());
                        try {
                            /*
                             * Check if '+' is present, in the telephone number
                             */
                            if (InfoStato.getInstance().getNumTelSMS().indexOf("+") >= 0 && InfoStato.getInstance().getValidSMS() == true) {

                                if (InfoStato.getInstance().getSMSCommand().equalsIgnoreCase(keySMS)) {
                                    try {

                                        /*
                                         * formatting sms text
                                         */
                                        if (InfoStato.getInstance().getValidFIX() == true) {
                                            text = "SW Java:" + revNumber + "\r\n" + "FW:" + release + "\r\n" + InfoStato.getInstance().getInfoFileString(Header) + "-" + InfoStato.getInstance().getInfoFileString(IDtraker) + ":\r\n"
                                                    + InfoStato.getInstance().getDataSMS(1) + " " + InfoStato.getInstance().getDataSMS(2) + " GMT\r\n"
                                                    + "LAT: " + InfoStato.getInstance().getDataSMS(3) + "\r\n"
                                                    + "LON: " + InfoStato.getInstance().getDataSMS(4) + "\r\n"
                                                    //+ "ROT: " + InfoStato.getInstance().getDataSMS(5)  + "\r\n"
                                                    + "ALT: " + InfoStato.getInstance().getDataSMS(6) + " m" + "\r\n"
                                                    + "VEL: " + InfoStato.getInstance().getDataSMS(7) + " kmh" + "\r\n"
                                                    + "BATT: " + InfoStato.getInstance().getBatteryVoltage();

                                        }
										//System.out.println("SMS length: " + text.length());									} catch(StringIndexOutOfBoundsException sie) {
                                        //System.out.println("CheckSMS: StringIndexOutOfBoundsException (text)");
                                    } catch (Exception e) {
                                        //System.out.println("\r\nexception: " + e.getMessage());
                                        e.printStackTrace();
										//System.out.println("\r\n");
                                        //System.out.println("CheckSMS: generic Exception (text)");
                                        new LogError("CheckSMS generic Exception (text)");
                                        text = "Data not available";
                                    }

                                    /*
                                     * Convert telephone number to integer, if not a number
                                     * throws the exception NumberFormatException and exit from 'if'
                                     */
									//System.out.print("Extracted number: " + InfoStato.getInstance().getNumTelSMS() + ",coversion: ");
                                    //System.out.println(Integer.parseInt(InfoStato.getInstance().getNumTelSMS().substring(6)));
                                    // Send msg to sender
                                    InfoStato.getInstance().setATexec(true);
                                    Mailboxes.getInstance(2).write("AT+CMGS=\"" + InfoStato.getInstance().getNumTelSMS() + "\"\r");
                                    if (debug) {
                                        System.out.println("AT+CMGS=\"" + InfoStato.getInstance().getNumTelSMS() + "\"\r");
                                    }
                                    while (InfoStato.getInstance().getATexec()) {
                                        Thread.sleep(whileSleep);
                                    }

                                    // PAY ATTENTION, answer to wait isn't 'OK' but '>' -> OK
                                    InfoStato.getInstance().setATexec(true);
                                    Mailboxes.getInstance(2).write(text + "\032");

                                    while (InfoStato.getInstance().getATexec()) {
                                        Thread.sleep(whileSleep);
                                    }

                                    if (debug) {
                                        System.out.println("Th*CheckSMS: Inviato SMS di risposta al mittente");
                                    }
                                } else {
                                    if (InfoStato.getInstance().getSMSCommand().equalsIgnoreCase(keySMS1)) {

                                        InfoStato.getInstance().setReboot();

                                    } else if (InfoStato.getInstance().getSMSCommand().equalsIgnoreCase(keySMS2)) {

                                        text = "+CSQ:" + InfoStato.getInstance().getCSQ() + ";BEARER:" + InfoStato.getInstance().getGPRSBearer()
                                                + ";CREG:" + InfoStato.getInstance().getCREG() + ";CGREG:" + InfoStato.getInstance().getCGREG() + ";ERR:"
                                                + InfoStato.getInstance().getERROR() + ";IN:" + InfoStato.getInstance().getInfoFileInt(TrkIN) + ";OUT:" + InfoStato.getInstance().getInfoFileInt(TrkOUT)
                                                + ";t1:" + InfoStato.getInstance().getTask1Timer() + ";t2:" + InfoStato.getInstance().getTask2Timer() + ";t3:" + InfoStato.getInstance().getTask3Timer()
                                                + ";uFW:" + release + ";SW:" + revNumber;

                                        // Send msg to sender
                                        InfoStato.getInstance().setATexec(true);
                                        Mailboxes.getInstance(2).write("AT+CMGS=\"" + InfoStato.getInstance().getNumTelSMS() + "\"\r");
                                        if (debug) {
                                            System.out.println("AT+CMGS=\"" + InfoStato.getInstance().getNumTelSMS() + "\"\r");
                                        }
                                        while (InfoStato.getInstance().getATexec()) {
                                            Thread.sleep(whileSleep);
                                        }

                                        // PAY ATTENTION, answer to wait isn't 'OK' but '>' -> OK
                                        InfoStato.getInstance().setATexec(true);
                                        Mailboxes.getInstance(2).write(text + "\032");

                                        while (InfoStato.getInstance().getATexec()) {
                                            Thread.sleep(whileSleep);
                                        }

                                    }
                                }

                            } //if
                            /*
                             * If '+' not present, number is invalid -> not answers
                             */ else {
                                //System.out.println("Th*CheckSMS: No anser to SMS because telephone number or MSS text are invalid");
                            } //else

                        } catch (NumberFormatException nfe) {
                            //System.out.println("Th*CheckSMS: NumberFormatException");
                            new LogError("CheckSMS NumberFormatException");
                        }

                        // Delete message
                        InfoStato.getInstance().setATexec(true);
                        Mailboxes.getInstance(2).write("AT+CMGD=" + num + "\r");
                        while (InfoStato.getInstance().getATexec()) {
                            Thread.sleep(whileSleep);
                        }

                    } catch (InterruptedException ie) {
                        //System.out.println("CheckSMS: InterruptedException (2nd part)");
                        new LogError("CheckSMS InterruptedException (2nd part)");

                    } catch (StringIndexOutOfBoundsException sie) {
                        //System.out.println("CheckSMS: StringIndexOutOfBoundsException (2nd part)");
                        new LogError("CheckSMS StringIndexOutOfBoundsException (2nd part)");

                    } catch (Exception e) {
                        //System.out.println("CheckSMS: generic Exception (2nd part)");
                        new LogError("CheckSMS generic Exception (2nd part)");
                    }

                    SemAT.getInstance().putCoin();

                    // Reset 'numSMS' and 'CodSMS'
                    InfoStato.getInstance().setNumSMS(0);
                    InfoStato.getInstance().setCodSMS(-1);

                } else {
                    num = 0;
                    try {
                        Thread.sleep(SMSsleep);
                    } catch (InterruptedException ie) {
                        //System.out.println("CheckSMS: InterruptedException (SMSsleep)");
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
