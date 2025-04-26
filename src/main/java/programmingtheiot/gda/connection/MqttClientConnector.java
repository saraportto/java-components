/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.connection;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class MqttClientConnector implements IPubSubClient, MqttCallbackExtended
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientConnector.class.getName());
	
	// params
	private boolean useAsyncClient = false;

	private MqttClient           mqttClient = null;
	private MqttConnectOptions   connOpts = null;
	private MemoryPersistence    persistence = null;
	private IDataMessageListener dataMsgListener = null;

	private String  clientID = null;
	private String  brokerAddr = null;
	private String  host = ConfigConst.DEFAULT_HOST;
	private String  protocol = ConfigConst.DEFAULT_MQTT_PROTOCOL;
	private int     port = ConfigConst.DEFAULT_MQTT_PORT;
	private int     brokerKeepAlive = ConfigConst.DEFAULT_KEEP_ALIVE;

	private static final int DEFAULT_QOS = 1;

	
	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	
	public MqttClientConnector()
	{
		super();

		ConfigUtil configUtil = ConfigUtil.getInstance();

		this.host = configUtil.getProperty(
			ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);

		this.port = configUtil.getInteger(
			ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_PORT);

		this.brokerKeepAlive = configUtil.getInteger(
			ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);

		this.useAsyncClient = configUtil.getBoolean(
			ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.USE_ASYNC_CLIENT_KEY);

		this.clientID = MqttClient.generateClientId();

		this.persistence = new MemoryPersistence();
		this.connOpts = new MqttConnectOptions();
		this.connOpts.setKeepAliveInterval(this.brokerKeepAlive);
		this.connOpts.setCleanSession(false);
		this.connOpts.setAutomaticReconnect(true);

		this.brokerAddr = this.protocol + "://" + this.host + ":" + this.port;
	}
	
	// public methods
	
	@Override
	public boolean connectClient()
	{
		if (this.mqttClient == null) {
			try {
				this.mqttClient = new MqttClient(this.brokerAddr, this.clientID, this.persistence);
				this.mqttClient.setCallback(this);
	
				_Logger.info("Connecting to broker: " + this.brokerAddr);
				this.mqttClient.connect(this.connOpts);
	
				_Logger.info("Connected to broker: " + this.brokerAddr);
				
				return true;
			} catch (MqttSecurityException e) {
				_Logger.log(Level.SEVERE, "Security exception during connect.", e);
			} catch (MqttException e) {
				_Logger.log(Level.SEVERE, "MQTT exception during connect.", e);
			}
		} else {
			_Logger.warning("Client already initialized.");
		}
	
		return false;
	}
	

	@Override
	public boolean disconnectClient()
	{
		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			try {
				this.mqttClient.disconnect();
				_Logger.info("Disconnected from broker: " + this.brokerAddr);
				return true;
			} catch (MqttException e) {
				_Logger.log(Level.SEVERE, "Exception during disconnect.", e);
			}
		} else {
			_Logger.warning("Client is not connected. Disconnect skipped.");
		}
		return false;
	}

	@Override
	public boolean isConnected() {
		return (this.mqttClient != null && this.mqttClient.isConnected());
	}
	
	@Override
	public boolean publishMessage(ResourceNameEnum topicName, String msg, int qos) {
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to publish message: " + this.brokerAddr);
			return false;
		}
	
		if (msg == null || msg.length() == 0) {
			_Logger.warning("Message is null or empty. Unable to publish message: " + this.brokerAddr);
			return false;
		}
	
		if (qos < 0 || qos > 2) {
			qos = DEFAULT_QOS;
		}
	
		try {
			byte[] payload = msg.getBytes();
			MqttMessage mqttMsg = new MqttMessage(payload);
			mqttMsg.setQos(qos);
			this.mqttClient.publish(topicName.getResourceName(), mqttMsg);
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to publish message to topic: " + topicName, e);
		}
	
		return false;
	}
	

	@Override
	public boolean subscribeToTopic(ResourceNameEnum topicName, int qos) {
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to subscribe to topic: " + this.brokerAddr);
			return false;
		}
	
		if (qos < 0 || qos > 2) {
			qos = DEFAULT_QOS;
		}
	
		try {
			this.mqttClient.subscribe(topicName.getResourceName(), qos);
			_Logger.info("Successfully subscribed to topic: " + topicName.getResourceName());
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to subscribe to topic: " + topicName, e);
		}
	
		return false;
	}
	

	@Override
	public boolean unsubscribeFromTopic(ResourceNameEnum topicName) {
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to unsubscribe from topic: " + this.brokerAddr);
			return false;
		}
	
		try {
			this.mqttClient.unsubscribe(topicName.getResourceName());
			_Logger.info("Successfully unsubscribed from topic: " + topicName.getResourceName());
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to unsubscribe from topic: " + topicName, e);
		}
	
		return false;
	}
	
	

	@Override
	public boolean setConnectionListener(IConnectionListener listener)
	{
		return false;
	}
	
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
			return true;
		}
	
		return false;
	}
	
	// callbacks
	
	@Override
	public void connectComplete(boolean reconnect, String serverURI)
	{
		_Logger.info("MQTT connection successful (is reconnect = " + reconnect + "). Broker: " + serverURI);
	}

	@Override
	public void connectionLost(Throwable t)
	{
		_Logger.log(Level.WARNING, "Lost connection to MQTT broker: " + this.brokerAddr, t);
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		if (token != null) {
			try {
				if (token.getMessage() != null) {
					_Logger.info("Delivery complete. Message ID: " + token.getMessageId() + 
						", Topic: " + token.getTopics()[0] + 
						", Payload: " + new String(token.getMessage().getPayload()));
				} else {
					_Logger.warning("Delivery complete, but message is null.");
				}
			} catch (MqttException e) {
				_Logger.log(Level.SEVERE, "Error getting message from token", e);
			}
		} else {
			_Logger.warning("Delivery complete, but token is null.");
		}
	}
	
	
	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception
	{
		try {
			// Log the arrival of the MQTT message
			_Logger.info("MQTT message arrived on topic: '" + topic + "' with payload: " + new String(msg.getPayload()));
	
			// If a data message listener is set, pass the message to it
			if (this.dataMsgListener != null) {
				this.dataMsgListener.handleIncomingMessage(
					ResourceNameEnum.getEnumFromValue(topic), 
					new String(msg.getPayload())
				);
			} else {
				_Logger.warning("No data message listener configured. Message not processed.");
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Exception occurred while processing incoming message.", e);
		}
	}
	


	
	// private methods
	
	/**
	 * Called by the constructor to set the MQTT client parameters to be used for the connection.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initClientParameters(String configSectionName)
	{
		// TODO: implement this
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to load credentials.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initCredentialConnectionParameters(String configSectionName)
	{
		// TODO: implement this
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to enable encryption.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initSecureConnectionParameters(String configSectionName)
	{
		// TODO: implement this
	}
}
