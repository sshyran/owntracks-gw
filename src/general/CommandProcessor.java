package general;

import com.cinterion.imp.io.tls.SSLStreamConnection;
import com.cinterion.io.ATCommand;
import com.cinterion.io.ATCommandFailedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;
import javax.microedition.io.Connector;
import javax.microedition.io.SecureConnection;
import javax.microedition.io.SecurityInfo;
import javax.microedition.io.SocketConnection;
import javax.microedition.io.StreamConnection;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.pki.CertificateException;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 *
 * @author christoph krey
 */
public class CommandProcessor implements GlobCost {

    private final String login = "login";
    private final String logout = "logout";
    private final String gps = "gps";
    private final String state = "state";
    private final String set = "set";
    private final String reboot = "reboot";
    private final String close = "close";
    private final String reconnect = "reconnect";
    private final String secure = "secure";
    private final String magic = "magic";
    private final String exec = "exec";
    private final String destroy = "destroy";
    private final String log = "log";
    private final String dump = "dump";

    private final String[] unauthorizedCommands = {login, gps, state, close, secure, magic, exec};
    private final String[] authorizedCommands = {set, reboot, reconnect, dump, log, logout, destroy};

    private final String CRLF = "\r\n";

    private long authorizedSince = 0;
    public String message = null;

    private boolean isInStringArray(String string, String[] stringArray) {
        for (int i = 0; i < stringArray.length; i++) {
            if (string.equalsIgnoreCase(stringArray[i])) {
                return true;
            }
        }
        return false;
    }

    private CommandProcessor() {
    }

    public static CommandProcessor getInstance() {
        return CommandProcessorHolder.INSTANCE;
    }

    private static class CommandProcessorHolder {

        private static final CommandProcessor INSTANCE = new CommandProcessor();
    }

    public synchronized boolean execute(String commandLine, boolean atThread) {

        if (Settings.getInstance().getSetting("debug", false)) {
            System.out.println("execute " + (commandLine != null ? commandLine : "<null>"));
        }
        if (commandLine != null && commandLine.length() > 0) {
            String[] words = StringSplitter.split(commandLine, " ");
            if (words.length >= 1) {
                Settings settings = Settings.getInstance();
                message = "command: \"" + commandLine + "\"" + CRLF;
                if ((isInStringArray(words[0], unauthorizedCommands))
                        || ((isInStringArray(words[0], authorizedCommands))
                        && ((authorizedSince + settings.getSetting("loginTimeout", 30) > new Date().getTime() / 1000)
                        || (settings.getSetting("loginTimeout", 30) == 0)))) {
                    if (words[0].equals("login")) {
                        if ((words.length == 2)
                                && (words[1].equals(settings.getSetting("secret", "1234567890")))) {
                            authorizedSince = new Date().getTime() / 1000;
                            message = message.concat("login accepted");
                            return true;
                        } else {
                            message = message.concat("incorrect login");
                            return false;
                        }
                    } else if (words[0].equals("logout")) {
                        message = message.concat("logged out");
                        return true;
                    } else {
                        return perform(words[0], words, atThread);
                    }
                } else {
                    message = message.concat("illegal command");
                    return false;
                }
            } else {
                message = "no command given";
                return false;
            }
        } else {
            message = "no commandLine given";
            return false;
        }
    }

