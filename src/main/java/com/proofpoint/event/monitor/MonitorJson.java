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

import com.proofpoint.units.Duration;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.validation.constraints.NotNull;

public class MonitorJson
{
    private static final Duration DEFAULT_INTERVAL = Duration.valueOf("30s");

    private final String eventType;
    private final String eventFilter;
    private final Duration maxAge;
    private final Duration checkInterval;

    @JsonCreator
    public MonitorJson(
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventFilter") String eventFilter,
            @JsonProperty("maxAge") Duration maxAge,
            @JsonProperty("checkInterval") Duration checkInterval)
    {
        this.eventType = eventType;
        this.eventFilter = eventFilter;
        this.maxAge = maxAge;
        this.checkInterval = checkInterval == null ? DEFAULT_INTERVAL : checkInterval;
    }

    @NotNull
    public String getEventType()
    {
        return eventType;
    }

    @NotNull
    public EventPredicate getEventPredicate()
    {
        return new EventPredicate(eventType, eventFilter);
    }

    @NotNull
    public Duration getMaxAge()
    {
        return maxAge;
    }

    @NotNull
    public Duration getCheckInterval()
    {
        return checkInterval;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MonitorJson that = (MonitorJson) o;

        if (checkInterval != null ? !checkInterval.equals(that.checkInterval) : that.checkInterval != null) {
            return false;
        }
        if (eventFilter != null ? !eventFilter.equals(that.eventFilter) : that.eventFilter != null) {
            return false;
        }
        if (eventType != null ? !eventType.equals(that.eventType) : that.eventType != null) {
            return false;
        }
        if (maxAge != null ? !maxAge.equals(that.maxAge) : that.maxAge != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = eventType != null ? eventType.hashCode() : 0;
        result = 31 * result + (eventFilter != null ? eventFilter.hashCode() : 0);
        result = 31 * result + (maxAge != null ? maxAge.hashCode() : 0);
        result = 31 * result + (checkInterval != null ? checkInterval.hashCode() : 0);
        return result;
    }
}
