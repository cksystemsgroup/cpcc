// This code is part of the CPCC-NG project.
//
// Copyright (c) 2013 Clemens Krainer <clemens.krainer@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software Foundation,
// Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

package cpcc.vvrte.services.js;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.ServiceResources;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.assertj.core.api.Fail;
import org.hibernate.internal.util.SerializationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;

import cpcc.vvrte.base.VirtualVehicleMappingDecision;
import cpcc.vvrte.entities.VirtualVehicle;
import cpcc.vvrte.entities.VirtualVehicleState;

/**
 * JavascriptServiceTest
 */
// @Test(singleThreaded = true)
public class JavascriptServiceTest
{
    private PerthreadManager perthreadManager;
    private HibernateSessionManager sessionManager;
    private ServiceResources serviceResources;
    private Logger logger;

    @BeforeEach
    public void setUp()
    {
        perthreadManager = mock(PerthreadManager.class);
        sessionManager = mock(HibernateSessionManager.class);
        logger = mock(Logger.class);

        serviceResources = mock(ServiceResources.class);
        when(serviceResources.getService(PerthreadManager.class)).thenReturn(perthreadManager);
        when(serviceResources.getService(HibernateSessionManager.class)).thenReturn(sessionManager);
    }

    @Test
    public void shouldExecuteSimpleJS() throws InterruptedException, IOException
    {
        VirtualVehicle vv = new VirtualVehicle();
        vv.setId(123);
        vv.setCode("function f(x){return x+1} f(7)");
        vv.setApiVersion(1);
        vv.setUuid("27369070-a042-11e5-a35d-0f12a6b8b54e");

        JavascriptService jss = new JavascriptServiceImpl(logger, serviceResources, null);
        JavascriptWorker sut = jss.createWorker(vv, false);
        MyWorkerStateListener workerListener = new MyWorkerStateListener();
        sut.addStateListener(workerListener);
        sut.run();

        assertThat(sut.getResult()).isNotNull().isEqualTo("undefined");
        assertThat(sut.getWorkerState()).isNotNull().isEqualTo(VirtualVehicleState.FINISHED);
        assertThat(workerListener.getWorker()).isNotNull().isEqualTo(sut);
        assertThat(workerListener.getState()).isNotNull().isEqualTo(VirtualVehicleState.FINISHED);

        verify(sessionManager, times(3)).commit();
        verify(perthreadManager).cleanup();
    }

    @Test
    public void shouldNotExecuteNaughtyScript() throws InterruptedException, IOException
    {
        VirtualVehicle vv = new VirtualVehicle();
        vv.setId(123);
        vv.setCode("java.lang.System.currentTimeMillis()");
        vv.setApiVersion(1);
        vv.setUuid("599b7ada-a042-11e5-98b1-9767a13e60f2");

        JavascriptService jss = new JavascriptServiceImpl(logger, serviceResources, null);
        JavascriptWorker sut = jss.createWorker(vv, false);
        sut.run();

        assertThat(sut.getWorkerState()).isNotNull().isEqualTo(VirtualVehicleState.DEFECTIVE);
        assertThat(sut.getResult()).isNotNull().startsWith(
            "TypeError: Cannot call property currentTimeMillis in object");

        verify(sessionManager, times(3)).commit();
        verify(perthreadManager).cleanup();
    }

    @Test
    public void shouldDenyWrongApiVersion() throws InterruptedException, IOException
    {
        VirtualVehicle vv = new VirtualVehicle();
        vv.setId(123);
        vv.setCode("function f(x){return x+1} f(7)");
        vv.setApiVersion(1000);
        vv.setUuid("599bf294-a042-11e5-a552-abf9c5301b8b");

        JavascriptService jss = new JavascriptServiceImpl(logger, serviceResources, null);

        try
        {
            jss.createWorker(vv, false);
            Fail.failBecauseExceptionWasNotThrown(IOException.class);
        }
        catch (IOException e)
        {
            assertThat(e.getMessage()).isEqualTo("Can not handle API version 1000");
        }

        verifyNoInteractions(sessionManager);
        verifyNoInteractions(perthreadManager);
    }

