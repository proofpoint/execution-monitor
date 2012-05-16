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

