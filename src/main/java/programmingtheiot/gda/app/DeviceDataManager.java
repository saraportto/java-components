/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

 package programmingtheiot.gda.app;

 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.time.OffsetDateTime;
 import java.time.temporal.ChronoUnit;
 
 import programmingtheiot.common.ConfigConst;
 import programmingtheiot.common.ConfigUtil;
 import programmingtheiot.common.IActuatorDataListener;
 import programmingtheiot.common.IDataMessageListener;
 import programmingtheiot.common.ResourceNameEnum;
 
 import programmingtheiot.data.ActuatorData;
 import programmingtheiot.data.DataUtil;
 import programmingtheiot.data.SensorData;
 import programmingtheiot.data.SystemPerformanceData;
 import programmingtheiot.data.SystemStateData;
 import programmingtheiot.data.BaseIotData;
 
 import programmingtheiot.gda.connection.CloudClientConnector;
 import programmingtheiot.gda.connection.CoapServerGateway;
 import programmingtheiot.gda.connection.IPersistenceClient;
 import programmingtheiot.gda.connection.IPubSubClient;
 import programmingtheiot.gda.connection.IRequestResponseClient;
 import programmingtheiot.gda.connection.MqttClientConnector;
 import programmingtheiot.gda.connection.SmtpClientConnector;
 import programmingtheiot.gda.system.SystemPerformanceManager;

/**
 * Shell representation of class for student implementation.
 *
 */
