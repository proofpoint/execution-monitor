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

import com.proofpoint.configuration.Config;

import javax.validation.constraints.NotNull;

public class MonitorConfig
{
    private String monitorRulesFile = "etc/monitor.json";

    @NotNull
    public String getMonitorRulesFile()
    {
        return monitorRulesFile;
    }

    @Config("monitor.file")
    public MonitorConfig setMonitorRulesFile(String monitorRulesFile)
    {
        this.monitorRulesFile = monitorRulesFile;
        return this;
    }
}
