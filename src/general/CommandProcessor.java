package general;

import com.cinterion.io.ATCommand;
import com.cinterion.io.ATCommandFailedException;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 *
 * @author christoph krey
 */
public class CommandProcessor {

    private final String login = "login";
    private final String logout = "logout";
    private final String gps = "gps";
    private final String state = "state";
    private final String set = "set";
    private final String reboot = "reboot";
    private final String upgrade = "upgrade";
    private final String reconnect = "reconnect";
    private final String exec = "exec";
    private final String destroy = "destroy";
    private final String log = "log";
    private final String dump = "dump";

    private final String[] authorizedCommands = {set, reboot, reconnect, dump, log, logout, destroy, exec, upgrade};

    private final String CRLF = "\r\n";

    private long authorizedSince = 0;
    public String message = null;

    private CommandProcessor() {
    }

    public static CommandProcessor getInstance() {
        return CommandProcessorHolder.INSTANCE;
    }

    private static class CommandProcessorHolder {

        private static final CommandProcessor INSTANCE = new CommandProcessor();
    }

    public synchronized boolean execute(String commandLine) {

        SLog.log(SLog.Debug, "CommandProcessor", "execute " + (commandLine != null ? commandLine : "<null>"));
        if (commandLine != null && commandLine.length() > 0) {
            String[] words = StringSplitter.split(commandLine, " ");
            if (words.length >= 1) {
                Settings settings = Settings.getInstance();
                message = "";
                if (!StringSplitter.isInStringArray(words[0], authorizedCommands)
                        || settings.getSetting("loginTimeout", 30) == 0
                        || authorizedSince + settings.getSetting("loginTimeout", 30) > new Date().getTime() / 1000) {
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
                        return perform(words[0], words);
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

    private boolean perform(String command, String[] parameters) {
        Settings settings = Settings.getInstance();
        if (command.equalsIgnoreCase(gps)) {
            LocationManager locationManager = LocationManager.getInstance();

            String human = locationManager.getLastHumanString();
            if (human != null) {
                message = message.concat(human);
            } else {
                message = message.concat("no location available");
            }

            String json = locationManager.getlastJSONString("m");
            if (json != null) {
                SocketGPRSThread.getInstance().put(
                        settings.getSetting("publish", "owntracks/gw/")
                        + settings.getSetting("clientID", MicroManager.getInstance().getIMEI()),
                        settings.getSetting("qos", 1),
                        settings.getSetting("retain", true),
                        json.getBytes());
                return true;
            } else {
                return false;
            }

        } else if (command.equalsIgnoreCase(reboot)) {
            BatteryManager.getInstance().reboot();
            message = message.concat("rebooting");
            return true;

        } else if (command.equalsIgnoreCase(destroy)) {
            try {
                AppMain.getInstance().destroyApp(true);
            } catch (MIDletStateChangeException ex) {
                SLog.log(SLog.Error, "CommandProcessor", "MidletStateChangeException");
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

        } else if (command.equalsIgnoreCase(exec)) {
            return execCommand(parameters);

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
        message = "NUMSAT:" + LocationManager.getInstance().getNumSat();
        message = message.concat(";BEARER:" + Bearer.getInstance().getBearerState());
        message = message.concat(";CREG:" + SocketGPRSThread.getInstance().creg);
        message = message.concat(";CGREG:" + SocketGPRSThread.getInstance().cgreg);
        message = message.concat(";BATT:" + BatteryManager.getInstance().getBatteryVoltageString());
        message = message.concat(";EXTV:" + BatteryManager.getInstance().getExternalVoltageString());
        message = message.concat(";Q:" + SocketGPRSThread.getInstance().qSize());
        message = message.concat(";CONN:" + (SocketGPRSThread.getInstance().isConnected() ? 1 : 0));
        message = message.concat(";NETW:" + (SocketGPRSThread.getInstance().isNetwork() ? 1 : 0));
        message = message.concat(";OPER:" + SocketGPRSThread.getInstance().getOperator());
        message = message.concat(";WAKEUP:" + AppMain.getInstance().wakeupMode);
        message = message.concat(";uFW:" + MicroManager.getInstance().getRelease()
                + "," + MicroManager.getInstance().getBootRelease()
                + "," + MicroManager.getInstance().getJavaRelease());
        message = message.concat(";SW:" + AppMain.getInstance().getAppProperty("MIDlet-Version"));
        message = message.concat(";EG5:" + MicroManager.getInstance().getInfo());
        message = message.concat(";IMEI:" + MicroManager.getInstance().getIMEI());
        message = message.concat(";DATE:" + DateFormatter.isoString(new Date()));
        return true;
    }

    boolean logCommand(String[] parameters) {
        if (parameters.length == 1) {
            message = SLog.readCurrentLog().toString();
            return true;
        } else if (parameters.length == 2) {
            if (parameters[1].equalsIgnoreCase("old")) {
                message = SLog.readOldLog().toString();
                return true;
            } else if (parameters[1].equalsIgnoreCase("del")) {
                SLog.deleteLog();
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
        int indexOldString;

        if (newString.indexOf(oldString) >= 0) {
            SLog.log(SLog.Error, "CommandProcessor", "replaceString recursion " + originalString + " " + oldString + " " + newString);
            return originalString;
        }
        do {
            indexOldString = intermediateString.indexOf(oldString);
            if (indexOldString >= 0) {
                String workString;
                if (indexOldString > 0) {
                    workString = intermediateString.substring(0, indexOldString);
                } else {
                    workString = "";
                }
                workString = workString.concat(newString);
                if (intermediateString.length() > indexOldString + oldString.length()) {
                    workString = workString.concat(intermediateString.substring(indexOldString + oldString.length()));
                }
                intermediateString = workString;
            }
        } while (indexOldString >= 0);
        return intermediateString;
    }

    boolean upgradeCommand(String[] parameters) {
        if (parameters.length == 1) {
            String clientID = Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI());
            String otapURI = Settings.getInstance().getSetting("otapURI", "");
            String notifyURI = Settings.getInstance().getSetting("notifyURI", "");
            otapURI = replaceString(otapURI, "@", clientID);
            notifyURI = replaceString(notifyURI, "@", clientID);

            String apn = Settings.getInstance().getSetting("apn", "internet");
            String otapUser = Settings.getInstance().getSetting("otapUser", "");
            String otapPassword = Settings.getInstance().getSetting("otapPassword", "");

            String otap
                    = "AT^SJOTAP=,"
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

            SLog.log(SLog.Debug, "CommandProcessor", "upgrade " + otap);

            String response1 = ATManager.getInstance().executeCommandSynchron(otap);
            String response2 = ATManager.getInstance().executeCommandSynchron("AT^SJOTAP\r");
            message = "upgrade " + response1 + " " + response2;
            return true;
        } else {
            message = "usage " + upgrade;
            return false;
        }
    }

    boolean execCommand(String[] parameters) {
        if (parameters.length == 2) {
            String response = ATManager.getInstance().executeCommandSynchron(parameters[1] + "\r");
            message = response;
            return true;
        } else {
            message = "usage " + exec + " at-cmd";
            return false;
        }
    }
}
