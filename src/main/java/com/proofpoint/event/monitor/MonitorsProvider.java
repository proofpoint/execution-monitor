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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import org.weakref.jmx.MBeanExporter;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.management.MBeanServer;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.inject.name.Names.named;
import static org.weakref.jmx.ObjectNames.generatedNameOf;

public class MonitorsProvider implements Provider<Set<Monitor>>
{
    private final MonitorLoader monitorLoader;
    private final String monitorRulesFile;

    private Set<Monitor> monitors;
    private final List<String> mbeanNames = newArrayList();
    private final MBeanExporter exporter;

    @Inject
    public MonitorsProvider(MonitorLoader monitorLoader,
            MonitorConfig config,
            MBeanServer mbeanServer)
    {
        Preconditions.checkNotNull(monitorLoader, "monitorLoader is null");
        Preconditions.checkNotNull(config, "config is null");

        this.monitorLoader = monitorLoader;
        monitorRulesFile = config.getMonitorRulesFile();

        if (mbeanServer != null) {
            exporter = new MBeanExporter(mbeanServer);
        }
        else {
            exporter = null;
        }
    }

    @Override
    public synchronized Set<Monitor> get()
    {
        if (monitors == null) {
            try {
                String json = Files.toString(new File(monitorRulesFile), Charsets.UTF_8);
                monitors = monitorLoader.load(json);
            }
            catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        for (Monitor monitor : monitors) {
            monitor.start();
        }

        Set<String> eventTypes = newHashSet();
        for (Monitor monitor : monitors) {
            eventTypes.add(monitor.getEventType());
        }

        if (exporter != null) {
            for (Monitor monitor : monitors) {
                String name = generatedNameOf(Monitor.class, named(monitor.getName()));
                exporter.export(name, monitor);
                mbeanNames.add(name);
            }
        }

        return monitors;
    }

    @PreDestroy
    public synchronized void stop()
    {
        if (monitors == null) {
            return;
        }

        if (exporter != null) {
            for (String name : mbeanNames) {
                try {
                    // TODO: use unexportAll when jmxutils is upgraded to 1.11+
                    exporter.unexport(name);
                }
                catch (Exception ignored) {
                }
            }
        }

        for (Monitor monitor : monitors) {
            monitor.stop();
        }
    }
}