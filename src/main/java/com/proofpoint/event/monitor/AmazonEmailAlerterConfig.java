package com.proofpoint.event.monitor;

import com.proofpoint.configuration.Config;

import javax.validation.constraints.NotNull;

public class AmazonEmailAlerterConfig
{
    private String toAddress = null;
    private String fromAddress = null;

    @NotNull
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

    @NotNull
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
}