    private boolean perform(String command, String[] parameters, boolean atThread) {
        Settings settings = Settings.getInstance();
        if (command.equalsIgnoreCase(gps)) {
            LocationManager locationManager = LocationManager.getInstance();
            String[] fields = StringSplitter.split(
                    settings.getSetting("fields", "course,speed,altitude,distance,battery"), ",");
            String json = locationManager.getlastJSONString(fields);
            if (json == null) {
                message = message.concat("no location available");
                return false;
            } else {
                message = message.concat(json);
                try {
                    MQTTHandler.getInstance().publish(settings.getSetting("publish", "owntracks/gw/")
                            + settings.getSetting("clientID", InfoStato.getInstance().getIMEI()),
                            settings.getSetting("qos", 1),
                            settings.getSetting("retain", true),
                            json.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException uee) {
                    uee.printStackTrace();
                }
                return true;
            }

        } else if (command.equalsIgnoreCase(reboot)) {
            closeCommand(atThread);
            if (atThread) {
                try {
                    ATCommand atc = new ATCommand(false, false, false, false, false, false);
                    atc.send("AT+CFUN=1,1\r");
                } catch (ATCommandFailedException atcfe) {
                    atcfe.printStackTrace();
                } catch (IllegalStateException ise) {
                    ise.printStackTrace();
                }
            } else {
                SemAT.getInstance().getCoin(5);
                InfoStato.getInstance().writeATCommand("AT+CFUN=1,1\r");
                SemAT.getInstance().putCoin();
            }
            message = message.concat("rebooting");
            return true;

        } else if (command.equalsIgnoreCase(close)) {
            closeCommand(atThread);
            message = message.concat("closing");
            return true;

        } else if (command.equalsIgnoreCase(destroy)) {
            try {
                AppMain.getInstance().destroyApp(true);
            } catch (MIDletStateChangeException ex) {
                System.err.println("MidletStateChangeException");
            }
            message = message.concat("destroying app");
            return true;

        } else if (command.equalsIgnoreCase(reconnect)) {
            message = message.concat("reconnecting");
            InfoStato.getInstance().setCloseGPRS(true);
            Mailboxes.getInstance(3).write(rebootTrack);
            return true;

        } else if (command.equalsIgnoreCase(set)) {
            return setCommand(parameters);

        } else if (command.equalsIgnoreCase(secure)) {
            return secureCommand(parameters);

        } else if (command.equalsIgnoreCase(exec)) {
            return execCommand(parameters);

        } else if (command.equalsIgnoreCase(magic)) {
            return magicCommand(parameters);

        } else if (command.equalsIgnoreCase(dump)) {
            String combined;

            stateCommand(parameters);
            combined = message;
            setCommand(parameters);
            combined = combined.concat(message);

            message = combined;
            return true;

        } else if (command.equalsIgnoreCase(state)) {
            return stateCommand(parameters);
        } else if (command.equalsIgnoreCase(log)) {
            return logCommand(parameters);
        } else {
            message = message.concat("not implemented");
            return false;
        }
    }

    boolean setCommand(String[] parameters) {
        Settings settings = Settings.getInstance();
        if (parameters.length == 1) {
            for (Enumeration e = settings.keys(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                message = message.concat(key + "=" + settings.getSetting(key, null) + CRLF);
            }
            return true;
        } else if (parameters.length == 2) {
            String key = null;
            String value = null;
            int equal = parameters[1].indexOf('=');
            if (equal != -1) {
                value = parameters[1].substring(equal + 1);
                if (equal >= 1) {
                    key = parameters[1].substring(0, equal);
                }
            } else {
                if (parameters[1].length() > 0) {
                    key = parameters[1];
                }
            }
            if (key != null) {
                if (value != null) {
                    settings.setSetting(key, value);
                }
                message = message.concat(key + "=" + settings.getSetting(key, ""));
                return true;
            } else {
                return false;
            }
        } else {
            message = message.concat("usage " + set + " [<key>[=[<value>]]]");
            return false;
        }
    }

    boolean stateCommand(String[] parameters) {
        message = "CSQ:" + InfoStato.getInstance().getCSQ() + "," + InfoStato.getInstance().getNumSat();
        message = message.concat(";BEARER:" + Bearer.getInstance().getBearerState());
        message = message.concat(";CREG:" + InfoStato.getInstance().getCREG());
        message = message.concat(";CGREG:" + InfoStato.getInstance().getCGREG());
        message = message.concat(";ERR:" + InfoStato.getInstance().getERROR());
        message = message.concat(";BATT:" + InfoStato.getInstance().getBatteryVoltage());
        message = message.concat(";t1:" + InfoStato.getInstance().getTask1Timer());
        message = message.concat(";t2:" + InfoStato.getInstance().getTask2Timer());
        message = message.concat(";gpsQ:" + InfoStato.getInstance().gpsQ.size());
        message = message.concat(";sgt:" + (AppMain.getInstance().socketGPRSThread.isSending() ? "1" : "0"));
        message = message.concat(";uFW:" + InfoStato.getInstance().getReleaseMicro());
        message = message.concat(";SW:" + Settings.getInstance().getSetting("MIDlet-Version", "unknown"));
        message = message.concat(";EG5:" + InfoStato.getInstance().getREV());
        message = message.concat(";IMEI:" + InfoStato.getInstance().getIMEI());
        return true;
    }

    boolean logCommand(String[] parameters) {
        if (parameters.length == 1) {
            message = LogError.readCurrentLog().toString();
            return true;
        } else if (parameters.length == 2) {
            if (parameters[1].equalsIgnoreCase("old")) {
                message = LogError.readOldLog().toString();
                return true;
            } else if (parameters[1].equalsIgnoreCase("del")) {
                LogError.deleteLog();
                message = "deleted";
                return true;
            } else {
                message = "usage " + log + "[old|del]";
                return false;
            }
        } else {
            message = "usage " + log + "[old|del]";
            return false;
        }
    }

    void closeCommand(boolean atThread) {
        if (atThread) {
            try {
                ATCommand atc = new ATCommand(true, false, false, false, false, false);
                atc.breakConnection();
            } catch (ATCommandFailedException atcfe) {
                atcfe.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (IllegalStateException ise) {
                ise.printStackTrace();
            }
        }
    }

    boolean secureCommand(String[] parameters) {
        if (parameters.length == 3) {
            try {
                SecureConnection secc;
                InputStream is;
                OutputStream os;

                message = "Opening a SecureConnection to " + parameters[1] + CRLF;
                secc = (SecureConnection) Connector.open(parameters[1]);
                SecurityInfo info = secc.getSecurityInfo();
                message = message.concat("ProtocolName " + info.getProtocolName() + CRLF);
                message = message.concat("ProtocolVersion " + info.getProtocolVersion() + CRLF);
                message = message.concat("CipherSuite " + info.getCipherSuite() + CRLF);

                message = message.concat("Issuer " + info.getServerCertificate().getIssuer() + CRLF);
                message = message.concat("Serial " + info.getServerCertificate().getSerialNumber() + CRLF);
                message = message.concat("SigAlgName " + info.getServerCertificate().getSigAlgName() + CRLF);
                message = message.concat("Subject " + info.getServerCertificate().getSubject() + CRLF);
                message = message.concat("Type " + info.getServerCertificate().getType() + CRLF);
                message = message.concat("Version " + info.getServerCertificate().getVersion() + CRLF);

                secc.setSocketOption(SocketConnection.LINGER, 5);

                is = secc.openInputStream();
                os = secc.openOutputStream();
                String request = parameters[2].replace('_', ' ');
                message = message.concat("Request: " + request + CRLF);
                os.write((request + "\r\n\r\n").getBytes());

                int ch = 0;
                StringBuffer buffer = new StringBuffer();
                while (ch != -1) {
                    buffer.append((char) is.read());
                    if (buffer.length() > 200) {
                        break;
                    }
                }

                message = message.concat("Response " + CRLF + buffer.toString() + CRLF);

                is.close();
                os.close();
                secc.close();

            } catch (IllegalArgumentException iae) {
                message = message.concat("IllegalArgumentException " + iae.getMessage());
            } catch (CertificateException ce) {
                message = message.concat("CertificateException " + ce.getMessage());
            } catch (IOException ex) {
                message = message.concat("IOException " + ex.getMessage());
            }
            return true;

        } else {
            message = "usage " + secure + " url message";
            return false;
        }
    }
    boolean execCommand(String[] parameters) {
        if (parameters.length == 2) {
            SemAT.getInstance().getCoin(5);
            InfoStato.getInstance().writeATCommand(parameters[1] + "\r");
            SemAT.getInstance().putCoin();
            message = "To see response set gsmDebug=1";
            return true;
        } else {
            message = "usage " + exec + " at-command";
            return false;
        }
    }
    boolean magicCommand(String[] parameters) {
        if (parameters.length == 2) {
            try {
                Date date = new Date(Long.parseLong(parameters[1]));
                message = date.toString();
            } catch (NumberFormatException nfe) {
                message = message.concat("IllegalArgumentException " + parameters[1]);
                return false;
            }
            return true;
        } else if (parameters.length == 3) {
            DateParser dp = new DateParser(parameters[1], parameters[2]);
            Date date = dp.getDate();
            if (date != null) {
                message = "tst " + date.getTime() + CRLF;
                message = message.concat("toString " + date.toString()) + CRLF;
                return true;
            } else {
                message = "Parsing error";
                return false;
            }
        } else {
            message = "usage " + magic + " timestamp | YYMMDD HHMMSS";
            return false;
        }
    }
}