    @Test
    public void shouldHandleVvRte() throws IOException, InterruptedException
    {
        MyBuiltInFunctions functions = new MyBuiltInFunctions();
        JavascriptService jss = new JavascriptServiceImpl(logger, serviceResources, functions);
        jss.addAllowedClassRegex("cpcc.vvrte.services.js.JavascriptServiceTest\\$MyBuiltInFunctions");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stdOut = new PrintStream(out, true);
        VvRteFunctions.setStdOut(stdOut);

        InputStream scriptStream = this.getClass().getResourceAsStream("simple-vv.js");
        String script = IOUtils.toString(scriptStream, "UTF-8");
        assertThat(script).isNotNull().isNotEmpty();

        VirtualVehicle vv = new VirtualVehicle();
        vv.setId(123);
        vv.setCode(script);
        vv.setApiVersion(1);
        vv.setUuid("599c6710-a042-11e5-a0fb-83f9ea30f21e");

        functions.setMigrate(true);
        JavascriptWorker sut = jss.createWorker(vv, false);
        sut.run();

        // System.out.println("shouldHandleVvRte() result1: '" + sut.getResult() + "'");
        // System.out.println("shouldHandleVvRte() output1: '" + out.toString("UTF-8") + "'");
        assertThat(sut.getWorkerState()).isNotNull().isEqualTo(VirtualVehicleState.INTERRUPTED);
        InputStream resultStream = this.getClass().getResourceAsStream("simple-vv-expected-result-1.txt");
        String expectedResult = IOUtils.toString(resultStream, "UTF-8");
        assertThat(out.toString("UTF-8")).isNotNull().isEqualTo(expectedResult);

        functions.setMigrate(false);
        byte[] snapshot = sut.getSnapshot();
        vv.setContinuation(snapshot);
        sut = jss.createWorker(vv, true);
        sut.run();
        stdOut.flush();
        assertThat(sut.getWorkerState()).isNotNull().isEqualTo(VirtualVehicleState.FINISHED);

        // System.out.println("shouldHandleVvRte() result2: '" + x.getResult() + "'");
        // System.out.println("shouldHandleVvRte() output2: '" + out.toString("UTF-8") + "'");
        resultStream = this.getClass().getResourceAsStream("simple-vv-expected-result-2.txt");
        expectedResult = IOUtils.toString(resultStream, "UTF-8");
        assertThat(out.toString("UTF-8")).isNotNull().isEqualTo(expectedResult);

        verify(sessionManager, times(6)).commit();
        verify(perthreadManager, times(2)).cleanup();
    }

    @Test
    public void shouldHandleVvStorage() throws IOException
    {
        MyBuiltInFunctions functions = new MyBuiltInFunctions();
        JavascriptService jss = new JavascriptServiceImpl(logger, serviceResources, functions);
        jss.addAllowedClassRegex("cpcc.vvrte.services.js.JavascriptServiceTest\\$MyBuiltInFunctions");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stdOut = new PrintStream(out, true);
        VvRteFunctions.setStdOut(stdOut);

        InputStream scriptStream = this.getClass().getResourceAsStream("storage-test.js");
        String script = IOUtils.toString(scriptStream, "UTF-8");
        assertThat(script).isNotNull().isNotEmpty();

        VirtualVehicle vv = new VirtualVehicle();
        vv.setId(123);
        vv.setCode(script);
        vv.setApiVersion(1);
        vv.setUuid("599cda7e-a042-11e5-a5b4-ffed137d569e");

        functions.setMigrate(false);
        JavascriptWorker sut = jss.createWorker(vv, false);
        sut.run();
        stdOut.flush();

        // System.out.println("shouldHandleVvRte() result: '" + x.getResult() + "'");
        // System.out.println("shouldHandleVvRte() output: '" + out.toString("UTF-8") + "'");
        assertThat(sut.getWorkerState()).isNotNull().isEqualTo(VirtualVehicleState.FINISHED);

        InputStream resultStream = this.getClass().getResourceAsStream("storage-test-expected-result.txt");
        String expectedResult = IOUtils.toString(resultStream, "UTF-8");

        assertThat(out.toString("UTF-8")).isNotNull().isEqualTo(expectedResult);

        verify(sessionManager, times(3)).commit();
        verify(perthreadManager).cleanup();
    }

    static Stream<Arguments> emptyScriptDataProvider()
    {
        return Stream.of(
            arguments((String) null),
            arguments(""),
            arguments("\n"),
            arguments("\n\n\n\n\r\n"));
    }

    @ParameterizedTest
    @MethodSource("emptyScriptDataProvider")
    public void shouldHandleEmptyScript(String script) throws IOException, InterruptedException
    {
        VirtualVehicle vv = new VirtualVehicle();
        vv.setId(123);
        vv.setCode(script);
        vv.setApiVersion(1);
        vv.setUuid("599d4f4a-a042-11e5-a1d0-474802529ab0");

        JavascriptService jss = new JavascriptServiceImpl(logger, serviceResources, null);
        JavascriptWorker sut = jss.createWorker(vv, false);
        sut.run();
        assertThat(sut.getWorkerState()).isNotNull().isEqualTo(VirtualVehicleState.DEFECTIVE);

        verify(sessionManager, times(3)).commit();
        verify(perthreadManager).cleanup();
    }

