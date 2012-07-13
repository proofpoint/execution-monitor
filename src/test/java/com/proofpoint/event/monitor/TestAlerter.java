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

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class TestAlerter
{
    private AmazonSimpleEmailService mockEmailService;

    @BeforeMethod
    public void setUp()
            throws Exception
    {
        mockEmailService = mock(AmazonSimpleEmailService.class);
    }

    @Test
    public void testAlerterAlertsWhenEnabled()
            throws Exception
    {
        AmazonEmailAlerter alerter = new AmazonEmailAlerter(new AmazonEmailAlerterConfig().setAlertingEnabled(true), mockEmailService);

        alerter.sendMessage("from", "to");
        verify(mockEmailService).sendEmail(Matchers.<SendEmailRequest>any());
    }

    @Test
    public void testAlerterSkipsWhenDisabled()
            throws Exception
    {
        AmazonEmailAlerter alerter = new AmazonEmailAlerter(new AmazonEmailAlerterConfig().setAlertingEnabled(false), mockEmailService);

        alerter.sendMessage("from", "to");

        verifyZeroInteractions(mockEmailService);
    }
}
