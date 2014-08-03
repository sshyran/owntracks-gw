package com.m2mgo.net;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.SecureConnection;
import javax.microedition.io.SocketConnection;

import com.m2mgo.util.GPRSConnectOptions;

public class SSLSocketFactory extends SocketFactory {

    private static SSLSocketFactory sslSF = null;
    private SecureConnection secConn = null;

    final private GPRSConnectOptions connOptions =
            GPRSConnectOptions.getConnectOptions();

    private static SocketFactory getSocketFactory() {
        if (sslSF == null) {
            sslSF = new SSLSocketFactory();
        }
        return sslSF;
    }

    public static SocketFactory getDefault() {
        return getSocketFactory();
    }

    public SocketConnection createSocket(String host, int port)
            throws IOException {

        // example ssl://test.mosquitto.org:8883
        
        String uri = "ssl://" + host + ":" + port
                + ";bearer_type="
                + connOptions.getBearerType()
                + ";access_point="
                + connOptions.getAPN()
                + ";username="
                + connOptions.getUser()
                + ";password="
                + connOptions.getPasswd()
                + ";timeout="
                + connOptions.getTimeout();

        // System.out.println("Connector.open " + uri);
        try {
            secConn = (SecureConnection) Connector.open(uri);
        } catch (IOException ioe) {
            System.err.println("IOException");
            throw ioe;
        }
        return secConn;
    }
}
