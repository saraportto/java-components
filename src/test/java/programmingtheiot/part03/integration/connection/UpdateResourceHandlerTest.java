package programmingtheiot.part03.integration.connection;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import programmingtheiot.common.DefaultDataMessageListener;
import programmingtheiot.data.DataUtil;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.CoapClientConnector;
import programmingtheiot.gda.connection.handlers.UpdateSystemPerformanceResourceHandler;
import programmingtheiot.gda.connection.handlers.UpdateTelemetryResourceHandler;
public class UpdateResourceHandlerTest
{
    private static final Logger _Logger =
            Logger.getLogger(UpdateResourceHandlerTest.class.getName());

    public static final int DEFAULT_TIMEOUT = 5;
    public static final boolean USE_DEFAULT_RESOURCES = true;

    private static CoapServerGateway _ServerGateway;
    private static DefaultDataMessageListener dataMsgListener;

    private CoapClientConnector coapClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        dataMsgListener = new DefaultDataMessageListener();
        _ServerGateway = new CoapServerGateway(dataMsgListener);

        // System Performance
        UpdateSystemPerformanceResourceHandler sysHandler =
            new UpdateSystemPerformanceResourceHandler(
                ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE.getResourceName()
            );
        sysHandler.setDataMessageListener(dataMsgListener);

        // ahora sólo pasamos el enum
        _ServerGateway.addResource(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);

        // Telemetry
        UpdateTelemetryResourceHandler telHandler =
            new UpdateTelemetryResourceHandler(
                ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE.getResourceName()
            );
        telHandler.setDataMessageListener(dataMsgListener);

        _ServerGateway.addResource(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);

        assertTrue("No se pudo iniciar el servidor CoAP",
            _ServerGateway.startServer()
        );
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
        assertTrue("No se pudo detener el servidor CoAP",
            _ServerGateway.stopServer()
        );
    }

    @Before
    public void setUp() throws Exception
    {
        this.coapClient = new CoapClientConnector();
        this.coapClient.setDataMessageListener(dataMsgListener);
    }

    @Test
    public void testSystemPerformancePutMessage()
    {
        SystemPerformanceData spData = new SystemPerformanceData();
        String jsonData = DataUtil.getInstance()
            .systemPerformanceDataToJson(spData);

        boolean result = this.coapClient.sendPutRequest(
            ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE,
            null,
            USE_DEFAULT_RESOURCES,
            jsonData,
            DEFAULT_TIMEOUT
        );
        assertTrue("El PUT de SystemPerformanceData falló", result);
    }

    @Test
    public void testTelemetryPutMessage()
    {
        SensorData sData = new SensorData();
        String jsonData = DataUtil.getInstance()
            .sensorDataToJson(sData);

        boolean result = this.coapClient.sendPutRequest(
            ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE,
            null,
            USE_DEFAULT_RESOURCES,
            jsonData,
            DEFAULT_TIMEOUT
        );
        assertTrue("El PUT de SensorData (telemetría) falló", result);
    }
}
