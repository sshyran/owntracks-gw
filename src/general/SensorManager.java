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
public class SensorManager {

    private final int NUMTEMPERATURES = 2;
    final private double temperatures[] = new double[NUMTEMPERATURES];

    private final Timer timer;
    private final TimerTask timerTask;

    private SensorManager() {
        for (int i = 0; i < NUMTEMPERATURES; i++) {
            temperatures[i] = temperature(32767);
        }

        timer = new Timer();
        timerTask = new TemperatureRequestTimerTask();
        int interval = Settings.getInstance().getSetting("temperatureInterval", 0);
        if (interval > 0) {
            timer.schedule(timerTask, 0, interval * 1000);
        }
    }

    public static SensorManager getInstance() {
        return PowerManagerHolder.INSTANCE;
    }

    private static class PowerManagerHolder {

        private static final SensorManager INSTANCE = new SensorManager();
    }

    private static double temperature(int voltage) {
        return (voltage / 10.0 / 0.184) - 273.0;
    }

    String temperatureString(double temperature) {
        return "" + (int) Math.floor(temperature)
                + "." + (int) (Math.floor(temperature * 10)) % 10
                + (int) (Math.floor(temperature * 100)) % 10;
    }

    class TemperatureRequestTimerTask extends TimerTask {

        public void run() {
            for (int i = 0; i < NUMTEMPERATURES; i++) {
                String response = ATManager.getInstance().executeCommandSynchron("AT^SRADC=" + i + "\r");
                String[] lines = StringFunc.split(response, "\r\n");
                int voltage = 32767;
                if (lines.length >= 2) {
                    final String SRADC = "^SRADC: ";
                    if (lines[1].startsWith(SRADC) && lines[1].length() > SRADC.length()) {
                        String[] values = StringFunc.split(lines[1].substring(SRADC.length()), ",");
                        if (values.length == 3) {
                            try {
                                voltage = Integer.parseInt(values[2]);
                            } catch (NumberFormatException nfe) {
                                //
                            }
                        }
                    }
                }
                if (Math.abs(temperatures[i] - temperature(voltage)) > Settings.getInstance().getSetting("dTemperature", 1) / 100.0) {
                    SocketGPRSThread.getInstance().put(
                            Settings.getInstance().getSetting("publish", "owntracks/gw/")
                            + Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI())
                            + "/temperature/" + i,
                            Settings.getInstance().getSetting("qos", 1),
                            Settings.getInstance().getSetting("retain", true),
                            temperatureString(temperature(voltage)).getBytes()
                    );
                    temperatures[i] = temperature(voltage);
                }
            }
        }
    }
}
