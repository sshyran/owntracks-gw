/*
 * Class 	CommASC0Thread
 *
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved.
 */
package general;

/**
 * @version	1.07 <BR> <i>Last update</i>: 04-08-2008
 * @author matteobo
 * @author Christoph Krey <krey.christoph@gmail.com>
 *
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.CommConnection;
import javax.microedition.io.Connector;

public class CommASC0Thread extends Thread {

    OutputStream serialOut;
    InputStream serialIn;

    private CommASC0Thread() {
    }

    public static CommASC0Thread getInstance() {
        return CommASC0ThreadHolder.INSTANCE;
    }

    private static class CommASC0ThreadHolder {

        private static final CommASC0Thread INSTANCE = new CommASC0Thread();
    }

    public void run() {
        try {
            CommConnection connASC0 = (CommConnection) Connector.open("comm:com0;baudrate=115200;bitsperchar=8;blocking=on");
            serialIn = connASC0.openInputStream();
            serialOut = connASC0.openOutputStream();
        } catch (IOException ie) {
            ie.printStackTrace();
        }

        while (true) {
            String command = "";
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }

            try {
                serialOut.write('>');

                int rx = 0;
                do {
                    rx = serialIn.read();
                    if (rx != '\n') {
                        if (rx >= 0) {
                            System.out.print((char) rx);
                        }
                        // update read string
                        if ((byte) rx != '\r') {
                            //serialOut.write((byte)rx);
                            command = command + (char) rx;
                        } else {
                            serialOut.write("\r\n".getBytes());
                        }
                    }
                } while ((char) rx != '\r');
            } catch (IOException ie) {
                //
            }

            CommandProcessor commandProcessor = CommandProcessor.getInstance();
            try {
                String response;
                if (commandProcessor.execute(command, false)) {
                    response = commandProcessor.message;
                } else {
                    response = "NACK: " + commandProcessor.message;
                }

                if (response.length() > 0) {

                    serialOut.write((response + "\r\n").getBytes());
                }
            } catch (IOException ioe) {
                SLog.log(SLog.Error, "CommASC0Thread", "IOException");
            }
        }
    }
}
