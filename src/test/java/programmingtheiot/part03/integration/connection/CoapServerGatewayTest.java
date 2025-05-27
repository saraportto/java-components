/**
 * 
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 by Andrew D. King
 */ 

package programmingtheiot.part03.integration.connection;

import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.WebLink;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.DefaultDataMessageListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.gda.connection.*;

import programmingtheiot.gda.connection.CoapServerGateway;


/**
 * This test case class contains very basic integration tests for
 * CoapServerGateway. It should not be considered complete,
 * but serve as a starting point for the student implementing
 * additional functionality within their Programming the IoT
 * environment.
 *
 */

 public class CoapServerGatewayTest
 {
	 private static final Logger _Logger = Logger.getLogger(CoapServerGatewayTest.class.getName());
	 private static final int DEFAULT_TIMEOUT = 120000; // 2 minutos
 
	 private CoapServerGateway csg = null;
 
	 @Test
	 public void testRunSimpleCoapServerGatewayIntegration()
	 {
		 try {
			 String url = "coap://localhost:5683";
 
			 // Crear e iniciar el servidor
			 this.csg = new CoapServerGateway(); // Constructor sin args: crea recursos por defecto
			 this.csg.startServer();
 
			 // Crear cliente y ejecutar descubrimiento
			 CoapClient clientConn = new CoapClient(url);
			 Set<WebLink> wlSet = clientConn.discover();
 
			 if (wlSet != null) {
				 for (WebLink wl : wlSet) {
					 _Logger.info(" --> WebLink: " + wl.getURI() + ". Attributes: " + wl.getAttributes());
				 }
			 }
 
			 // Pausar para permitir pruebas adicionales manuales (GET, POST con cf-client)
			 Thread.sleep(DEFAULT_TIMEOUT);
 
			 this.csg.stopServer();
		 } catch (Exception e) {
			 _Logger.severe("Exception during CoAP integration test: " + e.getMessage());
			 e.printStackTrace();
		 }
	 }
 }