/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author christoph
 */
public class DateFormatter {
    
    public static Date parse(String DDMMYY, String HHMMSS) {
        long timeStamp = 0;

        int sYear;
        int sMonth;
        int sDay;
        int sHour;
        int sMinute;
        int sSecond;

        try {
            sDay = Integer.parseInt(DDMMYY.substring(0, 2));
            sMonth = Integer.parseInt(DDMMYY.substring(2, 4));
            sYear = Integer.parseInt(DDMMYY.substring(4));
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
                SLog.log(SLog.Warning, "DateFormatter", "Illegal date/time " + DDMMYY + " " + HHMMSS);
            }
        } catch (NumberFormatException nfe) {
            SLog.log(SLog.Error, "DateFormatter", "NumberFormatException " + DDMMYY + " " + HHMMSS);
        }
        return new Date(timeStamp * 1000L);
    }
    
    public static String atString(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        String s = leadingZeroes(cal.get(Calendar.YEAR) - 2000, 2)
                + "/" + leadingZeroes(cal.get(Calendar.MONTH) + 1, 2)
                + "/" + leadingZeroes(cal.get(Calendar.DAY_OF_MONTH), 2)
                + ","
                + leadingZeroes(cal.get(Calendar.HOUR) + (cal.get(Calendar.AM_PM) == Calendar.PM ? 12 : 0), 2)
                + ":" + leadingZeroes(cal.get(Calendar.MINUTE), 2)
                + ":" + leadingZeroes(cal.get(Calendar.SECOND), 2);
        return s;
    }

    public static String isoString(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        String s = ((year < 0) ? "-" : "") 
                + leadingZeroes(Math.abs(cal.get(Calendar.YEAR)), 4)
                + "-" + leadingZeroes(cal.get(Calendar.MONTH) + 1, 2)
                + "-" + leadingZeroes(cal.get(Calendar.DAY_OF_MONTH), 2)
                + " "
                + leadingZeroes(cal.get(Calendar.HOUR) + (cal.get(Calendar.AM_PM) == Calendar.PM ? 12 : 0), 2)
                + ":" + leadingZeroes(cal.get(Calendar.MINUTE), 2)
                + ":" + leadingZeroes(cal.get(Calendar.SECOND), 2);
        return s;
    }

    private static String leadingZeroes(int i, int length) {
        String s = "" + i;
        while (s.length() < length) {
            s = "0" + s;
        }
        return s;
    }
}
