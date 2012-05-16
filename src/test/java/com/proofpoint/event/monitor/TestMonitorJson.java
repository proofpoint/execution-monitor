package com.proofpoint.event.monitor;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.proofpoint.units.Duration;
import org.testng.annotations.Test;

import java.util.Map;

import static com.proofpoint.json.JsonCodec.mapJsonCodec;
import static com.proofpoint.testing.EquivalenceTester.equivalenceTester;
import static org.testng.Assert.assertEquals;

public class TestMonitorJson
{
    @Test
    public void testEquivalence()
            throws Exception
    {
        equivalenceTester()
                .addEquivalentGroup(new MonitorJson("foo", "foo", Duration.valueOf("1m"), Duration.valueOf("1m")), new MonitorJson("foo", "foo", Duration.valueOf("1m"), Duration.valueOf("1m")))
                .addEquivalentGroup(new MonitorJson("bar", "foo", Duration.valueOf("1m"), Duration.valueOf("1m")), new MonitorJson("bar", "foo", Duration.valueOf("1m"), Duration.valueOf("1m")))
                .addEquivalentGroup(new MonitorJson("foo", "bar", Duration.valueOf("1m"), Duration.valueOf("1m")), new MonitorJson("foo", "bar", Duration.valueOf("1m"), Duration.valueOf("1m")))
                .addEquivalentGroup(new MonitorJson("foo", "foo", Duration.valueOf("2d"), Duration.valueOf("1m")), new MonitorJson("foo", "foo", Duration.valueOf("2d"), Duration.valueOf("1m")))
                .addEquivalentGroup(new MonitorJson("foo", "foo", Duration.valueOf("1m"), Duration.valueOf("2d")), new MonitorJson("foo", "foo", Duration.valueOf("1m"), Duration.valueOf("2d")))
                .addEquivalentGroup(new MonitorJson(null, "foo", Duration.valueOf("1m"), Duration.valueOf("1m")), new MonitorJson(null, "foo", Duration.valueOf("1m"), Duration.valueOf("1m")))
                .addEquivalentGroup(new MonitorJson("foo", null, Duration.valueOf("1m"), Duration.valueOf("1m")), new MonitorJson("foo", null, Duration.valueOf("1m"), Duration.valueOf("1m")))
                .addEquivalentGroup(new MonitorJson("foo", "foo", null, Duration.valueOf("1m")), new MonitorJson("foo", "foo", null, Duration.valueOf("1m")))
                .check();
    }

    @Test
    public void testDefaultCheckInterval()
            throws Exception
    {
        MonitorJson hasDefaultCheckInterval = new MonitorJson("a", "b", Duration.valueOf("1s"), null);
        assertEquals(hasDefaultCheckInterval.getCheckInterval(), Duration.valueOf("30s"));
    }

    @Test
    public void testDeserializeFromJson()
            throws Exception
    {
        Map<String, MonitorJson> expected = ImmutableMap.of(
                "foo", new MonitorJson("foo", "bar", Duration.valueOf("1m"), Duration.valueOf("30s")),
                "baz", new MonitorJson("baz", "bat", Duration.valueOf("2d"), Duration.valueOf("1h")));

        String json = Resources.toString(Resources.getResource("monitor.json"), Charsets.UTF_8);

        Map<String, MonitorJson> actual = mapJsonCodec(String.class, MonitorJson.class).fromJson(json);

        assertEquals(actual, expected);
    }
}
