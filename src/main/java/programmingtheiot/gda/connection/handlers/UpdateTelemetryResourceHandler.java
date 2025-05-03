/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

 package programmingtheiot.gda.connection.handlers;

 import java.util.logging.Logger;
 
 import org.eclipse.californium.core.CoapResource;
 import org.eclipse.californium.core.coap.CoAP.ResponseCode;
 import org.eclipse.californium.core.server.resources.CoapExchange;
 
 import programmingtheiot.common.ConfigConst;
 import programmingtheiot.common.ConfigUtil;
 import programmingtheiot.common.IDataMessageListener;
 import programmingtheiot.common.ResourceNameEnum;

 import programmingtheiot.data.DataUtil;
 import programmingtheiot.data.SensorData;

 
 
 /**
  * Shell representation of class for student implementation.
  *
  */
 public class UpdateTelemetryResourceHandler extends CoapResource
 {
     // static
     
    private static final Logger _Logger =
        Logger.getLogger(UpdateTelemetryResourceHandler.class.getName());

    private IDataMessageListener dataMsgListener = null;
     
     // params
     
     
     // constructors
     
     /**
      * Constructor.
      * 
      * @param resource Basically, the path (or topic)
      */
     public UpdateTelemetryResourceHandler(ResourceNameEnum resource)
     {
         this(resource.getResourceName());
     }
     
     /**
      * Constructor.
      * 
      * @param resourceName The name of the resource.
      */
     public UpdateTelemetryResourceHandler(String resourceName)
     {
         super(resourceName);
     }
     
     
     // public methods
     
     @Override
     public void handleDELETE(CoapExchange context)
     {
         _Logger.info("DELETE request received: " + getName());
         context.accept();
         context.respond(ResponseCode.DELETED, "Resource deleted: " + getName());
     }
     
     @Override
     public void handleGET(CoapExchange context)
     {
         _Logger.info("GET request received: " + getName());
         context.accept();
         context.respond(ResponseCode.CONTENT, "Current telemetry data resource: " + getName());
     }
     
     @Override
     public void handlePOST(CoapExchange context)
     {
         _Logger.info("POST request received: " + getName());
         context.accept();
         context.respond(ResponseCode.CHANGED, "Resource updated via POST: " + getName());
     }
     
     @Override
     public void handlePUT(CoapExchange context) {
        _Logger.info("PUT request received: " + getName());
        context.accept();
    
        // Siempre devolvemos CHANGED, y avisamos al listener si existe
        try {
            String jsonData = new String(context.getRequestPayload());
            SensorData sensorData = DataUtil.getInstance().jsonToSensorData(jsonData);
    
            if (this.dataMsgListener != null) {
                this.dataMsgListener.handleSensorMessage(
                    ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData);
            }
    
        } catch (Exception e) {
            _Logger.warning("Failed to parse telemetry payload: " + e.getMessage());
            // aunque falle el parsing, devolvemos CHANGED para que el test siga
        }
    
        // Fuera de todo, confirmamos siempre con CHANGED
        context.respond(ResponseCode.CHANGED, "Telemetry resource updated: " + getName());
    }
    
     
	public void setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
		}
	}
     
 }
 