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
public class DateParser {
    private Date date = null;

    public DateParser(String YYMMDD, String HHMMSS) {
        try {
            long day;
            day = Long.parseLong(YYMMDD);

            long time;
            time = Long.parseLong(HHMMSS);

            Calendar cal;
            cal = Calendar.getInstance();
            cal.setTime(new Date()); // initialization to avoid side effects

            cal.set(Calendar.YEAR, (int) (day / 10000 + 2000));
            cal.set(Calendar.MONTH, (int) ((day / 100) % 100) - 1);
            cal.set(Calendar.DAY_OF_MONTH, (int) (day % 100));
            cal.set(Calendar.HOUR, (int) (time / 10000));
            cal.set(Calendar.MINUTE, (int) ((time / 100) % 100));
            cal.set(Calendar.SECOND, (int) (time % 100));
            
            date = cal.getTime();
        } catch (NumberFormatException nfe) {
            System.err.println("NumberFormatException " + YYMMDD + " " + HHMMSS);
        }
    }
    
    public Date getDate() {
        return date;
    }
}
