package com.proofpoint.event.monitor.s3;

import com.proofpoint.configuration.Config;
import com.proofpoint.configuration.ConfigDescription;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class S3Config
{
    private String eventStagingLocation = null;

    @Config("collector.s3-staging-location")
    @ConfigDescription("base S3 URI to staging location")
    public S3Config setEventStagingLocation(String eventStagingLocation)
    {
        this.eventStagingLocation = eventStagingLocation;
        return this;
    }

    @NotNull
    @Pattern(regexp = "s3://[A-Za-z0-9-]+/([A-Za-z0-9-]+/)*", message = "is malformed")
    public String getEventStagingLocation()
    {
        return eventStagingLocation;
    }
}
