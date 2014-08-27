/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import java.util.*;
import java.io.*;
import javax.microedition.io.Connector;
import com.cinterion.io.file.FileConnection;

/**
 *
 * @author christoph krey
 */

public class Settings {

    private String fileURL = "file:///a:/file/settings.properties";
    private Hashtable hashTable;
    private Vector vector;
    
    private Settings() {
    }

    public static Settings getInstance() {
        return SettingsHolder.INSTANCE;
    }

    private static class SettingsHolder {

        private static final Settings INSTANCE = new Settings();
    }

    public synchronized void setfileURL(String url) {
        fileURL = url;
    }
    
    private synchronized void set(String key, String value, boolean write) {
        if (hashTable == null) {
            loadSettings();
        }
        if (value == null || value.length() == 0) {
            hashTable.remove(key);
        } else {
            hashTable.put(key, value);
        }        
        if (write) {
            writeSettings();            
        }
    }
    
    public void setSettingNoWrite(String key, String value) {
        set(key, value, false);
    }

    public void setSetting(String key, String value) {
        set(key, value, true);
    }

    public synchronized String getSetting(String key, String defaultValue) {
        if (hashTable == null) {
            loadSettings();
        }

        String value;
        value = (String) hashTable.get(key);

        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public synchronized long getSetting(String key, long defaultValue) {
        String string = getSetting(key, Long.toString(defaultValue));

        long value;
        try {
            value = Long.parseLong(string);
        } catch (NumberFormatException nfe) {
            value = 0L;
        }

        return value;
    }

    public synchronized int getSetting(String key, int defaultValue) {
        String string = getSetting(key, Integer.toString(defaultValue));

        int value;
        try {
            value = Integer.parseInt(string);
        } catch (NumberFormatException nfe) {
            value = 0;
        }

        return value;
    }

    public synchronized boolean getSetting(String key, boolean defaultValue) {
        String string = getSetting(key, defaultValue ? "1" : "0");

        int value;
        try {
            value = Integer.parseInt(string);
        } catch (NumberFormatException nfe) {
            value = 0;
        }

        if (value != 0) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized void loadSettings() {
        if (hashTable == null) {
            hashTable = new Hashtable();
        }

        try {
            FileConnection fconn = (FileConnection) Connector.open(fileURL);
            if (!fconn.exists()) {
                fconn.create();
                fconn.setReadable(true);
                fconn.setWritable(true);
            }

            InputStream is = fconn.openInputStream();
            
            String line = null;

            do {
                int i;

                do {
                    i = is.read();

                    if (i == -1) {
                        break;
                    }

                    if (i != '\r') {
                        if (line == null) {
                            line = new String();
                        }

                        if (i == '\n') {
                            if (line.length() == 0 || line.charAt(0) == '#') {
                                // comment
                            } else {
                                int equal = line.indexOf('=');
                                if (equal == -1) {
                                    // illegal
                                } else {
                                    String key = line.substring(0, equal);
                                    String value = line.substring(equal + 1);
                                    hashTable.put(key, value);
                                }
                            }
                            line = null;
                        } else {
                            line = line.concat(new StringBuffer().append((char) i).toString());
                        }
                    }
                } while (i != -1);
            } while (line != null);

            is.close();

            fconn.close();
        
        } catch (IOException ioe) {
            SLog.log(SLog.Error, "Settings", "load IOException"); 
        }
    }

    public synchronized void writeSettings() {
        if (hashTable == null) {
            loadSettings();
        }

        try {
            FileConnection fconn = (FileConnection) Connector.open(fileURL);
            if (!fconn.exists()) {
                fconn.create();
                fconn.setReadable(true);
                fconn.setWritable(true);
            } else {
                fconn.truncate(0);
            }
        
            try {
                OutputStream os = fconn.openOutputStream();

                os.write(("# " + fileURL + " written: " + new Date() + "\n").getBytes("UTF-8"));
                
                for (Enumeration e = hashTable.keys(); e.hasMoreElements();) {
                    String key = (String) e.nextElement();
                    os.write((key + "=" + hashTable.get(key) + "\n").getBytes("UTF-8"));
                }

                os.write(("# EOF\n").getBytes("UTF-8"));

                os.flush();
                os.close();
            } catch (IOException ioe) {
                SLog.log(SLog.Debug, "Settings", "write IOException");
            }
            fconn.close();
        } catch (IOException ioe) {
            SLog.log(SLog.Debug, "Settings", "write open IOException");
        }
    }

    public Enumeration keys() {
        if (vector == null) {
            vector = new Vector();
        } else {
            vector.removeAllElements();
        }
        
        Enumeration enumeration = hashTable.keys();
        while (enumeration.hasMoreElements()) {
            String key = (String)enumeration.nextElement();
            int i;
            for (i = 0; i < vector.size(); i++) {
                String vectorKey = (String)vector.elementAt(i);
                if (key.compareTo(vectorKey) < 0) {
                    break;
                }
            }
            vector.insertElementAt(key, i);
        }
        
        return vector.elements();
    }
}
