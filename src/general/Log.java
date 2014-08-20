/*	
 * Class 	Log
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import java.io.*;
import java.util.*;
import javax.microedition.io.Connector;
import com.cinterion.io.file.FileConnection;

/**
 * Save important events on a log file
 *
 * @version	1.06 <BR> <i>Last update</i>: 07-05-2008
 * @author matteobo
 *
 */
public class Log {
    
    private static final String url = "file:///a:/log/";
    private static final String fileLog = "log.txt";
    private static final String fileOLD = "logOLD.txt";

    private static boolean free = true;
    
    public static void log(String error) {
        if (Settings.getInstance().getSetting("generalDebug", false)) {
            System.out.println("Log: " + error);
        }
        getLogSemaphore();
        write(error);
        freeLogSemaphore();
    }

    private static String write(String error) {
        
		//System.out.println("FreeMem: " + Runtime.getRuntime().freeMemory());
        //System.out.println("TotalMem: " + Runtime.getRuntime().totalMemory());
        try {
            if (Runtime.getRuntime().freeMemory() > 200) {
                FileConnection fconn = (FileConnection) Connector.open(url + fileLog);
	            // If no exception is thrown, then the URI is valid, but the file
                // may or may not exist.
                if (!fconn.exists()) {
                    fconn.create();   // create the file if it doesn't exist
                }

                DataOutputStream dos = fconn.openDataOutputStream();
                dos.write(("Versione software: " + AppMain.getInstance(). getAppProperty("MIDlet-Version") + "\r\n").getBytes());
                dos.flush();
                dos.close();
                //append writing
                fconn = (FileConnection) Connector.open(url + fileLog);
                fconn.setReadable(true);
                OutputStream os;
                //System.out.println(fconn.fileSize());
                os = fconn.openOutputStream(fconn.fileSize());
                Calendar cal = Calendar.getInstance();
                os.write((cal.getTime() + " - " + error + "\r\n").getBytes());
                //os.write((error + "\r\n").getBytes());
                os.flush();
                os.close();

                if (fconn.fileSize() > 20000) {
                    FileConnection fconn1 = (FileConnection) Connector.open(url + fileOLD);
                    if (fconn1.exists()) {
                        fconn1.delete();
                        fconn1.close();
                    }
                    fconn.rename(fileOLD);
                }
                //System.out.println(fconn.getName());
                fconn.close();
            }

        } catch (IOException ioe) {
			//System.out.println("exception: " + ioe.getMessage());
            //ioe.printStackTrace();
        } catch (SecurityException e) {
        }

        return "OK";
    }

    private static StringBuffer readLog(String fileName) {
        StringBuffer buffer = null;
        getLogSemaphore();

        try {
            FileConnection fconn = (FileConnection) Connector.open(fileName);
            if (fconn.exists()) {
                DataInputStream dos = fconn.openDataInputStream();
                buffer = new StringBuffer();
                while (dos.available() > 0) {
                    buffer.append((char) dos.read());
                }
            } else {
            }
            fconn.close();
        } catch (IOException ioe) {
        } catch (SecurityException e) {
        }
        freeLogSemaphore();
        return buffer;
    }
    
    public static StringBuffer readCurrentLog() {
        return readLog(url + fileLog);
    }
    public static StringBuffer readOldLog() {
        return readLog(url + fileOLD);
    }
    
    public static void deleteLog() {
        getLogSemaphore();

        try {
            FileConnection fconn1 = (FileConnection) Connector.open("file:///a:/log/log.txt");
            if (fconn1.exists()) {
                fconn1.delete();
            }
            fconn1.close();
        } catch (IOException e) {

        } catch (SecurityException e) {
        }
        freeLogSemaphore();
    }

    private synchronized static void getLogSemaphore() {
        try {
            while (!free) {
                Thread.sleep(1);
            }
        } catch (InterruptedException ie) {
            //
        }
    }

    private synchronized static void freeLogSemaphore() {
        free = true;
    }


}
