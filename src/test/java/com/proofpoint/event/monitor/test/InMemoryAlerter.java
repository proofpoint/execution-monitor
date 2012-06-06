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

import com.google.common.collect.Lists;
import com.proofpoint.event.monitor.Alerter;
import com.proofpoint.event.monitor.Monitor;
import org.testng.collections.Maps;

import java.util.List;
import java.util.Map;

public class InMemoryAlerter
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
