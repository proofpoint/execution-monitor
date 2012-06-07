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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.proofpoint.event.monitor.MonitorsResource.MonitorRepresentation;
import com.proofpoint.event.monitor.test.DummyEventStore;
import com.proofpoint.event.monitor.test.InMemoryAlerter;
import com.proofpoint.event.monitor.test.SerialScheduledExecutorService;
import com.proofpoint.jaxrs.testing.MockUriInfo;
import com.proofpoint.testing.Assertions;
import com.proofpoint.units.Duration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestMonitorsResource
{
    private Monitor fooMonitor;
    private Monitor barMonitor;
    private MonitorsResource resource;
    private SerialScheduledExecutorService executor;

    @BeforeMethod
    public void setUp()
            throws Exception
    {
        DummyEventStore store = new DummyEventStore();
        store.setEventExists(true);
        InMemoryAlerter alerter = new InMemoryAlerter();
        executor = new SerialScheduledExecutorService();
        fooMonitor = new Monitor("foo", "event", new EventPredicate("event", "true"), Duration.valueOf("1s"), Duration.valueOf("1s"), executor, store, alerter);
        barMonitor = new Monitor("bar", "event", new EventPredicate("event", "true"), Duration.valueOf("1s"), Duration.valueOf("1s"), executor, store, alerter);
        resource = new MonitorsResource(ImmutableSet.of(fooMonitor, barMonitor));

        fooMonitor.start();
        barMonitor.start();
    }

    @Test
    public void testListAllMonitors()
    {
        MockUriInfo uriInfo = new MockUriInfo(URI.create("http://example.com/v1/monitor"));
        Assertions.assertEqualsIgnoreOrder(
                resource.getAll(uriInfo),
                ImmutableList.of(MonitorsResource.MonitorRepresentation.of(fooMonitor, uriInfo), MonitorRepresentation.of(barMonitor, uriInfo)));
    }

    @Test
    public void testGetMonitor()
            throws Exception
    {
        MockUriInfo uriInfo = new MockUriInfo(URI.create("http://example.com/v1/monitor/foo"));
        executor.elapseTime(1, TimeUnit.MINUTES);

        Response response = resource.getMonitorRepresentation("foo", uriInfo);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        MonitorsResource.MonitorRepresentation representation = (MonitorsResource.MonitorRepresentation) response.getEntity();
        assertEquals(representation.getName(), "foo");
        assertEquals(representation.isOk(), true);
        assertTrue(representation.getLastChecked() != null);
    }

    @Test
    public void testGetMonitorNotFound()
            throws Exception
    {
        MockUriInfo uriInfo = new MockUriInfo(URI.create("http://example.com/v1/monitor/nothere"));
        Response response = resource.getMonitorRepresentation("nothere", uriInfo);
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
    }
}

