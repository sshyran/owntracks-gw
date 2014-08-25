/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import choral.io.InfoMicro;
import choral.io.MovListener;
import choral.io.MovSens;
import java.io.IOException;

/**
 *
 * @author christoph krey
 */
public class MicroManager implements MovListener {

    private String ati = "unknown";
    private String imei = "unknown";
    private String release = "unknown";
    private String bootRelease = "unknown";
    private String javaRelease = "unknown";

    final private MovSens movSens;
    private boolean moved = false;

    private MicroManager() {
        String response;
        String lines[];
        response = ATManager.getInstance().executeCommandSynchron("ATI\r");

        lines = StringSplitter.split(response, "\r\n");
        ati = lines[1];
        for (int i = 2; i < lines.length - 3; i++) {
            ati = ati + "," + lines[i];
        }

        response = ATManager.getInstance().executeCommandSynchron("AT+CGSN\r");
        lines = StringSplitter.split(response, "\r\n");
        imei = lines[1];

        InfoMicro infoMicro = new InfoMicro();
        try {
            release = infoMicro.getRelease();
            bootRelease = infoMicro.getBootRelease();
            javaRelease = infoMicro.getJavaRelease();
        } catch (IOException ie) {
            //
        }

        SocketGPRSThread.getInstance().put(
                Settings.getInstance().getSetting("publish", "owntracks/gw/")
                + Settings.getInstance().getSetting("clientID", imei)
                + "/hw",
                Settings.getInstance().getSetting("qos", 1),
                Settings.getInstance().getSetting("retain", true),
                ati.getBytes()
        );
        SocketGPRSThread.getInstance().put(
                Settings.getInstance().getSetting("publish", "owntracks/gw/")
                + Settings.getInstance().getSetting("clientID", imei)
                + "/hw/imei",
                Settings.getInstance().getSetting("qos", 1),
                Settings.getInstance().getSetting("retain", true),
                imei.getBytes()
        );
        SocketGPRSThread.getInstance().put(
                Settings.getInstance().getSetting("publish", "owntracks/gw/")
                + Settings.getInstance().getSetting("clientID", imei)
                + "/sw/gw",
                Settings.getInstance().getSetting("qos", 1),
                Settings.getInstance().getSetting("retain", true),
                (release + "," + bootRelease + "," + javaRelease).getBytes()
        );

        movSens = new MovSens();
        movSens.addMovListener(this);

        if (Settings.getInstance().getSetting("motion", 4) > 0) {
            try {
                movSens.setMovSens(Settings.getInstance().getSetting("motion", 4));
                movSens.movSensOn();
            } catch (IOException ioe) {
                SLog.log(SLog.Error, "MicroManager", "IOException movSensOn");
            }
        } else {
            try {
                movSens.movSensOff();
            } catch (IOException ioe) {
                SLog.log(SLog.Error, "MicroManager", "IOException movSensOff");
            }
        }
    }

    public void movSensEvent(String event) {
        SLog.log(SLog.Debug, "MicroManager", "movSensEvent " + event);

        if (event.equalsIgnoreCase("^MOVE: 0")) {
            //moved = false;
        } else if (event.equalsIgnoreCase("^MOVE: 1")) {
            moved = true;
        }
    }

    public static MicroManager getInstance() {
        return MicroManagerHolder.INSTANCE;
    }

    private static class MicroManagerHolder {

        private static final MicroManager INSTANCE = new MicroManager();
    }

    public String getInfo() {
        return ati;
    }

    public String getIMEI() {
        return imei;
    }

    public String getRelease() {
        return release;
    }

    public String getBootRelease() {
        return bootRelease;
    }

    public String getJavaRelease() {
        return javaRelease;
    }

    public boolean hasMoved() {
        return moved;
    }
}