    @ParameterizedTest
    @MethodSource("emptyScriptDataProvider")
    public void shouldCompileEmptyScript(String script) throws IOException
    {
        JavascriptService jss = new JavascriptServiceImpl(logger, serviceResources, null);
        Object[] result = jss.codeVerification(script, 1);
        assertThat(result).isNotNull().hasSize(0);
    }

    @Test
    public void shouldHandleNullContinuation() throws InterruptedException, IOException
    {
        VirtualVehicle vv = new VirtualVehicle();
        vv.setId(123);
        vv.setCode(null);
        vv.setApiVersion(1);
        vv.setContinuation(null);
        vv.setUuid("599d9932-a042-11e5-911b-f785d3884ce0");

        JavascriptService jss = new JavascriptServiceImpl(logger, serviceResources, null);
        JavascriptWorker sut = jss.createWorker(vv, true);
        sut.run();
        assertThat(sut.getWorkerState()).isNotNull().isEqualTo(VirtualVehicleState.DEFECTIVE);

        verify(sessionManager, times(3)).commit();
        verify(perthreadManager).cleanup();
    }

    @Test
    public void shouldReturnScriptWithApiPrefix() throws IOException
    {
        String script = "function f(x){return x+1} f(7)";

        VirtualVehicle vv = new VirtualVehicle();
        vv.setId(123);
        vv.setCode(script);
        vv.setApiVersion(1);
        vv.setUuid("599ddad2-a042-11e5-ab97-e3af3b4f34e8");

        JavascriptService jss = new JavascriptServiceImpl(logger, serviceResources, null);
        JavascriptWorker sut = jss.createWorker(vv, false);
        assertThat(sut.getScript()).isNotNull().endsWith(script + "\n})();");

        verifyNoInteractions(sessionManager);
        verifyNoInteractions(perthreadManager);
    }

    @Test
    public void shouldNotCompileErroneousScript() throws IOException
    {
        String script = "var x = 0;\nx x x";
        JavascriptService jss = new JavascriptServiceImpl(logger, serviceResources, null);
        Object[] result = jss.codeVerification(script, 1);
        assertThat(result).isNotNull().hasSize(4);

        Integer column = (Integer) result[0];
        Integer line = (Integer) result[1];
        String errorMessage = (String) result[2];
        String sourceLine = (String) result[3];

        assertThat(column).isNotNull().isEqualTo(4);
        assertThat(line).isNotNull().isEqualTo(2);
        assertThat(errorMessage).isNotNull().isEqualTo("missing ; before statement");
        assertThat(sourceLine).isNotNull().isEqualTo("x x x");

        verifyNoInteractions(sessionManager);
        verifyNoInteractions(perthreadManager);
    }

    @Test
    public void shouldCompileProperScript() throws IOException
    {
        String script = "function f(x){return x+1} f(7)";
        JavascriptService jss = new JavascriptServiceImpl(logger, serviceResources, null);
        Object[] result = jss.codeVerification(script, 1);
        assertThat(result).isNotNull().hasSize(0);

        verifyNoInteractions(perthreadManager);
    }

    /**
     * MyBuiltInFunctions
     */
    private static class MyBuiltInFunctions implements BuiltInFunctions
    {

        private boolean migrate = false;

        private Map<String, ScriptableObject> storageMap = new HashMap<String, ScriptableObject>();

