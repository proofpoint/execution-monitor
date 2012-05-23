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
package com.proofpoint.event.monitor.s3;

import com.google.common.annotations.VisibleForTesting;
import com.proofpoint.event.monitor.Event;
import com.proofpoint.event.monitor.EventPredicate;
import com.proofpoint.event.monitor.EventStore;
import com.proofpoint.log.Logger;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.iq80.snappy.SnappyInputStream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

import static com.proofpoint.event.monitor.s3.S3StorageHelper.buildS3Directory;
import static com.proofpoint.event.monitor.s3.S3StorageHelper.getS3FileName;

public class S3EventStore implements EventStore
{
    private static final DateTimeFormatter DATE_FORMAT = ISODateTimeFormat.date().withZone(DateTimeZone.UTC);
    private static final Logger log = Logger.get(S3EventStore.class);

    private final S3StorageSystem storageSystem;
    private final String eventStagingLocation;
    private final ObjectMapper objectMapper;

    @Inject
    public S3EventStore(S3Config config, S3StorageSystem storageSystem, ObjectMapper objectMapper)
    {
        this.eventStagingLocation = config.getEventStagingLocation();
        this.storageSystem = storageSystem;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean recentEventExists(String eventType, EventPredicate filter, DateTime limit)
    {
        for (URI dateBaseUri : storageSystem.listDirectoriesNewestFirst(buildS3Directory(eventStagingLocation, eventType))) {
            DateTime dateBucket = DATE_FORMAT.parseDateTime(getS3FileName(dateBaseUri));
            for (URI hourBaseUri : storageSystem.listDirectoriesNewestFirst(dateBaseUri)) {
                int hour = Integer.parseInt(getS3FileName(hourBaseUri));
                // The bucket may contain events up to the start of the following hour
                DateTime bucketDateTime = dateBucket.hourOfDay().setCopy(hour).plusHours(1);

                if (bucketDateTime.isBefore(limit)) {
                    return false;
                }

                for (URI eventFile : storageSystem.listObjects(hourBaseUri)) {
                    try {
                        Iterator<Event> eventIterator = getEventIterator(storageSystem.getInputSupplier(eventFile).getInput(), objectMapper);
                        while (eventIterator.hasNext()) {
                            Event event = eventIterator.next();
                            if (filter.apply(event) && event.getTimestamp().isAfter(limit)) {
                                return true;
                            }
                        }
                    }
                    catch (IOException e) {
                        log.warn(e, "Exception while checking S3 object %s for recent event of type %s (filter %s)", eventFile, eventType, filter);
                    }
                }
            }
        }
        return false;
    }

    @VisibleForTesting
    static Iterator<Event> getEventIterator(InputStream snappyJsonStream, ObjectMapper objectMapper)
            throws IOException
    {
        InputStream eventStream = new SnappyInputStream(snappyJsonStream);
        JsonParser parser = objectMapper.getJsonFactory().createJsonParser(eventStream);
        return parser.readValuesAs(Event.class);
    }
}
