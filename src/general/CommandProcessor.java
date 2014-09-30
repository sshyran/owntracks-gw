package general;

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
    private final String device = "device";
    private final String set = "set";
    private final String out = "out";
    private final String off = "off";
    private final String zero = "zero";
    private final String reboot = "reboot";
    private final String upgrade = "upgrade";
    private final String reconnect = "reconnect";
    private final String exec = "exec";
    private final String destroy = "destroy";
    private final String log = "log";
    private final String bootstrap = "bootstrap";

    private final String[] authorizedCommands = {
        set,
        device,
        out,
        off,
        zero,
        reboot,
        reconnect,
        log,
        logout,
        destroy,
        exec,
        upgrade
    };

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
        message = "";
        SLog.log(SLog.Debug, "CommandProcessor", "execute " + (commandLine != null ? commandLine : "<null>"));
        if (commandLine != null && commandLine.length() > 0) {
            String[] words = StringFunc.split(commandLine, " ");
            if (words.length >= 1) {
                Settings settings = Settings.getInstance();
                if (!StringFunc.isInStringArray(words[0], authorizedCommands)
                        || settings.getSetting("loginTimeout", 30) == 0
                        || authorizedSince + settings.getSetting("loginTimeout", 30) > new Date().getTime() / 1000) {
                    if (words[0].equals("login")) {
                        if ((words.length == 2)
                                && (words[1].equals(settings.getSetting("secret", "1234567890")))) {
                            authorizedSince = new Date().getTime() / 1000;
                            return true;
                        } else {
                            message = message.concat("incorrect login");
                            return false;
                        }
                    } else if (words[0].equals("logout")) {
                        authorizedSince = 0;
                        return true;
                    } else {
                        return perform(words[0], words);
                    }
                } else {
                    message = message.concat("illegal command " + commandLine);
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public boolean perform(String command, String[] parameters) {
        SLog.log(SLog.Debug, "CommandProcessor", "perform " + command + " " + parameters.length);

        message = "";
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
            return true;

        } else if (command.equalsIgnoreCase(zero)) {
            LocationManager.getInstance().zero();
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

        } else if (command.equalsIgnoreCase(out)) {
            return outCommand(parameters);

        } else if (command.equalsIgnoreCase(bootstrap)) {
            return bootstrapCommand(parameters);

        } else if (command.equalsIgnoreCase(off)) {
            return offCommand(parameters);

        } else if (command.equalsIgnoreCase(upgrade)) {
            return upgradeCommand(parameters);

        } else if (command.equalsIgnoreCase(exec)) {
            return execCommand(parameters);

        } else if (command.equalsIgnoreCase(state)) {
            return stateCommand(parameters);

        } else if (command.equalsIgnoreCase(device)) {
            return deviceCommand(parameters);

        } else if (command.equalsIgnoreCase(log)) {
            return logCommand(parameters);
        } else {
            message = message.concat("not implemented");
            return false;
        }
    }

    boolean setCommand(String[] parameters) {
        message = "";
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
                if (equal != -1) {
                    settings.setSetting(key, value);
                } else {
                    message = message.concat(key + "=" + settings.getSetting(key, "<null>"));
                }
                return true;
            }
        }
        message = message.concat("usage " + set + " [<key>[=[<value>]]]");
        return false;
    }

    boolean outCommand(String[] parameters) {
        message = "";
        if (parameters.length == 3) {
            int num;
            boolean on = false;

            try {
                int numInt = Integer.parseInt(parameters[1]);
                switch (numInt) {
                    case 1:
                        num = 8;
                        break;
                    case 2:
                        num = 9;
                        break;
                    default:
                        num = -1;
                        break;
                }

                int onInt = Integer.parseInt(parameters[2]);
                switch (onInt) {
                    case 0:
                        on = false;
                        break;
                    case 1:
                        on = true;
                        break;
                    default:
                        num = -1;
                        break;
                }
            } catch (NumberFormatException nfe) {
                num = -1;
            }

            if (num != -1) {
                GPIOManager.getInstance().setGPIO(num, on);
                return true;
            }
        }
        message = message.concat("usage " + out + " 1/2 0/1");
        return false;
    }

    boolean bootstrapCommand(String[] parameters) {
        message = "";
        Settings settings = Settings.getInstance();

        if (parameters.length == 3) {
            if (parameters[1].equals(settings.getSetting("secret", "1234567890"))) {
                String[] components = StringFunc.split(parameters[2], ",");
                if (components.length == 7) {
                    settings.setSettingNoWrite("apn", components[0]);
                    settings.setSettingNoWrite("apnUser", components[1]);
                    settings.setSettingNoWrite("apnPassword", components[2]);
                    settings.setSettingNoWrite("host", components[3]);
                    settings.setSettingNoWrite("port", components[4]);
                    settings.setSettingNoWrite("user", components[5]);
                    settings.setSetting("password", components[6]);
                    BatteryManager.getInstance().reboot();
                    return true;                    
                }
            }
        }
        message = message.concat("usage " + bootstrap + "<secret> <apn>,<apnUser>,<apnPassword>,<host>,<port>,<user>,<password>");
        return false;
    }

    boolean offCommand(String[] parameters) {
        message = "";
        if (parameters.length == 2) {
            int min;

            try {
                min = Integer.parseInt(parameters[1]);
            } catch (NumberFormatException nfe) {
                min = -1;
            }

            if (min >= 0) {
                if (min == 0) {
                    Settings.getInstance().setSetting("offUntil", null);
                } else {
                    Date now = new Date();
                    Date until = new Date(now.getTime() + min * 60L * 1000L);
                    Settings.getInstance().setSetting("offUntil", Long.toString(until.getTime() / 1000));
                }
                return true;
            }
        }
        message = message.concat("usage " + off + " 0|1..");
        return false;
    }

    boolean stateCommand(String[] parameters) {
        message = "NUMSAT=" + LocationManager.getInstance().getNumSat() + CRLF;
        message = message.concat("BEARER=" + Bearer.getInstance().getBearerState() + CRLF);
        message = message.concat("GPRS=" + (Bearer.getInstance().isGprsOn() ? 1 : 0) + CRLF);
        message = message.concat("CREG=" + SocketGPRSThread.getInstance().creg + CRLF);
        message = message.concat("CGREG=" + SocketGPRSThread.getInstance().cgreg + CRLF);
        message = message.concat("BATT=" + BatteryManager.getInstance().getBatteryVoltageString() + CRLF);
        message = message.concat("EXTV=" + BatteryManager.getInstance().getExternalVoltageString() + CRLF);
        message = message.concat("Q=" + SocketGPRSThread.getInstance().qSize() + CRLF);
        message = message.concat("CONN=" + (SocketGPRSThread.getInstance().isConnected() ? 1 : 0) + CRLF);
        message = message.concat("NETW=" + (SocketGPRSThread.getInstance().isNetwork() ? 1 : 0) + CRLF);
        message = message.concat("OPER=" + SocketGPRSThread.getInstance().getOperator() + CRLF);
        message = message.concat("WAKEUP=" + AppMain.getInstance().wakeupMode + CRLF);
        message = message.concat("DATE=" + DateFormatter.isoString(new Date()) + CRLF);
        return true;
    }

    boolean deviceCommand(String[] parameters) {
        message = "uFW=" + MicroManager.getInstance().getRelease()
                + "," + MicroManager.getInstance().getBootRelease()
                + "," + MicroManager.getInstance().getJavaRelease() + CRLF;
        message = message.concat("SW=" + AppMain.getInstance().getAppProperty("MIDlet-Version") + CRLF);
        message = message.concat("EG5=" + MicroManager.getInstance().getInfo() + CRLF);
        message = message.concat("IMEI=" + MicroManager.getInstance().getIMEI() + CRLF);
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

    boolean upgradeCommand(String[] parameters) {
        if (parameters.length == 1) {
            String clientID = Settings.getInstance().getSetting("clientID", MicroManager.getInstance().getIMEI());
            String otapURI = Settings.getInstance().getSetting("otapURI", "");
            String notifyURI = Settings.getInstance().getSetting("notifyURI", "");
            otapURI = StringFunc.replaceString(otapURI, "@", clientID);
            notifyURI = StringFunc.replaceString(notifyURI, "@", clientID);

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
