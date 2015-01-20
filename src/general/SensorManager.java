/*
 * Copyright (c) 2014-2015 OwnTracks.de
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *      OwnTracks.de Christoph Krey <krey.christoph@gmail.com> - initial API and implementation and/or initial documentation
 */
package general;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Christoph Krey <krey.christoph@gmail.com>
 */
public class SensorManager {

    /**
     * Returns the singleton object for managing sensors. It instanciates or
     * returns the existing SensorManager object and starts the monitoring
     * activities.
     *
     * @return the <code>SensorManager</code> object.
     */
    public static SensorManager getInstance() {
        return PowerManagerHolder.INSTANCE;
    }

    private static double temperature(int voltage) {
        return (voltage / 10.0 / 0.184) - 273.0;
    }

    private final int NUMTEMPERATURES = 2;
    final private double temperatures[] = new double[NUMTEMPERATURES];

    private final Timer timer;
    private final TimerTask timerTask;

    /**
     * Don't let anyone else instantiate this class
     */
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

    /**
     * convert a temperature to a temperature string
     *
     * @param temperature
     * @return String representation of temperature with 2 decimals
     */
    public String temperatureString(double temperature) {
        return "" + (int) Math.floor(temperature)
                + "." + (int) (Math.floor(Math.abs(temperature) * 10)) % 10
                + (int) (Math.floor(Math.abs(temperature) * 100)) % 10;
    }

    private static class PowerManagerHolder {

        private static final SensorManager INSTANCE = new SensorManager();
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
