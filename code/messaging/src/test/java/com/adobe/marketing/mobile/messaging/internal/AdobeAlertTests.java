/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AdobeAlertTests {
    private static final String TITLE = "testTitle";
    private static final String MESSAGE = "testMessageBody";
    private static final String DEFAULT_BUTTON_TEXT = "testDefaultButtonText";
    private static final String DEFAULT_BUTTON_URL = "https://testdefaultbuttonurl.com";
    private static final String CANCEL_BUTTON_TEXT = "testCancelButtonText";
    private static final String CANCEL_BUTTON_URL = "https://testcancelbuttonurl.com";


    @Before
    public void setup() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreateAdobeAlert_WithAlertStyle_AllParametersPresent_ThenAlertBuilt() {
        // test
        InternalAdobeAlert internalAdobeAlert = new InternalAdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(internalAdobeAlert);
        assertEquals(TITLE, internalAdobeAlert.getTitle());
        assertEquals(MESSAGE, internalAdobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, internalAdobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, internalAdobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, internalAdobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, internalAdobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, internalAdobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_WithActionSheetStyle_AllParametersPresent_ThenAlertBuilt() {
        // test
        InternalAdobeAlert internalAdobeAlert = new InternalAdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ACTION_SHEET)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(internalAdobeAlert);
        assertEquals(TITLE, internalAdobeAlert.getTitle());
        assertEquals(MESSAGE, internalAdobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, internalAdobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, internalAdobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, internalAdobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, internalAdobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ACTION_SHEET, internalAdobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_WithNullAlertStyle_AllOtherParametersPresent_ThenAlertNotBuilt() {
        // test
        InternalAdobeAlert internalAdobeAlert = new InternalAdobeAlert.Builder(DEFAULT_BUTTON_TEXT, null)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNull(internalAdobeAlert);
    }

    @Test
    public void testCreateAdobeAlert_WithUnsupportedAlertStyle_AllOtherParametersPresent_ThenAlertNotBuilt() {
        // test
        InternalAdobeAlert internalAdobeAlert = new InternalAdobeAlert.Builder(DEFAULT_BUTTON_TEXT, "unknown")
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNull(internalAdobeAlert);
    }

    @Test
    public void testCreateAdobeAlert_RequiredParametersOnly_ThenAlertBuilt() {
        // test
        InternalAdobeAlert internalAdobeAlert = new InternalAdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .build();

        // verify
        assertNotNull(internalAdobeAlert);
        assertEquals("", internalAdobeAlert.getTitle());
        assertEquals("", internalAdobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, internalAdobeAlert.getDefaultButton());
        assertEquals("", internalAdobeAlert.getDefaultButtonUrl());
        assertEquals("", internalAdobeAlert.getCancelButton());
        assertEquals("", internalAdobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, internalAdobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_AlertTitleNotRequired() {
        // test
        InternalAdobeAlert internalAdobeAlert = new InternalAdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(internalAdobeAlert);
        assertEquals("", internalAdobeAlert.getTitle());
        assertEquals(MESSAGE, internalAdobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, internalAdobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, internalAdobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, internalAdobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, internalAdobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, internalAdobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_AlertMessageBodyNotRequired() {
        // test
        InternalAdobeAlert internalAdobeAlert = new InternalAdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(internalAdobeAlert);
        assertEquals(TITLE, internalAdobeAlert.getTitle());
        assertEquals("", internalAdobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, internalAdobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, internalAdobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, internalAdobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, internalAdobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, internalAdobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_DefaultButtonUrlNotRequired() {
        // test
        InternalAdobeAlert internalAdobeAlert = new InternalAdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(internalAdobeAlert);
        assertEquals(TITLE, internalAdobeAlert.getTitle());
        assertEquals(MESSAGE, internalAdobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, internalAdobeAlert.getDefaultButton());
        assertEquals("", internalAdobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, internalAdobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, internalAdobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, internalAdobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_CancelButtonTextNotRequired() {
        // test
        InternalAdobeAlert internalAdobeAlert = new InternalAdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(internalAdobeAlert);
        assertEquals(TITLE, internalAdobeAlert.getTitle());
        assertEquals(MESSAGE, internalAdobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, internalAdobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, internalAdobeAlert.getDefaultButtonUrl());
        assertEquals("", internalAdobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, internalAdobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, internalAdobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_CancelButtonUrlNotRequired() {
        // test
        InternalAdobeAlert internalAdobeAlert = new InternalAdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .build();

        // verify
        assertNotNull(internalAdobeAlert);
        assertEquals(TITLE, internalAdobeAlert.getTitle());
        assertEquals(MESSAGE, internalAdobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, internalAdobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, internalAdobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, internalAdobeAlert.getCancelButton());
        assertEquals("", internalAdobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, internalAdobeAlert.getStyle());
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testCreateAdobeAlert_CanOnlyBuildOnce() {
        // setup
        InternalAdobeAlert.Builder builder = new InternalAdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL);
        InternalAdobeAlert internalAdobeAlert = builder.build();

        // verify
        assertNotNull(internalAdobeAlert);
        assertEquals(TITLE, internalAdobeAlert.getTitle());
        assertEquals(MESSAGE, internalAdobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, internalAdobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, internalAdobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, internalAdobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, internalAdobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, internalAdobeAlert.getStyle());

        // test, throws UnsupportedOperationException
        builder.build();
    }
}
