package com.proofpoint.event.monitor;

import org.joda.time.DateTime;

public interface EventStore
{
    boolean recentEventExists(String eventType, EventPredicate eventPredicate, DateTime searchLimit);
}
