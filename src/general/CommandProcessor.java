package general;

//#define DEBUGGING

import java.util.Date;
import java.util.Enumeration;
import java.io.UnsupportedEncodingException;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 *
 * @author christoph krey
 */
public class CommandProcessor implements GlobCost {
    
    final private String CRLF = "\r\n";
    private long authorizedSince = 0;
    public String message = null;
        
    private boolean isInStringArray(String string, String[] stringArray) {
        for (int i = 0; i < stringArray.length; i++) {
            if (string.equals(stringArray[i])) {
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
    
    public synchronized boolean execute(String commandLine) {
        final String[] unauthorizedCommands = {"login", "gps"};
        final String[] authorizedCommands = {"set", "reboot", "logout"};

        //#ifdef DEBUGGING
        System.out.println("execute " + (commandLine != null ? commandLine : "<null>"));
        //#endif
        if (commandLine != null && commandLine.length() > 0) {
            String[] words = StringSplitter.split(commandLine, " ");
            //#ifdef DEBUGGING
            for (int i = 0; i < words.length; i++) {
                System.out.println("words[" + i + "]=" + words[i]);                
            }
            //#endif
            if (words.length >= 1) {
                Settings settings = Settings.getInstance();
                message = "command: \"" + commandLine + "\"" + CRLF;
                if (    
                        (isInStringArray(words[0], unauthorizedCommands)) ||
                        (
                            (isInStringArray(words[0], authorizedCommands)) &&
                            (authorizedSince + settings.getSetting("loginTimeout", 30) > new Date().getTime()/1000)
                        )
                    ) {
                    if (words[0].equals("login")) {
                        if (
                                (words.length == 2) &&
                                (words[1].equals(settings.getSetting("secret", "1234567890")))
                           ) {
                            authorizedSince = new Date().getTime()/1000;
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
                        return perform(words[0], words);
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
    
    private boolean perform(String command, String[] parameters) {
        Settings settings = Settings.getInstance();
        //#ifdef DEBUGGING
        System.out.println("perform " + command);
        //#endif
        
        if (command.equals("gps")) {
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
            
        } else if (command.equals("reboot")) {
            if (parameters.length == 1) {
                message = message.concat("rebooting");
                SemAT.getInstance().getCoin(5);
                InfoStato.getInstance().writeATCommand("AT+CFUN=1,1\r");				
                SemAT.getInstance().putCoin();

                return true;
            } else {
                message = message.concat("usage reboot");
                return false;
            }
            
        } else if (command.equals("set")) {
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
                message = message.concat("usage set [<key>[=[<value>]]]");
                return false;
            }
        } else {
            message = message.concat("not implemented");
            return false;
        }
    }
}