        /**
         * @param migrate the migrate to set
         */
        public void setMigrate(boolean migrate)
        {
            this.migrate = migrate;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<ScriptableObject> listSensors()
        {
            // TODO fix this.
            return listActiveSensors();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<ScriptableObject> listActiveSensors()
        {
            // System.out.println("listSensors start");

            NativeObject barometer = new NativeObject();
            barometer.put("name", barometer, "barometer");

            NativeObject camera = new NativeObject();
            camera.put("name", camera, "camera");

            NativeObject thermometer = new NativeObject();
            thermometer.put("name", thermometer, "thermometer");

            // NativeArray sensors = new NativeArray(3);
            List<ScriptableObject> sensors = new ArrayList<ScriptableObject>();
            sensors.add(barometer);
            sensors.add(thermometer);
            sensors.add(camera);

            // System.out.println("listSensors end");
            return sensors;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScriptableObject getSensor(String name)
        {
            NativeObject sensor = new NativeObject();
            sensor.put("name", sensor, name);

            // System.out.println("getSensor");
            return sensor;
        }

        /**
         * {@inheritDoc}
         */
        //        @Override
        private ScriptableObject getSensorValue(ScriptableObject sensor)
        {
            NativeObject sensorValue = new NativeObject();
            sensorValue.put("name", sensorValue, sensor.get("name"));
            sensorValue.put("value", sensorValue, "value");

            System.out.println("getSensorValue for " + sensor.get("name"));
            return sensorValue;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void executeTask(ScriptableObject managementParameters, ScriptableObject taskParameters)
        {
            // System.out.println("executeTask1");
            if (!verifyTaskParameters(taskParameters))
            {
                managementParameters.put("repeat", managementParameters, Boolean.FALSE);
                return;
            }

            // System.out.println("executeTask2");

            Number sequence = (Number) managementParameters.get("sequence");
            if (sequence.intValue() == 0)
            {
                // TODO decide for migration or not.
                // TODO migration: initiate migration by throwing CP-Exception.

                if (migrate)
                {
                    // System.out.println("migration");
                    Context cx = Context.enter();
                    try
                    {
                        ContinuationPending cp = cx.captureContinuation();
                        VirtualVehicleMappingDecision decision = new VirtualVehicleMappingDecision();
                        decision.setMigration(true);
                        cp.setApplicationState(new ApplicationState(decision));
                        throw cp;
                    }
                    finally
                    {
                        Context.exit();
                    }
                }

                // System.out.println("no migration");

                // TODO no migration: schedule task and wait for completion.

                managementParameters.put("valid", managementParameters, Boolean.TRUE);
                managementParameters.put("sequence", managementParameters, Integer.valueOf(sequence.intValue() + 1));

                NativeArray sensors = (NativeArray) taskParameters.get("sensors");
                NativeArray sensorValues = new NativeArray(sensors.getLength());

                for (int k = 0; k < sensors.getLength(); ++k)
                {
                    NativeObject s = (NativeObject) sensors.get(k);
                    sensorValues.put(k, sensorValues, getSensorValue(s));
                }

                managementParameters.put("sensorValues", managementParameters, sensorValues);
                managementParameters.put("repeat", managementParameters, Boolean.TRUE);
                return;
            }

            // String type = (String) taskParameters.get("type");
            Number tolerance = (Number) taskParameters.get("tolerance");
            tolerance.doubleValue();

            // NativeArray sensors = (NativeArray) taskParameters.get("sensors");

            // TODO Auto-generated method stub
            managementParameters.put("repeat", managementParameters, Boolean.FALSE);
            return;
        }

        private boolean verifyTaskParameters(ScriptableObject taskParameters)
        {
            Object sensors = taskParameters.get("sensors");
            Object type = taskParameters.get("type");

            if (sensors == null || !(sensors instanceof NativeArray) || ((NativeArray) sensors).getLength() == 0)
            {
                return false;
            }

            if (type == null || !(type instanceof String) || !"point".equalsIgnoreCase((String) type))
            {
                return false;
            }

            // !('sensors' in taskParams) || !('length' in taskParams.sensors) || taskParams.sensors.length == 0 || taskParams.type)

            // TODO Auto-generated method stub
            return true;
        }

        @Override
        public ScriptableObject loadObject(String name)
        {
            // System.out.println("loadObject " + name);
            return storageMap.get(name);
        }

        @Override
        public void storeObject(String name, ScriptableObject obj)
        {
            // System.out.println("storeObject " + name);
            storageMap.put(name, (ScriptableObject) SerializationHelper.clone(obj));
        }

        @Override
        public List<String> listObjects(String pattern)
        {
            // System.out.println("listObjects " + pattern);
            List<String> result = new ArrayList<String>();
            for (String entry : storageMap.keySet())
            {
                if (entry.matches(pattern))
                {
                    result.add(entry);
                }
            }
            return result;
        }

        @Override
        public void removeObject(String name)
        {
            // System.out.println("removeObject " + name);
            storageMap.remove(name);
        }
    }

    /**
     * MyWorkerStateListener
     */
    private static class MyWorkerStateListener implements JavascriptWorkerStateListener
    {
        private JavascriptWorker worker = null;
        private VirtualVehicleState state = null;

        /**
         * {@inheritDoc}
         */
        @Override
        public void notify(JavascriptWorker worker, VirtualVehicleState state)
        {
            this.worker = worker;
            this.state = state;
        }

        /**
         * @return the worker
         */
        public JavascriptWorker getWorker()
        {
            return worker;
        }

        /**
         * @return the state
         */
        public VirtualVehicleState getState()
        {
            return state;
        }
    }
}
