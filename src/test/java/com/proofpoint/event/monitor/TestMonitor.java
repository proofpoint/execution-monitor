package com.proofpoint.event.monitor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.proofpoint.event.monitor.executors.SerialScheduledExecutorService;
import com.proofpoint.units.Duration;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Maps;

import java.util.List;
import java.util.Map;

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
        executorService.elapseTime(Duration.valueOf("5s"));

        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of());
    }


    @Test
    public void testDetectsFailure()
            throws Exception
    {
        eventStore.setEventExists(false);

        monitor.start();
        executorService.elapseTime(Duration.valueOf("5s"));

        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false));
    }

    @Test
    public void testDoesNotResendAlertWhenStaysFailed()
            throws Exception
    {
        eventStore.setEventExists(false);

        monitor.start();
        executorService.elapseTime(Duration.valueOf("5s"));
        executorService.elapseTime(Duration.valueOf("30s"));

        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false));
    }


    @Test
    public void testDetectsRecovery()
            throws Exception
    {
        // Initial failure
        eventStore.setEventExists(false);

        monitor.start();
        executorService.elapseTime(Duration.valueOf("5s"));

        // Should have detected the failure
        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false));

        eventStore.setEventExists(true);
        executorService.elapseTime(Duration.valueOf("30s"));

        assertEquals(alerter.getList(monitor.getName()), ImmutableList.of(false, true));
    }

    private class DummyEventStore
        implements EventStore
    {
        private boolean eventExists = false;

        public void setEventExists(boolean eventExists)
        {
            this.eventExists = eventExists;
        }

        @Override
        public boolean recentEventExists(String eventType, EventPredicate eventPredicate, DateTime searchLimit)
        {
            return eventExists;
        }
    }

    private class InMemoryAlerter
        implements Alerter
    {
        private final Map<String, List<Boolean>> alerts = Maps.newHashMap();

        public List<Boolean> getList(String monitorName)
        {
            List<Boolean> list = alerts.get(monitorName);
            if (list == null) {
                list = Lists.newArrayList();
                alerts.put(monitorName, list);
            }
            return list;
        }

        @Override
        public void failed(Monitor monitor, String description)
        {
            getList(monitor.getName()).add(false);
        }

        @Override
        public void recovered(Monitor monitor, String description)
        {
            getList(monitor.getName()).add(true);
        }
    }
}
