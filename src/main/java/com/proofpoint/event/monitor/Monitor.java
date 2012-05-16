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

import com.google.common.base.Preconditions;
import com.proofpoint.log.Logger;
import com.proofpoint.units.Duration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.weakref.jmx.Managed;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.joda.time.DateTime.now;

public class Monitor
{
    private static final Logger log = Logger.get(Monitor.class);
    private final String name;
    private final String eventType;
    private final EventPredicate eventPredicate;
    private final ScheduledExecutorService executor;
    private final int maxAgeInMinutes;
    private final Duration checkInterval;
    private final EventStore eventStore;
    private final Alerter alerter;
    private final AtomicBoolean failed = new AtomicBoolean();
    private ScheduledFuture<?> scheduledFuture;

    public Monitor(String name, String eventType, EventPredicate eventPredicate, Duration maxAge, Duration checkInterval, ScheduledExecutorService executor, EventStore eventStore, Alerter alerter)
    {
        Preconditions.checkNotNull(name, "name is null");
        Preconditions.checkNotNull(eventType, "eventType is null");
        Preconditions.checkNotNull(eventPredicate, "eventPredicate is null");
        Preconditions.checkNotNull(maxAge, "maxAge is null");
        Preconditions.checkNotNull(checkInterval, "checkInterval is null");
        Preconditions.checkNotNull(executor, "executor is null");
        Preconditions.checkNotNull(eventStore, "eventStore is null");
        Preconditions.checkNotNull(alerter, "alerter is null");

        this.name = name;
        this.eventType = eventType;
        this.eventPredicate = eventPredicate;
        this.maxAgeInMinutes = (int) maxAge.convertTo(TimeUnit.MINUTES);
        this.checkInterval = checkInterval;
        this.executor = executor;
        this.eventStore = eventStore;
        this.alerter = alerter;
    }

    @PostConstruct
    public synchronized void start()
    {
        if (scheduledFuture == null) {
            scheduledFuture = executor.scheduleAtFixedRate(new Runnable()
            {
                @Override
                public void run()
                {
                    checkState();
                }
            }, 5, (long) checkInterval.convertTo(TimeUnit.SECONDS), TimeUnit.SECONDS);
        }
    }

    @PreDestroy
    public synchronized void stop()
    {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    @Managed
    public String getName()
    {
        return name;
    }

    @Managed(description = "Maximum permissible time since last event (minutes)")
    public int getMaxAgeInMinutes()
    {
        return maxAgeInMinutes;
    }

    @Managed(description = "Is this monitor in the failed state?")
    public boolean isFailed()
    {
        return failed.get();
    }

    @Managed(description = "Type of the event being monitored")
    public String getEventType()
    {
        return eventType;
    }

    @Managed(description = "Filter on the event being monitored")
    public String getEventFilter()
    {
        return eventPredicate.getEventFilter();
    }

    @Managed
    public void checkState()
    {
        DateTime startTime = now(DateTimeZone.UTC);
        DateTime oldestGoodDateTime = startTime.minusMinutes(maxAgeInMinutes);

        log.info("Checking state for monitor %s", name);
        if (eventStore.recentEventExists(eventType, eventPredicate, oldestGoodDateTime)) {
            recovered(String.format("At least one event has been sent within the last %d minutes", maxAgeInMinutes));
        }
        else {
            failed(String.format("Expected to have seen an event since %s (%s minutes ago), but have not", oldestGoodDateTime, maxAgeInMinutes));
        }
    }

    private void failed(String description)
    {
        log.debug("Monitor %s is failed", name);
        if (failed.compareAndSet(false, true)) {
            // fire error message
            alerter.failed(this, description);
        }
    }

    private void recovered(String description)
    {
        log.debug("Monitor %s is recovered", name);
        if (failed.compareAndSet(true, false)) {
            // fire recovery message
            alerter.recovered(this, description);
        }
    }
}
