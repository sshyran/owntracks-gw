/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package general;

import java.util.Timer;
import java.util.TimerTask;

import choral.io.PowerManager;
import java.io.IOException;

/**
 *
 * @author christoph
 */
public class BatteryManager {
    
    private final double LowVoltageThreshold = 3.599;
    private final int BatteryRequestLoop = 60;
    
    private double batteryVoltage;
    private final PowerManager powerManager;
    private final Timer timer;
    private final TimerTask timerTask;
    
    private BatteryManager() {
        batteryVoltage = 0.0;
        powerManager = new PowerManager();
        timer = new Timer();
        timerTask = new BatteryRequestTimerTask();
        timer.schedule(timerTask, 0, BatteryRequestLoop * 1000);
    }
    
    public static BatteryManager getInstance() {
        return PowerManagerHolder.INSTANCE;
    }
    
    private static class PowerManagerHolder {

        private static final BatteryManager INSTANCE = new BatteryManager();
    }
    
    public boolean isBatteryVoltageLow() {
        return batteryVoltage < LowVoltageThreshold;
    }
    
    public double getBatteryVoltage() {
        return batteryVoltage;
    }
    
    private String voltageString(double voltage) {
        return "" + (int)Math.floor(voltage) + "." + (int)(Math.floor(voltage * 10)) % 10 + "V";
    }
    
    public String getBatteryVoltageString() {
        return voltageString(batteryVoltage);
    }
    
    public double getExternalVoltage() {
        try {
            double voltage = powerManager.getDoubleVIN();
            {
                long longVoltage = (long)(voltage * 1000.0);
                voltage = longVoltage / 1000.0;
            }
            return voltage;
        } catch (IOException ioe) {
            System.err.println("IOException powerManager.getDoubleVIN");
            return 0.0;
        }
    }
    
    public String getExternalVoltageString() {
        return voltageString(getExternalVoltage());
    }
    
    public void setBatteryVoltage(double voltage) {
        {
            long longVoltage = (long)(voltage * 1000.0);
            voltage = longVoltage / 1000.0;
        }
        if (voltage < LowVoltageThreshold) {
            eventLowBattery();
        }
        batteryVoltage = voltage;
    }
    
    public void eventLowBattery() {
        AppMain.getInstance().underVoltageEvent(getBatteryVoltageString());
    }
    
    public boolean reboot() {
        try {
            return (powerManager.setReboot() == 1);
        } catch (IOException ioe) {
            System.err.println("IOException powerManager.setReboot");
            return false;
        }
    }

    public boolean lowPowerMode() {
        try {
            return (powerManager.setLowPwrMode() == 1);
        } catch (IOException ioe) {
            System.err.println("IOException powerManager.setLowPowerMode");
            return false;
        }
    }

    class BatteryRequestTimerTask extends TimerTask {
        public void run() {
            String response = ATManager.getInstance().executeCommandSynchron("AT^SBV\r");
            final String SBV = "^SBV: ";
            try {
                int start = response.indexOf(SBV) + SBV.length();
                int end = response.substring(start).indexOf("\r");
                double voltage = Double.parseDouble(response.substring(
                        start, start + end));
                setBatteryVoltage(voltage / 1000);
            } catch (NumberFormatException nfe) {
                System.err.println("NumberFormatException " + response);
            }
        }
    }
}
