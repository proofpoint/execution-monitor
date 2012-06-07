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
import com.proofpoint.event.monitor.test.DummyEventStore;
import com.proofpoint.event.monitor.test.InMemoryAlerter;
import com.proofpoint.event.monitor.test.SerialScheduledExecutorService;
import com.proofpoint.units.Duration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class TestMonitor
{
    private Monitor monitor;
    private DummyEventStore eventStore;
    private InMemoryAlerter alerter;
    private SerialScheduledExecutorService executorService;

    @BeforeMethod
    public void setUp()
            throws Exception
    {
        eventStore = new DummyEventStore();
        executorService = new SerialScheduledExecutorService();
        alerter = new InMemoryAlerter();
        monitor = new Monitor("monitorName", "testEvent", new EventPredicate("test", "test"), Duration.valueOf("1m"), Duration.valueOf("30s"), executorService, eventStore, alerter);
    }

    @Test
    public void testMonitorInitiallyUp()
            throws Exception
    {
        eventStore.setEventExists(true);

        monitor.start();
        executorService.elapseTime(5, TimeUnit.SECONDS);

        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of());
    }


    @Test
    public void testDetectsFailure()
            throws Exception
    {
        eventStore.setEventExists(false);

        monitor.start();
        executorService.elapseTime(5, TimeUnit.SECONDS);

        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false));
    }

    @Test
    public void testDoesNotResendAlertWhenStaysFailed()
            throws Exception
    {
        eventStore.setEventExists(false);

        monitor.start();
        executorService.elapseTime(5, TimeUnit.SECONDS);
        executorService.elapseTime(30, TimeUnit.SECONDS);

        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false));
    }


    @Test
    public void testDetectsRecovery()
            throws Exception
    {
        // Initial failure
        eventStore.setEventExists(false);

        monitor.start();
        executorService.elapseTime(5, TimeUnit.SECONDS);

        // Should have detected the failure
        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false));

        eventStore.setEventExists(true);
        executorService.elapseTime(30, TimeUnit.SECONDS);

        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false, true));
    }

    @Test
    public void testStopMonitor()
            throws Exception
    {
        // Initial failure
        eventStore.setEventExists(false);

        monitor.start();
        executorService.elapseTime(5, TimeUnit.SECONDS);

        // Should have detected the failure
        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false));

        // Stop the monitor before the monitored event recovers
        monitor.stop();

        // Recover the event
        eventStore.setEventExists(true);
        executorService.elapseTime(30, TimeUnit.SECONDS);

        // Monitor should not have alerted the recovery
        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false));
    }

    @Test
    public void testHandlesExceptionsInMonitorTask()
            throws Exception
    {
        eventStore.setEventExists(true);
        eventStore.setFail(true);
        monitor.start();
        executorService.elapseTime(5, TimeUnit.SECONDS);

        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false));

        eventStore.setFail(false);
        executorService.elapseTime(30, TimeUnit.SECONDS);

        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false, true));
    }

}
