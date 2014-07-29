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
        //System.out.println("setSetting " + key + " " + ((value == null) ? value : "null"));
        if (hashTable == null) {
            loadSettings();
            //System.out.println("loaded");
        }
        if (value == null || value.length() == 0) {
            hashTable.remove(key);
        } else {
            hashTable.put(key, value);
        }

        //System.out.println(hashTable.toString());
        writeSettings();
    }

    public synchronized String getSetting(String key, String defaultValue) {
        //System.out.println("getSetting " + key);
        if (hashTable == null) {
            loadSettings();
            //System.out.println("loaded");
        }

        String value;
        value = (String) hashTable.get(key);

        //System.out.println("getSetting found" + ((value == null) ? value : "null"));
        if (value == null) {
            value = defaultValue;
        }
        //System.out.println("getSetting returns " + value);
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
        //System.out.println("loadSettings");
        if (hashTable == null) {
            hashTable = new Hashtable();
            //System.out.println("new HashTable");
        }

        try {
            FileConnection fconn = (FileConnection) Connector.open(fileURL);
            //System.out.println("Connector.open");
            if (!fconn.exists()) {
                fconn.create();
                fconn.setReadable(true);
                fconn.setWritable(true);
                //System.out.println("fconn.create");
            }

            InputStream is = fconn.openInputStream();
            //System.out.println("fconn.openInputStream");
            
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
                        //System.out.println("line " + line);
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

                } while (i != -1);
            } while (line != null);

            //System.out.println(hashTable.toString());
            is.close();
            //System.out.println("is.close");

            fconn.close();
            //System.out.println("fconn.close");
        
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void writeSettings() {
        //System.out.println("writeSettings");
        if (hashTable == null) {
            loadSettings();
            //System.out.println("loaded");
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
            //System.out.println("Connector.open");
        
            try {
                OutputStream os = fconn.openOutputStream();
                //System.out.println("fconn.openOutputStream");

                os.write(("# " + fileURL + " written: " + new Date() + "\n").getBytes("UTF-8"));

                for (Enumeration e = hashTable.keys(); e.hasMoreElements();) {
                    String key = (String) e.nextElement();
                    os.write((key + "=" + hashTable.get(key) + "\n").getBytes("UTF-8"));
                }

                os.write(("# EOF\n").getBytes("UTF-8"));

                os.flush();
                os.close();
                //System.out.println("os.close");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            fconn.close();
            //System.out.println("fconn.close");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Enumeration keys() {
        return hashTable.keys();
    }
}