public class DeviceDataManager implements IDataMessageListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManager.class.getName());

	
	// private var's
	
	private boolean enableMqttClient = true;
	private boolean enableCoapServer = true;
	private boolean enableCloudClient = false;
	private boolean enableSmtpClient = false;
	private boolean enablePersistenceClient = false;
	private boolean enableSystemPerf = false;
	
	private IActuatorDataListener actuatorDataListener = null;
	private IPubSubClient mqttClient = null;
	//private IPubSubClient cloudClient = null;
	private IPersistenceClient persistenceClient = null;
	private IRequestResponseClient smtpClient = null;
	private CoapServerGateway coapServer = null;
	private SystemPerformanceManager sysPerfMgr = null;

	// Actuator and Sensor data tracking
	private ActuatorData latestHumidifierActuatorData = null;
	private ActuatorData latestHumidifierActuatorResponse = null;
	private SensorData latestHumiditySensorData = null;
	private OffsetDateTime latestHumiditySensorTimeStamp = null;
	
	// Threshold config
	private boolean handleHumidityChangeOnDevice = false;
	private int lastKnownHumidifierCommand = ConfigConst.OFF_COMMAND;
	
	private long humidityMaxTimePastThreshold = 300;
	private float nominalHumiditySetting = 40.0f;
	private float triggerHumidifierFloor = 30.0f;
	private float triggerHumidifierCeiling = 50.0f;

	private CloudClientConnector cloudClient = null;
	

	//private MqttClientConnector mqttClient; 

	
	// constructors
	
	public DeviceDataManager()
	{
		super();
		
		ConfigUtil configUtil = ConfigUtil.getInstance();

		this.enableMqttClient = configUtil.getBoolean(
								ConfigConst.GATEWAY_DEVICE, 
								ConfigConst.ENABLE_MQTT_CLIENT_KEY
								);

		this.enableCoapServer = configUtil.getBoolean(
								ConfigConst.GATEWAY_DEVICE, 
								ConfigConst.ENABLE_COAP_SERVER_KEY
								);

		this.enableCloudClient = configUtil.getBoolean(
								ConfigConst.GATEWAY_DEVICE, 
								ConfigConst.ENABLE_CLOUD_CLIENT_KEY
								);

		this.enablePersistenceClient = configUtil.getBoolean(
								ConfigConst.GATEWAY_DEVICE, 
								ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY
								);

		// --- Load humidity control settings ---
		this.handleHumidityChangeOnDevice = configUtil.getBoolean(
								ConfigConst.GATEWAY_DEVICE, 
								ConfigConst.HANDLE_HUMIDITY_CHANGE_ON_DEVICE_KEY
								);

		this.humidityMaxTimePastThreshold = configUtil.getInteger(
								ConfigConst.GATEWAY_DEVICE, 
								ConfigConst.HUMIDITY_MAX_TIME_PAST_THRESHOLD_KEY
								);

		this.nominalHumiditySetting = configUtil.getFloat(
								ConfigConst.GATEWAY_DEVICE, 
								ConfigConst.NOMINAL_HUMIDITY_SETTING_KEY
								);

		this.triggerHumidifierFloor = configUtil.getFloat(
								ConfigConst.GATEWAY_DEVICE, 
								ConfigConst.TRIGGER_HUMIDIFIER_FLOOR_KEY
								);

		this.triggerHumidifierCeiling = configUtil.getFloat(
								ConfigConst.GATEWAY_DEVICE, 
								ConfigConst.TRIGGER_HUMIDIFIER_CEILING_KEY
								);

		// Optional basic validation
		if (this.humidityMaxTimePastThreshold < 10 || this.humidityMaxTimePastThreshold > 7200) {
		this.humidityMaxTimePastThreshold = 300;
		}


		initManager();
		initConnections();
		
	}
	
	public DeviceDataManager(
		boolean enableMqttClient,
		boolean enableCoapClient,
		boolean enableCloudClient,
		boolean enableSmtpClient,
		boolean enablePersistenceClient)
	{
		super();
		
		initConnections();
	}
	
	
	// public methods

	@Override
	public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
	{
		_Logger.info("Handling actuator command response for resource: " + resourceName.toString());
		if (data != null) {
			_Logger.info("Handling actuator command response");
			if (data.hasError()) {
				_Logger.log(Level.WARNING, "Received actuator with error of status code: {0}", data.getStatusCode());
			}
			return true;
		} 
		return false;	
	}
	
	@Override
	public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.log(
				Level.FINE,
				"Actuator request received: {0}. Message: {1}",
				new Object[] {resourceName.getResourceName(), Integer.valueOf((data.getCommand()))});
	
			if (data.hasError()) {
				_Logger.warning("Error flag set for ActuatorData instance.");
			}
	
			this.sendActuatorCommandtoCda(resourceName, data);
	
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		if (data != null) {
			_Logger.fine("Handling sensor message: " + data.getName());
	
			if (data.hasError()) {
				_Logger.warning("Error flag set for SensorData instance.");
			}
	
			String jsonData = DataUtil.getInstance().sensorDataToJson(data);
	
			if (this.enablePersistenceClient && this.persistenceClient != null) {
				this.persistenceClient.storeData(resourceName.getResourceName(), ConfigConst.DEFAULT_QOS, data);
			}
	
			handleIncomingDataAnalysis(resourceName, data);

	
			handleUpstreamTransmission(resourceName, jsonData, ConfigConst.DEFAULT_QOS);
	
			return true;
		} else {
			return false;
		}
	}



	@Override
	public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		if (data != null) {
			_Logger.info("Handling system performance message: " + data.getName());
	
			if (data.hasError()) {
				_Logger.warning("Error flag set for SystemPerformanceData instance.");
			}
	
			if (this.cloudClient != null) {
				this.cloudClient.sendEdgeDataToCloud(resourceName, data);
			}
	
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
		if (listener != null) {
			this.actuatorDataListener = listener;
		}
	}
	

	public void startManager() {

		_Logger.info("Starting DeviceDataManager...");

		if (this.mqttClient != null) {
			if (this.mqttClient.connectClient()) {
				_Logger.info("Successfully connected MQTT client to broker.");
		
				int qos = ConfigConst.DEFAULT_QOS;
				this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos);
		
		
			} else {
				_Logger.severe("Failed to connect MQTT client to broker.");
			}
		}
		

		if (this.sysPerfMgr != null) {
			this.sysPerfMgr.startManager();
			_Logger.info("SystemPerformanceManager started.");
		}

		if (this.enableCoapServer && this.coapServer != null) {
			if (this.coapServer.startServer()) {
				_Logger.info("CoAP server started.");
			} else {
				_Logger.severe("Failed to start CoAP server. Check log file for details.");
			}
		}

		if (this.enableCloudClient && this.cloudClient != null) {
			this.cloudClient.setDataMessageListener(this);
			if (this.cloudClient.connectClient()) {
				_Logger.info("Connected to CloudClient.");
			} else {
				_Logger.warning("Failed to connect to CloudClient.");
			}
		}
		
	}

	public void stopManager() {

		_Logger.info("Stopping DeviceDataManager...");

		if (this.sysPerfMgr != null) {
			this.sysPerfMgr.stopManager();
			_Logger.info("SystemPerformanceManager stopped.");
		}
	
		if (this.mqttClient != null) {
			// Unsubscribe from topics
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);
	
			// Disconnect from the MQTT broker
			if (this.mqttClient.disconnectClient()) {
				_Logger.info("Successfully disconnected MQTT client from broker.");
			} else {
				_Logger.severe("Failed to disconnect MQTT client from broker.");
				// Handle disconnection failure.
			}
		}

		if (this.enableCoapServer && this.coapServer != null) {
			if (this.coapServer.stopServer()) {
				_Logger.info("CoAP server stopped.");
			} else {
				_Logger.severe("Failed to stop CoAP server. Check log file for details.");
			}
		}

		if (this.enableCloudClient && this.cloudClient != null) {
			if (this.cloudClient.disconnectClient()) {
				_Logger.info("Disconnected from CloudClient.");
			} else {
				_Logger.warning("Failed to disconnect from CloudClient.");
			}
		}
		
	}

	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		if (resourceName != null && msg != null) {
			try {
				if (resourceName == ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE) {
					_Logger.info("Handling incoming ActuatorData message: " + msg);

					ActuatorData ad = DataUtil.getInstance().jsonToActuatorData(msg);
					String jsonData = DataUtil.getInstance().actuatorDataToJson(ad);

					if (this.mqttClient != null) {
						int qos = ConfigUtil.getInstance().getInteger(
							ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.DEFAULT_QOS_KEY, ConfigConst.DEFAULT_QOS);
						_Logger.fine("Publishing data to MQTT broker: " + jsonData);
						return this.mqttClient.publishMessage(resourceName, jsonData, qos);
					}
					// TODO: If the GDA is hosting a CoAP server (or a CoAP client that
					// will connect to the CDA's CoAP server), you can add that logic here
					// in place of the MQTT client or in addition

				} else {
					_Logger.warning("Failed to parse incoming message. Unknown type: " + msg);

					return false;
				}
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to process incoming message for resource: " + resourceName, e);
			}
		} else {
			_Logger.warning("Incoming message has no data. Ignoring for resource: " + resourceName);
		}

		return false;
	}

	
	// private methods
	
	/**
	 * Initializes the enabled connections. This will NOT start them, but only create the
	 * instances that will be used in the {@link #startManager() and #stopManager()) methods.
	 * 
	 */

	private boolean isRunningInTestMode() { 
    	return Boolean.getBoolean("testMode"); 
	}


	private void initConnections()
	{
	}

	private void initManager() 
	{

		_Logger.info("Initializing DeviceDataManager...");

		ConfigUtil configUtil = ConfigUtil.getInstance();

		this.enableSystemPerf = configUtil.getBoolean(
								ConfigConst.GATEWAY_DEVICE, 
								ConfigConst.ENABLE_SYSTEM_PERF_KEY
								);

		if (this.enableSystemPerf) {
			this.sysPerfMgr = new SystemPerformanceManager();
			this.sysPerfMgr.setDataMessageListener(this);
		}

		if (this.enableMqttClient) {
			this.mqttClient = new MqttClientConnector();

			// NOTE: The next line isn't technically needed until Lab Module 10
			this.mqttClient.setDataMessageListener(this);  // Set the listener for MQTT messages.
		}
	
		if (this.enableCoapServer) {
			this.coapServer = new CoapServerGateway(this);
			_Logger.info("CoAP server gateway instance created.");
		}
	
		if (this.enableCloudClient) {
			this.cloudClient = new CloudClientConnector();
		}		
	
		if (this.enablePersistenceClient) {
			// TODO: implement this as an optional exercise in Lab Module 5
		}
	}

	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, ActuatorData data) {
		_Logger.fine("Analizando ActuatorData entrante: " + data.getName());
	}
	
	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SensorData data) {
		_Logger.fine("handleIncomingDataAnalysis called for SensorData.");
		handleHumiditySensorAnalysis(resourceName, data);
		handleUpstreamTransmission(resource, data, ConfigConst.DEFAULT_QOS);
		}

	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SystemStateData data)
	{
		_Logger.fine("handleIncomingDataAnalysis called for SystemStateData.");
	}
	

	private void handleHumiditySensorAnalysis(ResourceNameEnum resource, SensorData data) {
		_Logger.info("Analyzing humidity data from CDA: " + data.getLocationID() + ". Value: " + data.getValue());

		boolean isLow = data.getValue() < this.triggerHumidifierFloor;
		boolean isHigh = data.getValue() > this.triggerHumidifierCeiling;

		if (isLow || isHigh) {
			_Logger.info("Humidity data from CDA exceeds nominal range.");

			if (this.latestHumiditySensorData == null) {
				this.latestHumiditySensorData = data;
				this.latestHumiditySensorTimeStamp = getDateTimeFromData(data);

				_Logger.info(
					"Starting humidity nominal exception timer. Waiting for seconds: " +
					this.humidityMaxTimePastThreshold);
				return;
			} else {
				OffsetDateTime curHumiditySensorTimeStamp = getDateTimeFromData(data);
				long diffSeconds = ChronoUnit.SECONDS.between(this.latestHumiditySensorTimeStamp, curHumiditySensorTimeStamp);

				_Logger.info("Checking Humidity value exception time delta: " + diffSeconds);

				if (diffSeconds >= this.humidityMaxTimePastThreshold) {
					ActuatorData ad = new ActuatorData();
					ad.setName(ConfigConst.HUMIDIFIER_ACTUATOR_NAME);
					ad.setLocationID(data.getLocationID());
					ad.setTypeID(ConfigConst.HUMIDIFIER_ACTUATOR_TYPE);
					ad.setValue(this.nominalHumiditySetting);

					if (isLow) {
						ad.setCommand(ConfigConst.ON_COMMAND);
					} else if (isHigh) {
						ad.setCommand(ConfigConst.OFF_COMMAND);
					}

					_Logger.info("Humidity exceptional value reached. Sending actuation event to CDA: " + ad);

					this.lastKnownHumidifierCommand = ad.getCommand();
					sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);

					this.latestHumidifierActuatorData = ad;
					this.latestHumiditySensorData = null;
					this.latestHumiditySensorTimeStamp = null;
				}
			}
		} else if (this.lastKnownHumidifierCommand == ConfigConst.ON_COMMAND) {
			if (this.latestHumidifierActuatorData != null) {
				if (data.getValue() >= this.nominalHumiditySetting) {
					this.latestHumidifierActuatorData.setCommand(ConfigConst.OFF_COMMAND);

					_Logger.info("Humidity nominal value reached. Sending OFF actuation event to CDA: " +
						this.latestHumidifierActuatorData);

					sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, this.latestHumidifierActuatorData);

					this.lastKnownHumidifierCommand = this.latestHumidifierActuatorData.getCommand();
					this.latestHumidifierActuatorData = null;
					this.latestHumiditySensorData = null;
					this.latestHumiditySensorTimeStamp = null;
				} else {
					_Logger.info("Humidifier is still on. Not yet at nominal levels (OK).");
				}
			} else {
				_Logger.warning("ERROR: ActuatorData for humidifier is null (shouldn't be). Can't send command.");
			}
		}
	}


	private void sendActuatorCommandtoCda(ResourceNameEnum resource, ActuatorData data)
	{
		if (this.actuatorDataListener != null) {
			this.actuatorDataListener.onActuatorDataUpdate(data);
		}

		if (this.enableMqttClient && this.mqttClient != null) {
			String jsonData = DataUtil.getInstance().actuatorDataToJson(data);
			this.mqttClient.publishMessage(resource, jsonData, ConfigConst.DEFAULT_QOS);
		}
	}

	private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, SensorData data, int qos)
	{
		_Logger.info("Sending Json data to cloud: " + resourceName.toString());
		if (this.cloudClient != null) {
			if (this.cloudClient.sendEdgeDataToCloud(resourceName, data)) {
				_Logger.info("Published data to cloud: " + resourceName.toString());
				return true;
			} else {
				_Logger.warning("Failed to publish data to cloud: " + resourceName.toString());
			}
		} else {
			_Logger.warning("Cloud client is not enabled. Cannot publish data.");
		}
		return false;
	}

	private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, SystemPerformanceData data, int qos)
	{
	_Logger.info("Sending Json data to cloud: " + resourceName.toString());
	if (this.cloudClient != null) {
		if (this.cloudClient.sendEdgeDataToCloud(resourceName, data)) {
			_Logger.info("Published data to cloud: " + resourceName.toString());
			return true;
		} else {
			_Logger.warning("Failed to publish data to cloud: " + resourceName.toString());
		}
	} else {
		_Logger.warning("Cloud client is not enabled. Cannot publish data.");
	}
	return false;
	}

	private OffsetDateTime getDateTimeFromData(BaseIotData data)
	{
		OffsetDateTime odt =null;

		try {
		odt =OffsetDateTime.parse(data.getTimeStamp());
			}catch (Exception e) {
		_Logger.warning(
		"Failed to extract ISO 8601 timestamp from IoT data. Using local current time.");

		// TODO: this won't be accurate, but should be reasonably close, as the CDA will
		// most likely have recently sent the data to the GDA
		odt =OffsetDateTime.now();
			}

		return odt;
	}

}
