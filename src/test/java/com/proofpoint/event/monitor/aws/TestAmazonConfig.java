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
package com.proofpoint.event.monitor.aws;

import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import static com.proofpoint.configuration.testing.ConfigAssertions.assertFullMapping;
import static com.proofpoint.configuration.testing.ConfigAssertions.assertRecordedDefaults;
import static com.proofpoint.configuration.testing.ConfigAssertions.recordDefaults;
import static com.proofpoint.testing.ValidationAssertions.assertFailsValidation;
import static com.proofpoint.testing.ValidationAssertions.assertValidates;

public class TestAmazonConfig
{
    private static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void testDefaults()
            throws Exception
    {
        assertRecordedDefaults(recordDefaults(AmazonConfig.class)
                .setAwsAccessKey(null)
                .setAwsSecretKey(null));
    }

    @Test
    public void testFullMapping()
            throws Exception
    {
        assertFullMapping(
                ImmutableMap.of(
                        "execution-monitor.aws-access-key", "access",
                        "execution-monitor.aws-secret-key", "secret"),
                new AmazonConfig()
                        .setAwsAccessKey("access")
                        .setAwsSecretKey("secret")
        );
    }

    @Test
    public void testValidatesNotNullKeys()
            throws Exception
    {
        assertValidates(new AmazonConfig().setAwsAccessKey("foo").setAwsSecretKey("bar"));
        assertFailsValidation(new AmazonConfig().setAwsAccessKey(null).setAwsSecretKey("bar"), "awsAccessKey", "may not be null", NotNull.class);
        assertFailsValidation(new AmazonConfig().setAwsAccessKey("foo").setAwsSecretKey(null), "awsSecretKey", "may not be null", NotNull.class);
    }
}

