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
package com.proofpoint.event.monitor.test;

import com.proofpoint.event.monitor.EventPredicate;
import com.proofpoint.event.monitor.EventStore;
import org.joda.time.DateTime;

public class DummyEventStore
    implements EventStore
{
    private boolean fail = false;
    private boolean eventExists = false;

    public void setFail(boolean fail)
    {
        this.fail = fail;
    }

    public void setEventExists(boolean eventExists)
    {
        this.eventExists = eventExists;
    }

    @Override
    public boolean recentEventExists(String eventType, EventPredicate eventPredicate, DateTime searchLimit)
    {
        if (fail) {
            throw new RuntimeException("Deliberate");
        }
        return eventExists;
    }
}
