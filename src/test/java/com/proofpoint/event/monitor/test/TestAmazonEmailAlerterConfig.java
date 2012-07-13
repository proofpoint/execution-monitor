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

import com.google.common.collect.ImmutableMap;
import com.proofpoint.event.monitor.AmazonEmailAlerterConfig;
import org.testng.annotations.Test;

import static com.proofpoint.configuration.testing.ConfigAssertions.assertFullMapping;
import static com.proofpoint.configuration.testing.ConfigAssertions.assertRecordedDefaults;
import static com.proofpoint.configuration.testing.ConfigAssertions.recordDefaults;

public class TestAmazonEmailAlerterConfig
{
    @Test
    public void testDefaults()
            throws Exception
    {
        assertRecordedDefaults(recordDefaults(AmazonEmailAlerterConfig.class)
                .setAlertingEnabled(true)
                .setFromAddress(null)
                .setToAddress(null));
    }

    @Test
    public void testFullMapping()
            throws Exception
    {
        assertFullMapping(
                ImmutableMap.of(
                        "execution-monitor.alerts.enabled", "false",
                        "execution-monitor.alerts.from", "from",
                        "execution-monitor.alerts.to", "to"),
                new AmazonEmailAlerterConfig()
                        .setAlertingEnabled(false)
                        .setFromAddress("from")
                        .setToAddress("to")
        );
    }
}
