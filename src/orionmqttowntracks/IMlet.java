/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package orionmqttowntracks;

import javax.microedition.midlet.*;

/**
 * @author christoph
 */
public class IMlet extends MIDlet {

    public void startApp() {
        System.out.println("Yo OwnTracks");
    }
    
    public void pauseApp() {
         System.out.println("Ho OwnTracks");

    }
    
    public void destroyApp(boolean unconditional) {
        System.out.println("No OwnTracks");

    }
}
