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

import javax.validation.constraints.AssertTrue;

public class AmazonEmailAlerterConfig
{
    private boolean alertingEnabled = true;
    private String toAddress = null;
    private String fromAddress = null;

    public String getFromAddress()
    {
        return fromAddress;
    }

    @Config("execution-monitor.alerts.from")
    public AmazonEmailAlerterConfig setFromAddress(String fromAddress)
    {
        this.fromAddress = fromAddress;
        return this;
    }

    public String getToAddress()
    {
        return toAddress;
    }

    @Config("execution-monitor.alerts.to")
    public AmazonEmailAlerterConfig setToAddress(String toAddress)
    {
        this.toAddress = toAddress;
        return this;
    }

    public boolean isAlertingEnabled()
    {
        return alertingEnabled;
    }

    @Config("execution-monitor.alerts.enabled")
    public AmazonEmailAlerterConfig setAlertingEnabled(boolean alertingEnabled)
    {
        this.alertingEnabled = alertingEnabled;
        return this;
    }

    @AssertTrue(message = "If AWS alerting is enabled, to- and from- addresses are required")
    public boolean isValid()
    {
        return !alertingEnabled || (fromAddress != null && toAddress != null);
    }
}
