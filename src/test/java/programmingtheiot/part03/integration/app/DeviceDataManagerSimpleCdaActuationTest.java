package programmingtheiot.part03.integration.app;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.gda.app.DeviceDataManager;

/**
 * Basic integration test for DeviceDataManager's humidity actuation logic.
 */
public class DeviceDataManagerSimpleCdaActuationTest
{
    private static final Logger _Logger =
        Logger.getLogger(DeviceDataManagerSimpleCdaActuationTest.class.getName());

    private DeviceDataManager devDataMgr;

    @Before
    public void setUp() throws Exception
    {
        devDataMgr = new DeviceDataManager();
        devDataMgr.startManager();
    }

    @After
    public void tearDown() throws Exception
    {
        devDataMgr.stopManager();
    }

    @Test
    public void testHumidityActuationLogic()
    {
        ConfigUtil cfgUtil = ConfigUtil.getInstance();

        float nominalVal = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "nominalHumiditySetting");
        float lowVal     = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierFloor");
        float highVal    = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierCeiling");
        int delay        = cfgUtil.getInteger(ConfigConst.GATEWAY_DEVICE, "humidityMaxTimePastThreshold");

        SensorData sd = new SensorData();
        sd.setName("Test Humidity Sensor");
        sd.setLocationID("constraineddevice001");
        sd.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);

        // Send nominal value twice - no actuation expected
        sd.setValue(nominalVal);
        devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitSeconds(2);
        devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitSeconds(2);

        // Send low value twice, spaced by delay - expect actuation ON
        sd.setValue(lowVal - 2);
        devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitSeconds(delay + 1);
        sd.setValue(lowVal - 1);
        devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitSeconds(delay + 1);

        // Send value above floor - should not actuate, just logging
        sd.setValue(lowVal + 1);
        devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitSeconds(delay + 1);

        // Send nominal value again twice spaced by delay - expect actuation OFF
        sd.setValue(nominalVal);
        devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitSeconds(delay + 1);
        devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitSeconds(delay + 1);
    }

    private void waitSeconds(int seconds)
    {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
