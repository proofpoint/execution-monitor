/*
 * Copyright 2011 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.event.monitor;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import com.proofpoint.configuration.ConfigurationFactory;
import com.proofpoint.configuration.ConfigurationModule;
import com.proofpoint.discovery.client.DiscoveryModule;
import com.proofpoint.event.monitor.test.DummyEventStore;
import com.proofpoint.event.monitor.test.InMemoryAlerter;
import com.proofpoint.event.monitor.test.SerialScheduledExecutorService;
import com.proofpoint.http.client.ApacheHttpClient;
import com.proofpoint.http.client.FullJsonResponseHandler.JsonResponse;
import com.proofpoint.http.client.HttpClient;
import com.proofpoint.http.server.testing.TestingHttpServer;
import com.proofpoint.http.server.testing.TestingHttpServerModule;
import com.proofpoint.jaxrs.JaxrsModule;
import com.proofpoint.jmx.JmxHttpModule;
import com.proofpoint.jmx.JmxModule;
import com.proofpoint.json.JsonCodec;
import com.proofpoint.json.JsonModule;
import com.proofpoint.node.testing.TestingNodeModule;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import javax.management.MBeanServer;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.proofpoint.http.client.FullJsonResponseHandler.createFullJsonResponseHandler;
import static com.proofpoint.http.client.Request.Builder.prepareGet;
import static com.proofpoint.testing.Assertions.assertEqualsIgnoreOrder;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

public class TestServer
{
    private static final JsonCodec<Map<String, Object>> MONITOR_CODEC = JsonCodec.mapJsonCodec(String.class, Object.class);
    private static final JsonCodec<List<Map<String, Object>>> MONITOR_LIST_CODEC = JsonCodec.listJsonCodec(JsonCodec.mapJsonCodec(String.class, Object.class));

    private HttpClient client;
    private TestingHttpServer server;
    private DummyEventStore store;
    private SerialScheduledExecutorService executorService;

    @BeforeMethod
    public void setup()
            throws Exception
    {
        ImmutableMap<String, String> config = ImmutableMap.of("monitor.file", "src/test/resources/monitor.json");

        final MBeanServer mockMBeanServer = mock(MBeanServer.class);
        store = new DummyEventStore();
        executorService = new SerialScheduledExecutorService();

        Injector injector = Guice.createInjector(
                new TestingNodeModule(),
                new TestingHttpServerModule(),
                new JsonModule(),
                new JaxrsModule(),
                new JmxHttpModule(),
                Modules.override(new JmxModule()).with(
                        new Module()
                        {
                            @Override
                            public void configure(Binder binder)
                            {
                                binder.bind(MBeanServer.class).toInstance(mockMBeanServer);
                            }
                        }),
                new DiscoveryModule(),
                Modules.override(new MainModule()).with(
                        new Module()
                        {
                            @Override
                            public void configure(Binder binder)
                            {
                                binder.bind(Alerter.class).to(InMemoryAlerter.class).in(Scopes.SINGLETON);
                                binder.bind(EventStore.class).toInstance(store);
                                binder.bind(ScheduledExecutorService.class).annotatedWith(MonitorExecutorService.class).toInstance(executorService);
                            }
                        }),
                new ConfigurationModule(new ConfigurationFactory(config)));

        server = injector.getInstance(TestingHttpServer.class);

        Map<String, Monitor> monitors = newHashMap();
        for (Monitor monitor : newArrayList(injector.getInstance(Key.get(new TypeLiteral<Set<Monitor>>(){})))) {
            monitors.put(monitor.getName(), monitor);
        }

        Assert.assertEquals(monitors.size(), 2);


        server.start();
        client = new ApacheHttpClient();
    }

    @AfterMethod
    public void teardown()
            throws Exception
    {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testListMonitors()
            throws Exception
    {
        JsonResponse<List<Map<String, Object>>> response = client.execute(prepareGet().setUri(urlFor("/v1/monitor")).build(), createFullJsonResponseHandler(MONITOR_LIST_CODEC));
        assertEquals(response.getStatusCode(), Status.OK.getStatusCode());
        Iterable<String> entryNames = Iterables.transform(response.getValue(), new Function<Map<String, Object>, String>()
        {
            @Override
            public String apply(@Nullable Map<String, Object> monitor)
            {
                return (String) monitor.get("name");
            }
        });

        assertEqualsIgnoreOrder(entryNames, ImmutableList.of("foo", "baz"));
    }

    @Test
    public void testGetMonitorDetail()
            throws Exception
    {
        JsonResponse<Map<String,Object>> response = client.execute(prepareGet().setUri(urlFor("/v1/monitor/foo")).build(), createFullJsonResponseHandler(MONITOR_CODEC));
        assertEquals(response.getStatusCode(), Status.OK.getStatusCode());
        Map<String, Object> actual = response.getValue();

        assertEquals(actual.get("name"), "foo");
        assertEquals(actual.get("ok"), true);
        assertEquals(actual.get("self"), urlFor("/v1/monitor/foo").toString());
    }

    @Test
    public void testGetMonitorState()
            throws Exception
    {
        JsonResponse<Map<String,Object>> response = client.execute(prepareGet().setUri(urlFor("/v1/monitor/foo")).build(), createFullJsonResponseHandler(MONITOR_CODEC));
        assertEquals(response.getStatusCode(), Status.OK.getStatusCode());
        Map<String, Object> before = response.getValue();
        assertEquals(before.get("ok"), true);

        failFooMonitor();

        response = client.execute(prepareGet().setUri(urlFor("/v1/monitor/foo")).build(), createFullJsonResponseHandler(MONITOR_CODEC));
        assertEquals(response.getStatusCode(), Status.OK.getStatusCode());
        Map<String, Object> after = response.getValue();

        assertEquals(after.get("ok"), false);
    }

    private void failFooMonitor()
    {
        store.setEventExists(false);
        executorService.elapseTime(1, TimeUnit.HOURS);
    }

    private URI urlFor(String path)
    {
        return server.getBaseUrl().resolve(path);
    }
}
