/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import java.util.Date;

/**
 *
 * @author christoph
 */
public class DateParser {

    private Date date;

    public DateParser(String YYMMDD, String HHMMSS) {
        long timeStamp = 0;

        int sYear;
        int sMonth;
        int sDay;
        int sHour;
        int sMinute;
        int sSecond;

        try {
            sYear = Integer.parseInt(YYMMDD.substring(0, 2));
            sMonth = Integer.parseInt(YYMMDD.substring(2, 4));
            sDay = Integer.parseInt(YYMMDD.substring(4));
            sHour = Integer.parseInt(HHMMSS.substring(0, 2));
            sMinute = Integer.parseInt(HHMMSS.substring(2, 4));
            sSecond = Integer.parseInt(HHMMSS.substring(4));

            if (sYear >= 0
                    || sYear <= 99
                    || sMonth >= 1
                    || sMonth <= 12
                    || sDay >= 1
                    || (sDay <= 28 && sMonth == 2 && (sYear + 2000) % 4 != 0)
                    || (sDay <= 29 && sMonth == 2 && (sYear + 2000) % 4 == 0)
                    || (sDay <= 30 && (sMonth == 4 || sMonth == 6 || sMonth == 9 || sMonth == 11))
                    || (sDay <= 31 && (sMonth == 1 || sMonth == 3 || sMonth == 5 || sMonth == 7 || sMonth == 8 || sMonth == 10 || sMonth == 12))
                    || sHour >= 0
                    || sHour <= 23
                    || sMinute >= 0
                    || sMinute <= 59
                    || sSecond >= 0
                    || sSecond <= 59) {
                for (int year = 1970; year < (sYear + 2000); year++) {
                    if (year % 4 == 0) {
                        timeStamp += 366L * 24L * 3600L;
                    } else {
                        timeStamp += 365L * 24L * 3600L;
                    }
                }
                for (int month = 1; month < sMonth; month++) {
                    if (month == 4 || month == 6 || month == 9 || month == 11) {
                        timeStamp += 30L * 24L * 3600L;
                    } else if (month == 2) {
                        if ((sYear + 2000) % 4 == 0) {
                            timeStamp += 29L * 24L * 3600L;
                        } else {
                            timeStamp += 28L * 24L * 3600L;
                        }
                    } else {
                        timeStamp += 31L * 24L * 3600L;
                    }
                }
                timeStamp += (sDay - 1) * 24L * 3600L + sHour * 3600L + sMinute * 60L + sSecond;
            } else {
                System.err.println("Illegal date/time " + YYMMDD + " " + HHMMSS);
            }
        } catch (NumberFormatException nfe) {
            System.err.println("NumberFormatException " + YYMMDD + " " + HHMMSS);
        }
        date = new Date(timeStamp * 1000L);
    }

    public Date getDate() {
        return date;
    }
}
