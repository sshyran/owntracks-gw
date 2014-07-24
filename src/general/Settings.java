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

//#undefine DEBUGGING

public class Settings {

    private String fileURL = "file:///a:/file/settings.properties";
    private Hashtable hashTable;

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
    
    public synchronized void setSetting(String key, String value) {
        //#ifdef DEBUGGING
//#         System.out.println("setSetting " + key + " " + ((value == null) ? value : "null"));
        //#endif
        if (hashTable == null) {
            loadSettings();
            //#ifdef DEBUGGING
//#             System.out.println("loaded");
            //#endif
        }
        if (value == null || value.length() == 0) {
            hashTable.remove(key);
        } else {
            hashTable.put(key, value);
        }

        //#ifdef DEBUGGING
//#         System.out.println(hashTable.toString());
        //#endif
        writeSettings();
    }

    public synchronized String getSetting(String key, String defaultValue) {
        //#ifdef DEBUGGING
//#         System.out.println("getSetting " + key);
        //#endif
        if (hashTable == null) {
            loadSettings();
            //#ifdef DEBUGGING
//#             System.out.println("loaded");
            //#endif
        }

        String value;
        value = (String) hashTable.get(key);

        //#ifdef DEBUGGING
//#         System.out.println("getSetting found" + ((value == null) ? value : "null"));
        //#endif
        if (value == null) {
            value = defaultValue;
        }
        //#ifdef DEBUGGING
//#         System.out.println("getSetting returns " + value);
        //#endif
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
        //#ifdef DEBUGGING
//#         System.out.println("loadSettings");
        //#endif
        if (hashTable == null) {
            hashTable = new Hashtable();
            //#ifdef DEBUGGING
//#             System.out.println("new HashTable");
            //#endif
        }

        try {
            FileConnection fconn = (FileConnection) Connector.open(fileURL);
            //#ifdef DEBUGGING
//#             System.out.println("Connector.open");
            //#endif
            if (!fconn.exists()) {
                fconn.create();
                fconn.setReadable(true);
                fconn.setWritable(true);
                //#ifdef DEBUGGING
//#                 System.out.println("fconn.create");
                //#endif
            }

            InputStream is = fconn.openInputStream();
            //#ifdef DEBUGGING
//#             System.out.println("fconn.openInputStream");
            //#endif

            String line = null;

            do {
                int i;

                do {
                    i = is.read();

                    if (i == -1) {
                        break;
                    }

                    if (i == '\r') {
                        // ignore
                    }

                    if (line == null) {
                        line = new String();
                    }

                    if (i == '\n') {
                        //#ifdef DEBUGGING
//#                         System.out.println("line " + line);
                        //#endif
                        if (line.charAt(0) == '#') {
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

                } while (i != -1);
            } while (line != null);

            System.out.println(hashTable.toString());

            is.close();
            //#ifdef DEBUGGING
//#             System.out.println("is.close");
            //#endif

            fconn.close();
            //#ifdef DEBUGGING
//#             System.out.println("fconn.close");
            //#endif

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public synchronized void writeSettings() {
        //#ifdef DEBUGGING
//#         System.out.println("writeSettings");
        //#endif
        if (hashTable == null) {
            loadSettings();
            //#ifdef DEBUGGING
//#             System.out.println("loaded");
            //#endif
        }

        try {
            FileConnection fconn = (FileConnection) Connector.open(fileURL);
            if (!fconn.exists()) {
                fconn.create();
                fconn.setReadable(true);
                fconn.setWritable(true);
            }
            //#ifdef DEBUGGING
//#             System.out.println("Connector.open");
            //#endif

            try {
                OutputStream os = fconn.openOutputStream();
                //#ifdef DEBUGGING
//#                 System.out.println("fconn.openOutputStream");
                //#endif

                os.write(("# " + fileURL + " written: " + new Date() + "\n").getBytes("UTF-8"));

                for (Enumeration e = hashTable.keys(); e.hasMoreElements();) {
                    String key = (String) e.nextElement();
                    os.write((key + "=" + hashTable.get(key) + "\n").getBytes("UTF-8"));
                }

                os.write(("# EOF\n").getBytes("UTF-8"));

                os.flush();
                os.close();
                //#ifdef DEBUGGING
//#                 System.out.println("os.close");
                //#endif
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            fconn.close();
            //#ifdef DEBUGGING
//#             System.out.println("fconn.close");
            //#endif
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public Enumeration keys() {
        return hashTable.keys();
    }
}
