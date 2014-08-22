package general;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

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

    public synchronized void connectToBroker() {
        if (Settings.getInstance().getSetting("mqttDebug", false)) {
            System.out.println("connectToBroker " + brokerURL
                    + " as " + clientId
                    + "(c" + (cleanSession ? "1" : "0")
                    + " k" + keepAlive
                    + " u" + ((userName == null) ? "<null>" : userName) + ")");
        }
        if (client == null) {
            try {
                client = new MqttClient(brokerURL, clientId, new MemoryPersistence());
                client.setCallback(this);
            } catch (MqttException e) {
                System.err.println("Error setCallback: " + e.getReasonCode());
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
                if (Settings.getInstance().getSetting("mqttDebug", false)) {
                    System.out.println("connect w/ options");
                }

                client.connect(options);
                
                publishIfConnected(willTopic, willQos, willRetain, "1".getBytes());

                if (subscription != null) {
                    if (cleanSession || firstConnect) {
                        if (Settings.getInstance().getSetting("mqttDebug", false)) {
                            System.out.println("subscribe");
                        }

                        client.subscribe(subscription, subscriptionQos);
                        firstConnect = false;
                    }
                }
            } catch (MqttSecurityException e) {
                System.err.println("Security Error connectToBroker: " + e.getReasonCode());
            } catch (MqttException e) {
                System.err.println("Error connectToBroker: " + e.getReasonCode());
            }
        }
        
    }

    public synchronized void publishIfConnected(String topicName,
            int qos,
            boolean retained,
            byte[] payload) {

        if (Settings.getInstance().getSetting("mqttDebug", false)) {
            System.out.println("publish");
        }

        if (client.isConnected()) {
            MqttTopic topic = client.getTopic(topicName);
            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);
            message.setRetained(retained);

            try {
                MqttDeliveryToken token = topic.publish(message);
            } catch (MqttException e) {
                System.err.println("Error publish: " + e.getReasonCode());
            }
        } else {
            // not connected
        }
    }

    public synchronized void publish(String topicName,
            int qos,
            boolean retained,
            byte[] payload) {

        if (!client.isConnected()) {
            connectToBroker();
        }

        publishIfConnected(topicName, qos, retained, payload);
    }

    public synchronized void subscribe(String topicName, int qos) {
        if (Settings.getInstance().getSetting("mqttDebug", false)) {
            System.out.println("subscribe " + topicName + " " + qos);
        }
        if (client.isConnected()) {
            try {
                client.subscribe(topicName, qos);
            } catch (MqttException e) {
                System.err.println("Error subscribe: " + e.getReasonCode());
            }
        } else {
            // not connected
        }
    }

    public synchronized void disconnect() {
        if (Settings.getInstance().getSetting("mqttDebug", false)) {
            System.out.println("disconnect");
        }
        
        if (client.isConnected()) {
            publish(willTopic, willQos, willRetain, "-1".getBytes());
        }

        try {
            client.disconnect(0);
        } catch (MqttException e) {
            System.err.println("Error disconnect: " + e.getReasonCode());
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
        if (Settings.getInstance().getSetting("mqttDebug", false)) {
            System.out.println("connectionLost");
        }
    }

    public void messageArrived(MqttTopic topic, MqttMessage message)
            throws Exception {
        if (Settings.getInstance().getSetting("mqttDebug", false)) {
            System.out.println("messageArrived " + topic.getName()
                + " q" + message.getQos()
                + " r" + (message.isRetained() ? "1" : "0")
                + "\r\n" + new String(message.getPayload()));
        }
        CommandProcessor commandProcessor = CommandProcessor.getInstance();
        if (commandProcessor.execute(message.toString(), false)) {
            SocketGPRSThread.getInstance().put(topic.getName() + "/out", 0, false, ("ACK: " + commandProcessor.message).getBytes());
        } else {
            SocketGPRSThread.getInstance().put(topic.getName() + "/out", 0, false, ("NACK: " + commandProcessor.message).getBytes());
        }
    }

    public void deliveryComplete(MqttDeliveryToken token) {
        if (Settings.getInstance().getSetting("mqttDebug", false)) {
            System.out.println("deliveryComplete");
        }
    }
}
