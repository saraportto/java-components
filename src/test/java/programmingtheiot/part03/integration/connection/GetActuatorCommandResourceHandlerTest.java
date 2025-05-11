package programmingtheiot.part03.integration.connection;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.gda.connection.handlers.GetActuatorCommandResourceHandler;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;

public class GetActuatorCommandResourceHandlerTest
{
	private CoapServer coapServer;
	private static final int TEST_COAP_PORT = 5683;
	private static final String RESOURCE_NAME = "GetActuatorCommand";

	@Before
	public void setUp() throws Exception
	{
		InetSocketAddress bindToAddress = new InetSocketAddress(TEST_COAP_PORT);
		CoapEndpoint udpEndpoint = new CoapEndpoint.Builder()
			.setInetSocketAddress(bindToAddress)
			.build();

		this.coapServer = new CoapServer();
		this.coapServer.add(new GetActuatorCommandResourceHandler(RESOURCE_NAME));
		this.coapServer.addEndpoint(udpEndpoint);
		this.coapServer.start();
		Thread.sleep(1000); // Pequeña espera para asegurar que el servidor esté listo
	}

	@After
	public void tearDown() throws Exception
	{
		if (this.coapServer != null) {
			this.coapServer.stop();
			this.coapServer.destroy();
		}
	}

	@Test
	public void testHandleGetRequest() throws ConnectorException, IOException
	{
		String uri = "coap://localhost:" + TEST_COAP_PORT + "/" + RESOURCE_NAME;
		CoapClient client = new CoapClient(uri);
		var response = client.get();

		assertNotNull("La respuesta no debe ser null", response);
		assertEquals("El código de respuesta debe ser 2.05 CONTENT",
			ResponseCode.CONTENT, response.getCode());
		assertTrue("El payload debe contener el nombre del recurso",
			response.getResponseText().contains(RESOURCE_NAME));
		client.shutdown();
	}
}
