package general;

import com.cinterion.io.ATCommand;
import com.cinterion.io.ATCommandFailedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import javax.microedition.io.Connector;
import javax.microedition.io.SecureConnection;
import javax.microedition.io.SecurityInfo;
import javax.microedition.io.SocketConnection;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.pki.CertificateException;

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
    private final String upgrade = "upgrade";
    private final String close = "close";
    private final String reconnect = "reconnect";
    private final String secure = "secure";
    private final String magic = "magic";
    private final String exec = "exec";
    private final String destroy = "destroy";
    private final String log = "log";
    private final String dump = "dump";

    private final String[] authorizedCommands = {set, reboot, reconnect, dump, log, logout, destroy, exec, upgrade, close};

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
                message = "";
                if (
                        !isInStringArray(words[0], authorizedCommands)
                        || settings.getSetting("loginTimeout", 30) == 0
                        || authorizedSince + settings.getSetting("loginTimeout", 30) > new Date().getTime() / 1000
                    ) {
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
                        authorizedSince = 0;
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
                message = "no cmd given";
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
            
            String human = locationManager.getLastHumanString();
            if (human != null) {
                message = message.concat(human);
            } else {
                message = message.concat("no location available");
            }
            
            String[] fields = StringSplitter.split(
                    settings.getSetting("fields", "course,speed,altitude,distance,battery"), ",");
            String json = locationManager.getlastJSONString(fields);
            if (json != null) {
                SocketGPRSThread.getInstance().put(
                    settings.getSetting("publish", "owntracks/gw/")
                    + settings.getSetting("clientID", InfoStato.getInstance().getIMEI()),
                    settings.getSetting("qos", 1),
                    settings.getSetting("retain", true),
                    json.getBytes());
                return true;
            } else {
                return false;
            }

        } else if (command.equalsIgnoreCase(reboot)) {
            closeCommand(atThread);
            if (atThread) {
                BatteryManager.getInstance().reboot();
            } else {
                BatteryManager.getInstance().reboot();
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
            SocketGPRSThread.getInstance().close();
            SocketGPRSThread.getInstance().open();
            return true;

        } else if (command.equalsIgnoreCase(set)) {
            return setCommand(parameters);

        } else if (command.equalsIgnoreCase(upgrade)) {
            return upgradeCommand(parameters);

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
        message = "NUMSAT:" + InfoStato.getInstance().getNumSat();
        message = message.concat(";BEARER:" + Bearer.getInstance().getBearerState());
        message = message.concat(";CREG:" + InfoStato.getInstance().getCREG());
        message = message.concat(";CGREG:" + InfoStato.getInstance().getCGREG());
        message = message.concat(";ERR:" + InfoStato.getInstance().getERROR());
        message = message.concat(";BATT:" + BatteryManager.getInstance().getBatteryVoltage());
        message = message.concat(";EXTV:" + BatteryManager.getInstance().getExternalVoltage());
        message = message.concat(";t2:" + InfoStato.getInstance().getTask2Timer());
        message = message.concat(";gpsQ:" + SocketGPRSThread.getInstance().qSize());
        message = message.concat(";sgt:" + (SocketGPRSThread.getInstance().isSending() ? "1" : "0"));
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

    String replaceString(String originalString, String oldString, String newString) {
        String intermediateString = originalString;
        //System.out.println("replaceString " + originalString + " " + oldString + " " + newString);
        int indexOldString;

        if (newString.indexOf(oldString) >= 0) {
            System.out.println("replaceString recursion " + originalString + " " + oldString + " " + newString);
            return originalString;
        }
        do {
            //System.out.println("intermediateString " + intermediateString);
            indexOldString = intermediateString.indexOf(oldString);
            //System.out.println("indexOldString " + indexOldString);
            if (indexOldString >= 0) {
                String workString;
                if (indexOldString > 0) {
                    workString = intermediateString.substring(0, indexOldString);
                } else {
                    workString = "";
                }
                //System.out.println("anfang " + workString);
                workString = workString.concat(newString);
                //System.out.println("mitte " + workString);
                if (intermediateString.length() > indexOldString + oldString.length()) {
                    workString = workString.concat(intermediateString.substring(indexOldString + oldString.length()));
                }
                //System.out.println("abschluss " + workString);
                intermediateString = workString;
            }
        } while (indexOldString >= 0);
        return intermediateString;
    }
    
    boolean upgradeCommand(String[] parameters) {
        if (parameters.length == 1) {
            String clientID = Settings.getInstance().getSetting("clientID", InfoStato.getInstance().getIMEI());
            String otapURI = Settings.getInstance().getSetting("otapURI", "");
            String notifyURI = Settings.getInstance().getSetting("notifyURI", "");
            otapURI = replaceString(otapURI, "@", clientID);
            notifyURI = replaceString(notifyURI, "@", clientID);

            String apn = Settings.getInstance().getSetting("apn", "internet");
            String otapUser = Settings.getInstance().getSetting("otapUser", "");
            String otapPassword = Settings.getInstance().getSetting("otapPassword", "");
            
            String otap =
                    "AT^SJOTAP=,"
                    + otapURI
                    + ",a:/app,"
                    + otapUser
                    + ","
                    + otapPassword
                    + ",gprs,"
                    + apn
                    + ",,,8.8.8.8,"
                    + notifyURI
                    + "\r";

            if (Settings.getInstance().getSetting("debug", false)) {
                System.out.println("upgrade " + otap);
            }

            ATManager.getInstance().executeCommand(otap);
            ATManager.getInstance().executeCommand("AT^SJOTAP\r");
            message = "upgrade " + otap;
            return true;
        } else {
            message = "usage " + upgrade;
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
            ATManager.getInstance().executeCommand(parameters[1] + "\r");
            message = "To see response set gsmDebug=1";
            return true;
        } else {
            message = "usage " + exec + " at-cmd";
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
