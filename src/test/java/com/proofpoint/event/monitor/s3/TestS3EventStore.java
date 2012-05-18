package com.proofpoint.event.monitor.s3;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.proofpoint.event.monitor.Event;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.Iterator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestS3EventStore
{
    @Test
    public void testJsonDecode()
            throws Exception
    {
        InputStream snappyJsonStream = Resources.newInputStreamSupplier(Resources.getResource("example.json.snappy")).getInput();

        Iterator<Event> eventIterator = S3EventStore.getEventIterator(snappyJsonStream, new ObjectMapper());

        assertTrue(eventIterator.hasNext());

        Event first = eventIterator.next();
        assertEquals(first.getType(), "Blackhole");
        assertEquals(first.getUuid(), "a1b4d730-564a-4570-944f-eb6a40b0fcfc");
        assertEquals(first.getHost(), "ip-10-94-13-48");
        assertEquals(first.getTimestamp(), DateTime.parse("2012-05-14T23:56:45.147Z"));
        assertEquals(first.getData(), ImmutableMap.of(
                "method","POST",
                "uri","http://www.proofpoint.com/",
                "entity", "this is a test"));

        assertTrue(eventIterator.hasNext());

        Event second = eventIterator.next();
        assertEquals(second.getType(), "Blackhole");
        assertEquals(second.getUuid(), "76754afa-7126-4a2d-a8b7-63a447466710");
        assertEquals(second.getHost(), "ip-10-94-13-48");
        assertEquals(second.getTimestamp(), DateTime.parse("2012-05-14T23:56:55.654Z"));
        assertEquals(second.getData(), ImmutableMap.of(
                "method","POST",
                "uri","http://www.proofpoint.com/",
                "entity", "this is another test"));

        assertFalse(eventIterator.hasNext());
    }
}
