/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import java.util.Timer;
import java.util.TimerTask;
import com.cinterion.misc.Watchdog;

/**
 *
 * @author christoph
 */
public class UserwareWatchDogTask extends TimerTask {

    final private int periodSec = 29;
    final private int timeoutSec = 90;

    private Timer timer = null;

    public boolean gpsRunning;
    public boolean GPRSRunning;

    public UserwareWatchDogTask() {
        start();
    }

    final public void start() {
        Watchdog.start(timeoutSec);
        timer = new Timer();
        timer.scheduleAtFixedRate(this, 0, periodSec * 1000);

    }
    
    public void stop() {
        timer.cancel();
        Watchdog.start(0);
    }

    public void run() {
        if (Settings.getInstance().getSetting("timerDebug", false)) {
            System.out.println("UserwareWatchDogTask " + System.currentTimeMillis()/1000);
        }

        if (gpsRunning && GPRSRunning) {
            if (Settings.getInstance().getSetting("timerDebug", false)) {
                System.out.println("UserwareWatchDogTask will kick watchdog");
            }

            gpsRunning = false;
            GPRSRunning = false;

            Watchdog.kick();
        }
    }
}