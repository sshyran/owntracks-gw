package com.m2mgo.net;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import com.m2mgo.util.GPRSConnectOptions;

public class SocketFactory extends Object {

	private static SocketFactory sf = null;
	protected SocketConnection sc = null;
	private GPRSConnectOptions connOptions = GPRSConnectOptions
			.getConnectOptions();

	private static SocketFactory getSocketFactory() {
		if (sf == null) {
			sf = new SocketFactory();
		}
		return sf;
	}

	public SocketConnection createSocket(String host, int port)
			throws IOException {
		sc = (SocketConnection) Connector.open("socket://" + host + ":" + port
				+ ";bearer_type="
				+ connOptions.getBearerType() 
				+ ";access_point="
				+ connOptions.getAPN() 
				+ ";username="
				+ connOptions.getUser() 
				+ ";password="
				+ connOptions.getPasswd() 
				+ ";timeout="
				+ connOptions.getTimeout());
		return sc;
	}

	public static SocketFactory getDefault() {
		return getSocketFactory();
	}
}
