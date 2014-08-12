/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package general;

import com.cinterion.io.BearerControlListener;
import com.m2mgo.util.GPRSConnectOptions;

/**
 *
 * @author christoph
 */
public class Bearer implements BearerControlListener {
    
    private int bearerState;
    private boolean gprsOn;
    
    private Bearer() {
        GPRSConnectOptions.getConnectOptions().setAPN(Settings.getInstance().getSetting("apn", "internet"));
        GPRSConnectOptions.getConnectOptions().setBearerType("gprs");
        GPRSConnectOptions.getConnectOptions().setUser(Settings.getInstance().getSetting("apnUser", ""));
        GPRSConnectOptions.getConnectOptions().setPasswd(Settings.getInstance().getSetting("apnPassword", ""));
    }
    
    public static Bearer getInstance() {
        return BearerHolder.INSTANCE;
    }
    
    private static class BearerHolder {

        private static final Bearer INSTANCE = new Bearer();
    }

    public void stateChanged(int state) {
        bearerState = state;

        if (state == BEARER_STATE_CLOSING) {
            gprsOn = false;
        } else if (state == BEARER_STATE_CONNECTING) {
            gprsOn = false;
        } else if (state == BEARER_STATE_DOWN) {
            gprsOn = false;
        } else if (state == BEARER_STATE_LIMITED_UP) {
            gprsOn = false;
        } else if (state == BEARER_STATE_UP) {
            gprsOn = true;
        }
    }

    public int getBearerState() {
        return bearerState;
    }

    public boolean isGprsOn() {
        return gprsOn;
    }
}
