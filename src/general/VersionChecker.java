/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.HttpsConnection;

/**
 *
 * @author christoph
 */
public class VersionChecker {
    private VersionChecker() {
    }

    public static VersionChecker getInstance() {
        return versionCheckerHolder.INSTANCE;
    }

    private static class versionCheckerHolder {

        private static final VersionChecker INSTANCE = new VersionChecker();
    }

    public boolean mismatch() {
        long lastCheck = Settings.getInstance().getSetting("lastVersionCheck", 0L) * 1000L;
        SLog.log(SLog.Debug, "VersionChecker", "mismatch " + (lastCheck / 1000L));

        if (lastCheck > 0
                && lastCheck + Settings.getInstance().getSetting("versionInterval", 3 * 3600L) * 1000L
                > System.currentTimeMillis()) {
            return false;
        }

        String uri = Settings.getInstance().getSetting("versionURI", null);
        if (uri == null) {
            return false;
        }

        HttpConnection c = null;
        InputStream is = null;
        OutputStream os = null;

        boolean checkResult = false;
        try {
            c = (HttpConnection) Connector.open(uri);
            c.setRequestMethod("POST");
            c.setRequestProperty("user-agent", "GW/" + MicroManager.getInstance().getIMEI());

            os = c.openOutputStream();
            os.write(AppMain.appMain.getAppProperty("MIDlet-Version").getBytes());

            is = c.openDataInputStream();

            int response = c.getResponseCode();
            SLog.log(SLog.Debug, "VersionChecker", "getResponseCode " + response);
            if (response == HttpConnection.HTTP_OK) {
                int ch;
                while ((ch = is.read()) != -1) {
                    SLog.log(SLog.Debug, "VersionChecker", "read " + ch);
                    if (ch == '1') {
                        checkResult = true;
                    }
                }
            }
        } catch (IOException ioe) {
            SLog.log(SLog.Warning, "VersionChecker", "IOException " + ioe);
        } catch (IllegalArgumentException iae) {
            SLog.log(SLog.Warning, "VersionChecker", "IllegalArgumentExeption " + uri);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ioe) {
                    //
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    //
                }
            }
            if (c != null) {
                try {
                    c.close();
                } catch (IOException ioe) {
                    //
                }
            }
        }
        SLog.log(SLog.Debug, "VersionChecker", "returning " + checkResult);
        lastCheck = System.currentTimeMillis();
        Settings.getInstance().setSetting("lastVersionCheck", Long.toString(lastCheck / 1000L));
        return checkResult;
    }
}
