package general;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

import com.m2mgo.net.SSLSocketFactory;


//#define DEBUGGING

public class MQTTHandler implements MqttCallback {

    private MqttClient client;
    
    private String clientId;
    private String brokerURL;
    private String userName;
    private String password;
    
    private SocketGPRStask view;

    public MQTTHandler(String clientId, String brokerURL, String userName, String password) {
        this.clientId = clientId;
        this.brokerURL = brokerURL;
        this.userName = userName;
        this.password = password;
    }

    public synchronized void connectToBroker()
            throws MqttSecurityException, MqttException {
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
                client.connect(options);
            } catch (MqttSecurityException se) {
                throw se;
            } catch (MqttException e) {
                throw e;
            }
        }
    }

    public void publish(String topicName,
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

    public void subscribe(String topicName, int qos) throws MqttException {
        //#ifdef DEBUGGING
        System.out.println("subscribe " + topicName + " " + qos);
        //#endif

        if (client.isConnected()) {
            client.subscribe(topicName, qos);
        } else {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
    }

    public void disconnect() {
        //#ifdef DEBUGGING
        System.out.println("disconnect");
        //#endif
        try {
            client.disconnect(0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

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
    }

    public void deliveryComplete(MqttDeliveryToken token) {
        //#ifdef DEBUGGING
        System.out.println("deliveryComplete");
        //#endif
    }

    public void applyView(SocketGPRStask view) {
        this.view = view;
    }
}
