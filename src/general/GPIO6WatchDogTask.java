/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

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
        ATManager.getInstance().executeCommandSynchron("at^scpin=1,5,1,0\r");
        timer = new Timer();
        timer.scheduleAtFixedRate(this, 0, periodSec * 1000);
    }
    
    public void stop() {
        timer.cancel();
    }

    public void run() {
        if (Settings.getInstance().getSetting("timerDebug", false)) {
            System.out.println("GPIO6WatchDogTask " + System.currentTimeMillis()/1000);
        }

        if (gpsRunning && GPRSRunning) {
            if (Settings.getInstance().getSetting("timerDebug", false)) {
                System.out.println("GPIO6WatchDogTask all threads are running");
            }

            gpsRunning = false;
            GPRSRunning = false;

            if (gpio6On) {
                ATManager.getInstance().executeCommandSynchron("at^ssio=5,0\r");
            } else {
                ATManager.getInstance().executeCommandSynchron("at^ssio=5,1\r");
            }
            gpio6On = !gpio6On;

            if (Settings.getInstance().getSetting("timerDebug", false)) {
                System.out.println("WatchDogTask gpio6 " + gpio6On);
            }
        }
    }
}