/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author christoph
 */
public class GPIO6WatchDogTask extends TimerTask {

    final private int periodSec = 59;

    private Timer timer = null;

    private boolean gpio6On = false;

    public boolean gpsRunning;
    public boolean GPRSRunning;

    public GPIO6WatchDogTask() {
        start();
    }

    final public void start() {
        timer = new Timer();
        timer.scheduleAtFixedRate(this, 0, periodSec * 1000);
    }

    public void stop() {
        timer.cancel();
    }

    public void run() {
        SLog.log(SLog.Debug, "GPIO6WatchDogTask", DateFormatter.isoString(new Date()));

        if (gpsRunning && GPRSRunning) {
            SLog.log(SLog.Debug, "GPIO6WatchDogTask", "all threads are running");

            gpsRunning = false;
            GPRSRunning = false;

            GPIOManager.getInstance().setGPIO(5, gpio6On);
            
            gpio6On = !gpio6On;

            SLog.log(SLog.Debug, "GPIO6WatchDogTask", "gpio6 " + gpio6On);
        }
    }
}
