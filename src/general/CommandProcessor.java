package general;

import com.cinterion.io.ATCommandFailedException;
import com.cinterion.io.ATCommand;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.io.UnsupportedEncodingException;
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
    private final String log = "log";
    private final String dump = "dump";

    private final String[] unauthorizedCommands = {login, gps, state, close};
    private final String[] authorizedCommands = {set, reboot, reconnect, dump, log, logout};

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
                } catch (MqttException e) {
                    e.printStackTrace();
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

        } else if (command.equalsIgnoreCase(reconnect)) {
            message = message.concat("reconnecting");
            InfoStato.getInstance().setCloseGPRS(true);
            Mailboxes.getInstance(3).write(rebootTrack);
            return true;

        } else if (command.equalsIgnoreCase(set)) {
            return setCommand(parameters);

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
        message = message.concat(";BEARER:" + InfoStato.getInstance().getGPRSBearer());
        message = message.concat(";CREG:" + InfoStato.getInstance().getCREG());
        message = message.concat(";CGREG:" + InfoStato.getInstance().getCGREG());
        message = message.concat(";ERR:" + InfoStato.getInstance().getERROR());
        message = message.concat(";BATT:" + InfoStato.getInstance().getBatteryVoltage());
        message = message.concat(";IN:" + InfoStato.getInstance().getTrkIN());
        message = message.concat(";OUT:" + InfoStato.getInstance().getTrkOUT());
        message = message.concat(";t1:" + InfoStato.getInstance().getTask1Timer());
        message = message.concat(";t2:" + InfoStato.getInstance().getTask2Timer());
        message = message.concat(";t3:" + InfoStato.getInstance().getTask3Timer());
        message = message.concat(";uFW:" + InfoStato.getInstance().getReleaseMicro());
        message = message.concat(";SW:" + Settings.getInstance().getSetting("MIDlet-Version", "unknown"));
        message = message.concat(";EG5:" + InfoStato.getInstance().getREV());
        message = message.concat(";IMEI: " + InfoStato.getInstance().getIMEI());
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
//#ifdef IMPORT
//#                                         } // @CODA --> Read queue						
//#                                         else if (comCSD.indexOf(logQUEUE) >= 0) {
//#                                             for (int indice = 0; indice < 99; indice++) {
//#                                                 dataOut.write(InfoStato.getInstance().getRecord(indice).getBytes());
//#                                             }
//#endif 
}
