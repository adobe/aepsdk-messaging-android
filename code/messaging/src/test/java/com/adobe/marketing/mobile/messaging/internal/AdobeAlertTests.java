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
        AdobeAlert adobeAlert = new AdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(adobeAlert);
        assertEquals(TITLE, adobeAlert.getTitle());
        assertEquals(MESSAGE, adobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, adobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, adobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, adobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, adobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, adobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_WithActionSheetStyle_AllParametersPresent_ThenAlertBuilt() {
        // test
        AdobeAlert adobeAlert = new AdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ACTION_SHEET)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(adobeAlert);
        assertEquals(TITLE, adobeAlert.getTitle());
        assertEquals(MESSAGE, adobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, adobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, adobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, adobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, adobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ACTION_SHEET, adobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_WithNullAlertStyle_AllOtherParametersPresent_ThenAlertNotBuilt() {
        // test
        AdobeAlert adobeAlert = new AdobeAlert.Builder(DEFAULT_BUTTON_TEXT, null)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNull(adobeAlert);
    }

    @Test
    public void testCreateAdobeAlert_WithUnsupportedAlertStyle_AllOtherParametersPresent_ThenAlertNotBuilt() {
        // test
        AdobeAlert adobeAlert = new AdobeAlert.Builder(DEFAULT_BUTTON_TEXT, "unknown")
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNull(adobeAlert);
    }

    @Test
    public void testCreateAdobeAlert_RequiredParametersOnly_ThenAlertBuilt() {
        // test
        AdobeAlert adobeAlert = new AdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .build();

        // verify
        assertNotNull(adobeAlert);
        assertEquals("", adobeAlert.getTitle());
        assertEquals("", adobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, adobeAlert.getDefaultButton());
        assertEquals("", adobeAlert.getDefaultButtonUrl());
        assertEquals("", adobeAlert.getCancelButton());
        assertEquals("", adobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, adobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_AlertTitleNotRequired() {
        // test
        AdobeAlert adobeAlert = new AdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(adobeAlert);
        assertEquals("", adobeAlert.getTitle());
        assertEquals(MESSAGE, adobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, adobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, adobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, adobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, adobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, adobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_AlertMessageBodyNotRequired() {
        // test
        AdobeAlert adobeAlert = new AdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(adobeAlert);
        assertEquals(TITLE, adobeAlert.getTitle());
        assertEquals("", adobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, adobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, adobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, adobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, adobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, adobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_DefaultButtonUrlNotRequired() {
        // test
        AdobeAlert adobeAlert = new AdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(adobeAlert);
        assertEquals(TITLE, adobeAlert.getTitle());
        assertEquals(MESSAGE, adobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, adobeAlert.getDefaultButton());
        assertEquals("", adobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, adobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, adobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, adobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_CancelButtonTextNotRequired() {
        // test
        AdobeAlert adobeAlert = new AdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButtonUrl(CANCEL_BUTTON_URL)
                .build();

        // verify
        assertNotNull(adobeAlert);
        assertEquals(TITLE, adobeAlert.getTitle());
        assertEquals(MESSAGE, adobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, adobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, adobeAlert.getDefaultButtonUrl());
        assertEquals("", adobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, adobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, adobeAlert.getStyle());
    }

    @Test
    public void testCreateAdobeAlert_CancelButtonUrlNotRequired() {
        // test
        AdobeAlert adobeAlert = new AdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .build();

        // verify
        assertNotNull(adobeAlert);
        assertEquals(TITLE, adobeAlert.getTitle());
        assertEquals(MESSAGE, adobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, adobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, adobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, adobeAlert.getCancelButton());
        assertEquals("", adobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, adobeAlert.getStyle());
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testCreateAdobeAlert_CanOnlyBuildOnce() {
        // setup
        AdobeAlert.Builder builder = new AdobeAlert.Builder(DEFAULT_BUTTON_TEXT, MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setDefaultButtonUrl(DEFAULT_BUTTON_URL)
                .setCancelButton(CANCEL_BUTTON_TEXT)
                .setCancelButtonUrl(CANCEL_BUTTON_URL);
        AdobeAlert adobeAlert = builder.build();

        // verify
        assertNotNull(adobeAlert);
        assertEquals(TITLE, adobeAlert.getTitle());
        assertEquals(MESSAGE, adobeAlert.getMessage());
        assertEquals(DEFAULT_BUTTON_TEXT, adobeAlert.getDefaultButton());
        assertEquals(DEFAULT_BUTTON_URL, adobeAlert.getDefaultButtonUrl());
        assertEquals(CANCEL_BUTTON_TEXT, adobeAlert.getCancelButton());
        assertEquals(CANCEL_BUTTON_URL, adobeAlert.getCancelButtonUrl());
        assertEquals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT, adobeAlert.getStyle());

        // test, throws UnsupportedOperationException
        builder.build();
    }
}
