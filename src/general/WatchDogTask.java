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
public class WatchDogTask extends TimerTask {
    final private int delaySec = 60;
    final private int periodSec = 100;
    final private int holdSec = 2;

    final private Timer timer;
    
    public boolean gpsRunning;
    public boolean GPRSRunning;
    
    public WatchDogTask() {
        
        timer = new Timer();
        timer.scheduleAtFixedRate(this, delaySec * 1000, periodSec * 1000);
    }
    
    public void run() {
        if (Settings.getInstance().getSetting("timerDebug", false)) {
            System.out.println("WatchDogTask " + System.currentTimeMillis());
        }
        
        if (gpsRunning && GPRSRunning) {
            gpsRunning = false;
            GPRSRunning = false;

            SemAT.getInstance().getCoin(5);
            
            try {

                InfoStato.getInstance().writeATCommand("at^ssio=5,1\r");
                Thread.sleep(holdSec * 1000);
                InfoStato.getInstance().writeATCommand("at^ssio=5,0\r");

                InfoStato.getInstance().writeATCommand("at^ssio=5,1\r");
                Thread.sleep(holdSec * 1000);
                InfoStato.getInstance().writeATCommand("at^ssio=5,0\r");
            
            } catch (InterruptedException ie) {
            }
            
            SemAT.getInstance().putCoin();
        }
    }
}

