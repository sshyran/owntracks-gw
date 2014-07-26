package general;

//#undefine TRIAL_AND_ERROR
//#ifdef TRIAL_AND_ERROR
//# import java.io.IOException;
//# import java.io.InputStream;
//# import java.io.OutputStream;
//# import javax.microedition.io.Connector;
//# import javax.microedition.io.SecureConnection;
//# import javax.microedition.io.SecurityInfo;
//# import javax.microedition.io.SocketConnection;
//#endif

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

//#define DEBUGGING
public class MQTTHandler implements MqttCallback {

    private MqttClient client;
    private boolean firstConnect;

    private String clientId;
    private String brokerURL;
    private String userName;
    private String password;
    private String willTopic;
    private byte[] will;
    private int willQos;
    private boolean willRetain;
    private int keepAlive;
    private boolean cleanSession;
    private String subscription;
    private int subscriptionQos;

    private MQTTHandler() {
    }

    public static MQTTHandler getInstance() {
        return MQTTHandlerHolder.INSTANCE;
    }

    private static class MQTTHandlerHolder {

        private static final MQTTHandler INSTANCE = new MQTTHandler();
    }

    public void init(String aClientId,
            String aBrokerURL,
            String aUserName,
            String aPassword,
            String aWillTopic,
            byte[] aWill,
            int aWillQos,
            boolean aWillRetain,
            int aKeepAlive,
            boolean aCleanSession,
            String aSubscription,
            int aSubscriptionQos) {
        clientId = aClientId;
        brokerURL = aBrokerURL;
        userName = aUserName;
        password = aPassword;
        willTopic = aWillTopic;
        will = aWill;
        willQos = aWillQos;
        willRetain = aWillRetain;
        keepAlive = aKeepAlive;
        cleanSession = aCleanSession;
        subscription = aSubscription;
        subscriptionQos = aSubscriptionQos;
        firstConnect = true;
        
        if (client != null) {
            disconnect();
            client = null;
        }
    }

    public synchronized void connectToBroker()
            throws MqttSecurityException, MqttException {
        //#ifdef TRIAL_AND_ERROR
//#         String url = "ssl://github.com:80";
//#         System.out.println("SecureConnection to " + url);
//# 
//#         try {
//#             SecureConnection sc = (SecureConnection) Connector.open(url);
//#             SecurityInfo info = sc.getSecurityInfo();
//#             System.out.println("getProtocolName " + info.getProtocolName());
//# 
//#             sc.setSocketOption(SocketConnection.LINGER, 5);
//# 
//#             InputStream is = sc.openInputStream();
//#             OutputStream os = sc.openOutputStream();
//# 
//#             os.write("\r\n".getBytes());
//#             int ch = 0;
//#             while (ch != -1) {
//#                 ch = is.read();
//#             }
//# 
//#             is.close();
//#             os.close();
//#             sc.close();
//#             System.out.println("SecureConnection closed");
//# 
//#         } catch (IOException ioe) {
//#             ioe.printStackTrace();
//#         }
        //#endif

        //#ifdef DEBUGGING
        System.out.println("connectToBroker " + brokerURL + " as " + clientId);
        //#endif
        if (client == null) {
            try {
                client = new MqttClient(brokerURL, clientId, new MemoryPersistence());
                client.setCallback(this);
            } catch (MqttException e) {
                throw e;
            }
        }

        if (!client.isConnected()) {
            try {
                MqttConnectOptions options = new MqttConnectOptions();
                if (userName != null) {
                    options.setUserName(userName);
                }
                if (password != null) {
                    options.setPassword(password.toCharArray());
                }
                options.setCleanSession(cleanSession);
                options.setKeepAliveInterval(keepAlive);
                if (willTopic != null) {
                    options.setWill(client.getTopic(willTopic),
                            will, willQos, willRetain);
                }
                client.connect(options);
                if (subscription != null) {
                    if (cleanSession || firstConnect) {
                        client.subscribe(subscription, subscriptionQos);
                        firstConnect = false;
                    }
                }
            } catch (MqttSecurityException se) {
                throw se;
            } catch (MqttException e) {
                throw e;
            }
        }
    }

    public synchronized void publish(String topicName,
            int qos,
            boolean retained,
            byte[] payload)
            throws MqttException {
        //#ifdef DEBUGGING
        System.out.println("publish");
        //#endif

        if (!client.isConnected()) {
            try {
                connectToBroker();
            } catch (MqttSecurityException mse) {
                //
            } catch (MqttException me) {
                //
            }
        }

        if (client.isConnected()) {
            MqttTopic topic = client.getTopic(topicName);
            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);
            message.setRetained(retained);

            MqttDeliveryToken token = topic.publish(message);
        } else {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
    }

    public synchronized void subscribe(String topicName, int qos) throws MqttException {
        //#ifdef DEBUGGING
        System.out.println("subscribe " + topicName + " " + qos);
        //#endif

        if (client.isConnected()) {
            client.subscribe(topicName, qos);
        } else {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
    }

    public synchronized void disconnect() {
        //#ifdef DEBUGGING
        System.out.println("disconnect");
        //#endif
        try {
            client.disconnect(0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    
    public boolean isConnected() {
        if (client != null) {
            return client.isConnected();
        } else {
            return false;
        }
    }

    // Callbacks
    
    public void connectionLost(Throwable cause) {
        //#ifdef DEBUGGING
        System.out.println("connectionLost");
        //#endif
    }

    public void messageArrived(MqttTopic topic, MqttMessage message)
            throws Exception {
        //#ifdef DEBUGGING
        System.out.println("Msg arrived!");
        System.out.println("Topic: " + topic.getName() + " QoS: "
                + message.getQos());
        System.out.println("Message: " + new String(message.getPayload()));
        //#endif
        CommandProcessor commandProcessor = CommandProcessor.getInstance();
        if (commandProcessor.execute(message.toString())) {
            publish(topic.getName() + "/out", 0, false, ("ACK: " + commandProcessor.message).getBytes());
        } else {
            publish(topic.getName() + "/out", 0, false, ("NACK: " + commandProcessor.message).getBytes());
        }                                                
    }

    public void deliveryComplete(MqttDeliveryToken token) {
        //#ifdef DEBUGGING
        System.out.println("deliveryComplete");
        //#endif
    }
}
