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

import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import static com.proofpoint.configuration.testing.ConfigAssertions.assertFullMapping;
import static com.proofpoint.configuration.testing.ConfigAssertions.assertRecordedDefaults;
import static com.proofpoint.configuration.testing.ConfigAssertions.recordDefaults;
import static com.proofpoint.testing.ValidationAssertions.assertFailsValidation;
import static com.proofpoint.testing.ValidationAssertions.assertValidates;

public class TestS3Config
{
    private static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void testDefaults()
            throws Exception
    {
        assertRecordedDefaults(recordDefaults(S3Config.class).setEventStagingLocation(null));
    }

    @Test
    public void testFullMapping()
            throws Exception
    {
        assertFullMapping(
                ImmutableMap.of(
                        "collector.s3-staging-location", "s3://foo/bar/baz/"),
                new S3Config()
                        .setEventStagingLocation("s3://foo/bar/baz/"));
    }

    @Test
    public void testValidation()
            throws Exception
    {
        assertValidates(new S3Config().setEventStagingLocation("s3://some/place/"));
        assertFailsValidation(new S3Config().setEventStagingLocation(null), "eventStagingLocation", "may not be null", NotNull.class);
        assertFailsValidation(new S3Config().setEventStagingLocation("not/an/s3/path"), "eventStagingLocation", "is malformed", Pattern.class);
        assertFailsValidation(new S3Config().setEventStagingLocation("s3:///emptybucket/"), "eventStagingLocation", "is malformed", Pattern.class);
        assertFailsValidation(new S3Config().setEventStagingLocation("s3://missing/trailing/slash"), "eventStagingLocation", "is malformed", Pattern.class);
    }

}
