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

import programmingtheiot.common.IActuatorDataListener;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;


/**
 * Shell representation of class for student implementation.
 *
 */
public class GetActuatorCommandResourceHandler extends CoapResource implements IActuatorDataListener
{
	// static
	private static final Logger _Logger = Logger.getLogger(GetActuatorCommandResourceHandler.class.getName());
	
	// params
	private ActuatorData actuatorData = new ActuatorData();

	
	
	// constructors
	
	/**
	 * Constructor.
	 * 
	 * @param resource Basically, the path (or topic)
	 */
	public GetActuatorCommandResourceHandler(ResourceNameEnum resource)
	{
		this(resource.getResourceName());
	}
	
	/**
	 * Constructor.
	 * 
	 * @param resourceName The name of the resource.
	 */
	public GetActuatorCommandResourceHandler(String resourceName)
	{
		super(resourceName);

		super.setObservable(true); // set the resource to be observable
	}

	// interface implementation
	@Override
	public boolean onActuatorDataUpdate(ActuatorData data)
	{
		if (data != null && this.actuatorData != null) {
			this.actuatorData.updateData(data);

			// notify all connected clients
			super.changed();

			_Logger.fine("Actuator data updated for URI: " + super.getURI() +
				": Data value = " + this.actuatorData.getValue());

			return true;
		}

		return false;
	}
	
	
	// public methods
	
	@Override
	public void handleDELETE(CoapExchange context)
	{
	}
	
	@Override
	public void handleGET(CoapExchange context)
	{
		// Log de la solicitud GET
		_Logger.info("GET request received: " + getName());
	
		// Validar el contexto (mínimamente, asegurarse de que no sea null)
		if (context == null) {
			_Logger.warning("Received null context in GET request.");
			return;
		}
	
		// Aceptar la solicitud del cliente
		context.accept();
	
		// Convertir los datos del actuador a JSON
		String jsonData = DataUtil.getInstance().actuatorDataToJson(this.actuatorData);
	
		// Enviar la respuesta con el código CONTENT y los datos JSON
		context.respond(ResponseCode.CONTENT, "Actuator command data for resource [" + getName() + "]: " + jsonData);
	}
	
	
	@Override
	public void handlePOST(CoapExchange context)
	{
	}
	
	@Override
	public void handlePUT(CoapExchange context)
	{
	}
	
	public void setDataMessageListener(IDataMessageListener listener)
	{
	}
	
}
