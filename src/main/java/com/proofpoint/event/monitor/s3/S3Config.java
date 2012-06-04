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
