/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.Message;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.InAppMessage;
import com.adobe.marketing.mobile.services.ui.Presentable;
import com.adobe.marketing.mobile.services.ui.PresentationError;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.services.ui.message.InAppMessageEventHandler;
import com.adobe.marketing.mobile.services.ui.message.InAppMessageSettings;
import com.adobe.marketing.mobile.services.uri.UriOpening;
import com.adobe.marketing.mobile.util.DefaultPresentationUtilityProvider;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PresentableMessageMapperTests {
    // Mocks
    @Mock ExtensionApi mockExtensionApi;
    @Mock ServiceProvider mockServiceProvider;
    @Mock UIService mockUIService;
    @Mock UriOpening mockUriOpening;
    @Mock Message mockMessage;
    @Mock Presentable<InAppMessage> mockInAppPresentable;
    @Mock InAppMessage mockPresentation;
    @Mock PresentationError mockPresentationError;
    @Mock InAppMessageEventHandler mockEventHandler;
    @Mock InAppMessageSettings mockMessageSettings;
    @Mock MessagingExtension mockMessagingExtension;
    @Mock PropositionInfo mockPropositionInfo;

    private static final String html =
            "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1"
                    + " align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br"
                    + " />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\""
                    + " href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>";
    private PresentableMessageMapper.InternalMessage internalMessage;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() {
        PresentableMessageMapper.getInstance().clearPresentableMessageMap();
        reset(mockExtensionApi);
        reset(mockServiceProvider);
        reset(mockUIService);
        reset(mockUriOpening);
        reset(mockMessage);
        reset(mockInAppPresentable);
        reset(mockPresentation);
        reset(mockMessageSettings);
        reset(mockPresentationError);
        reset(mockEventHandler);
        reset(mockMessagingExtension);
        reset(mockPropositionInfo);
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
            when(mockServiceProvider.getUriService()).thenReturn(mockUriOpening);
            when(mockUIService.create(any(InAppMessage.class), any()))
                    .thenReturn(mockInAppPresentable);
            when(mockInAppPresentable.getPresentation()).thenReturn(mockPresentation);
            when(mockPresentation.getId()).thenReturn("mockId");
            when(mockMessagingExtension.getApi()).thenReturn(mockExtensionApi);
            runnable.run();
        }
    }

    PropositionItem createPropositionItem() throws MessageRequiredFieldMissingException {
        Map<String, Object> data = new HashMap<>();
        data.put(MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT, html);
        data.put(
                MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT_TYPE,
                ContentType.TEXT_HTML.toString());
        return new PropositionItem("123456789", SchemaType.INAPP, data);
    }

    // ========================================================================================
    // createMessage tests
    // ========================================================================================
    @Test
    public void test_createMessage() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // verify
                    verify(mockUIService, times(1))
                            .create(
                                    any(InAppMessage.class),
                                    any(DefaultPresentationUtilityProvider.class));
                });
    }

    @Test
    public void test_createMessage_MessagesForSameConsequence() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    PresentableMessageMapper.InternalMessage internalMessageSameConsequence = null;
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                        internalMessageSameConsequence =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // verify
                    verify(mockUIService, times(1))
                            .create(
                                    any(InAppMessage.class),
                                    any(DefaultPresentationUtilityProvider.class));
                    assertEquals(internalMessage, internalMessageSameConsequence);
                });
    }

    @Test
    public void test_createMessage_NullPropositionItem() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        null,
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        assertEquals(
                                MessageRequiredFieldMissingException.class, exception.getClass());
                    }

                    // verify
                    verify(mockUIService, times(0))
                            .create(
                                    any(InAppMessage.class),
                                    any(DefaultPresentationUtilityProvider.class));
                });
    }

    @Test
    public void test_createMessage_InvalidSchema() throws MessageRequiredFieldMissingException {
        // setup
        Map<String, Object> data = new HashMap<>();
        data.put(MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT, html);
        data.put(
                MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT_TYPE,
                ContentType.TEXT_HTML.toString());
        PropositionItem propositionItem = new PropositionItem("123456789", SchemaType.FEED, data);

        runUsingMockedServiceProvider(
                () -> {
                    // test
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        propositionItem,
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        assertEquals(
                                MessageRequiredFieldMissingException.class, exception.getClass());
                    }

                    // verify
                    verify(mockUIService, times(0))
                            .create(
                                    any(InAppMessage.class),
                                    any(DefaultPresentationUtilityProvider.class));
                });
    }

    @Test
    public void test_createMessage_PropositionItemMissingItemData() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    try {
                        PropositionItem propositionItem =
                                new PropositionItem("123456789", SchemaType.INAPP, null);
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        propositionItem,
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        assertEquals(
                                MessageRequiredFieldMissingException.class, exception.getClass());
                    }

                    // verify
                    verify(mockUIService, times(0))
                            .create(
                                    any(InAppMessage.class),
                                    any(DefaultPresentationUtilityProvider.class));
                });
    }

    @Test
    public void test_createMessage_InAppSchemaDataMissingContent()
            throws MessageRequiredFieldMissingException {
        // setup
        Map<String, Object> data = new HashMap<>();
        data.put(
                MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT_TYPE,
                ContentType.TEXT_HTML.toString());
        PropositionItem propositionItem = new PropositionItem("123456789", SchemaType.INAPP, data);

        runUsingMockedServiceProvider(
                () -> {
                    // test
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        propositionItem,
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        assertEquals(
                                MessageRequiredFieldMissingException.class, exception.getClass());
                    }

                    // verify
                    verify(mockUIService, times(0))
                            .create(
                                    any(InAppMessage.class),
                                    any(DefaultPresentationUtilityProvider.class));
                });
    }

    @Test
    public void test_createMessage_InAppSchemaDataContentIsNotHtml()
            throws JSONException, MessageRequiredFieldMissingException {
        // setup
        Map<String, Object> data = new HashMap<>();
        data.put(
                MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT,
                new JSONObject("{\"key\":\"value\"}"));
        data.put(
                MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT_TYPE,
                ContentType.APPLICATION_JSON.toString());
        PropositionItem propositionItem = new PropositionItem("123456789", SchemaType.INAPP, data);

        runUsingMockedServiceProvider(
                () -> {
                    // test
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        propositionItem,
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        assertEquals(
                                MessageRequiredFieldMissingException.class, exception.getClass());
                    }

                    // verify
                    verify(mockUIService, times(0))
                            .create(
                                    any(InAppMessage.class),
                                    any(DefaultPresentationUtilityProvider.class));
                });
    }

    @Test
    public void test_createMessage_NullUIService() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    when(mockServiceProvider.getUIService()).thenReturn(null);
                    // test
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        assertEquals(IllegalStateException.class, exception.getClass());
                    }

                    // verify
                    verify(mockUIService, times(0))
                            .create(
                                    any(InAppMessage.class),
                                    any(DefaultPresentationUtilityProvider.class));
                });
    }

    @Test
    public void test_createMessage_WithoutMessageSettings() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    try {
                        Map<String, Object> data = new HashMap<>();
                        data.put(MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT, html);
                        data.put(
                                MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT_TYPE,
                                ContentType.TEXT_HTML.toString());
                        PropositionItem propositionItem =
                                new PropositionItem("123456789", SchemaType.INAPP, data);
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        propositionItem,
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // verify
                    ArgumentCaptor<InAppMessage> inAppMessageCaptor =
                            ArgumentCaptor.forClass(InAppMessage.class);
                    verify(mockUIService, times(1))
                            .create(
                                    inAppMessageCaptor.capture(),
                                    any(DefaultPresentationUtilityProvider.class));
                    InAppMessageSettings settings = inAppMessageCaptor.getValue().getSettings();
                    assertEquals(html, settings.getContent());
                    assertEquals(100, settings.getWidth());
                    assertEquals(Integer.MAX_VALUE, settings.getMaxWidth());
                    assertEquals(100, settings.getHeight());
                    assertEquals("#FFFFFF", settings.getBackdropColor());
                    assertEquals(0.0f, settings.getBackdropOpacity(), 0.0);
                    assertEquals(0.0f, settings.getCornerRadius(), 0.0);
                    assertEquals(
                            InAppMessageSettings.MessageAnimation.NONE,
                            settings.getDismissAnimation());
                    assertEquals(
                            InAppMessageSettings.MessageAnimation.NONE,
                            settings.getDisplayAnimation());
                    assertTrue(settings.getGestureMap().isEmpty());
                    assertEquals(
                            InAppMessageSettings.MessageAlignment.CENTER,
                            settings.getHorizontalAlignment());
                    assertEquals(0, settings.getHorizontalInset());
                    assertEquals(
                            InAppMessageSettings.MessageAlignment.CENTER,
                            settings.getVerticalAlignment());
                    assertEquals(0, settings.getVerticalInset());
                    assertTrue(settings.getShouldTakeOverUi());
                });
    }

    @Test
    public void test_createMessage_WithMessageSettings() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    try {
                        Map<String, String> gestureMap = new HashMap<>();
                        Map<String, Object> rawMessageSettings = new HashMap();
                        gestureMap.put("tapBackground", "adbinapp://dismiss");
                        gestureMap.put("swipeLeft", "adbinapp://dismiss?interaction=negative");
                        gestureMap.put("swipeRight", "adbinapp://dismiss?interaction=positive");
                        gestureMap.put("swipeUp", "adbinapp://dismiss");
                        gestureMap.put("swipeDown", "adbinapp://dismiss");
                        rawMessageSettings.put("width", 90);
                        rawMessageSettings.put("maxWidth", 200);
                        rawMessageSettings.put("height", 80);
                        rawMessageSettings.put("backdropColor", "808080");
                        rawMessageSettings.put("backdropOpacity", 0.5f);
                        rawMessageSettings.put("cornerRadius", 70.0f);
                        rawMessageSettings.put("dismissAnimation", "fade");
                        rawMessageSettings.put("displayAnimation", "bottom");
                        rawMessageSettings.put("gestures", gestureMap);
                        rawMessageSettings.put("horizontalAlign", "right");
                        rawMessageSettings.put("horizontalInset", 5);
                        rawMessageSettings.put("verticalAlign", "top");
                        rawMessageSettings.put("verticalInset", 10);
                        rawMessageSettings.put("uiTakeover", false);
                        Map<String, Object> data = new HashMap<>();
                        data.put(MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT, html);
                        data.put(
                                MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT_TYPE,
                                ContentType.TEXT_HTML.toString());
                        data.put(
                                MessagingTestConstants.ConsequenceDetailDataKeys.MOBILE_PARAMETERS,
                                rawMessageSettings);
                        PropositionItem propositionItem =
                                new PropositionItem("123456789", SchemaType.INAPP, data);
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        propositionItem,
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // verify
                    ArgumentCaptor<InAppMessage> inAppMessageCaptor =
                            ArgumentCaptor.forClass(InAppMessage.class);
                    verify(mockUIService, times(1))
                            .create(
                                    inAppMessageCaptor.capture(),
                                    any(DefaultPresentationUtilityProvider.class));
                    InAppMessageSettings settings = inAppMessageCaptor.getValue().getSettings();
                    assertEquals(html, settings.getContent());
                    assertEquals(90, settings.getWidth());
                    assertEquals(200, settings.getMaxWidth());
                    assertEquals(80, settings.getHeight());
                    assertEquals("808080", settings.getBackdropColor());
                    assertEquals(0.5f, settings.getBackdropOpacity(), 0.0);
                    assertEquals(70.0f, settings.getCornerRadius(), 0.0);
                    assertEquals(
                            InAppMessageSettings.MessageAnimation.FADE,
                            settings.getDismissAnimation());
                    assertEquals(
                            InAppMessageSettings.MessageAnimation.BOTTOM,
                            settings.getDisplayAnimation());
                    assertEquals(
                            "adbinapp://dismiss",
                            settings.getGestureMap()
                                    .get(InAppMessageSettings.MessageGesture.TAP_BACKGROUND));
                    assertEquals(
                            "adbinapp://dismiss?interaction=negative",
                            settings.getGestureMap()
                                    .get(InAppMessageSettings.MessageGesture.SWIPE_LEFT));
                    assertEquals(
                            "adbinapp://dismiss?interaction=positive",
                            settings.getGestureMap()
                                    .get(InAppMessageSettings.MessageGesture.SWIPE_RIGHT));
                    assertEquals(
                            "adbinapp://dismiss",
                            settings.getGestureMap()
                                    .get(InAppMessageSettings.MessageGesture.SWIPE_UP));
                    assertEquals(
                            "adbinapp://dismiss",
                            settings.getGestureMap()
                                    .get(InAppMessageSettings.MessageGesture.SWIPE_DOWN));
                    assertEquals(
                            InAppMessageSettings.MessageAlignment.RIGHT,
                            settings.getHorizontalAlignment());
                    assertEquals(5, settings.getHorizontalInset());
                    assertEquals(
                            InAppMessageSettings.MessageAlignment.TOP,
                            settings.getVerticalAlignment());
                    assertEquals(10, settings.getVerticalInset());
                    assertFalse(settings.getShouldTakeOverUi());
                });
    }

    @Test
    public void test_createMessage_WithFitToContentTrue() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        Map<String, Object> rawMessageSettings = new HashMap<>();
                        rawMessageSettings.put(
                                MessagingConstants.EventDataKeys.MobileParametersKeys
                                        .FIT_TO_CONTENT,
                                true);
                        Map<String, Object> data = new HashMap<>();
                        data.put(MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT, html);
                        data.put(
                                MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT_TYPE,
                                ContentType.TEXT_HTML.toString());
                        data.put(
                                MessagingTestConstants.ConsequenceDetailDataKeys.MOBILE_PARAMETERS,
                                rawMessageSettings);
                        PropositionItem propositionItem =
                                new PropositionItem("123456789", SchemaType.INAPP, data);
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        propositionItem,
                                                        new HashMap<>(),
                                                        null);

                        // verify
                        ArgumentCaptor<InAppMessage> inAppMessageCaptor =
                                ArgumentCaptor.forClass(InAppMessage.class);
                        verify(mockUIService, times(1))
                                .create(
                                        inAppMessageCaptor.capture(),
                                        any(DefaultPresentationUtilityProvider.class));
                        InAppMessageSettings settings = inAppMessageCaptor.getValue().getSettings();
                        assertTrue(settings.getFitToContent());
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }
                });
    }

    @Test
    public void test_createMessage_WithFitToContentFalse() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        Map<String, Object> rawMessageSettings = new HashMap<>();
                        rawMessageSettings.put(
                                MessagingConstants.EventDataKeys.MobileParametersKeys
                                        .FIT_TO_CONTENT,
                                false);
                        Map<String, Object> data = new HashMap<>();
                        data.put(MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT, html);
                        data.put(
                                MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT_TYPE,
                                ContentType.TEXT_HTML.toString());
                        data.put(
                                MessagingTestConstants.ConsequenceDetailDataKeys.MOBILE_PARAMETERS,
                                rawMessageSettings);
                        PropositionItem propositionItem =
                                new PropositionItem("123456789", SchemaType.INAPP, data);
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        propositionItem,
                                                        new HashMap<>(),
                                                        null);

                        // verify
                        ArgumentCaptor<InAppMessage> inAppMessageCaptor =
                                ArgumentCaptor.forClass(InAppMessage.class);
                        verify(mockUIService, times(1))
                                .create(
                                        inAppMessageCaptor.capture(),
                                        any(DefaultPresentationUtilityProvider.class));
                        InAppMessageSettings settings = inAppMessageCaptor.getValue().getSettings();
                        assertFalse(settings.getFitToContent());
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }
                });
    }

    // ========================================================================================
    // Message getId
    // ========================================================================================
    @Test
    public void test_messageGetId() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }
                    // test
                    String id = internalMessage.getId();

                    // verify
                    assertEquals("123456789", id);
                });
    }

    // ========================================================================================
    // Message set/getAutoTrack
    // ========================================================================================
    @Test
    public void test_messageSetGetAutoTrack() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }
                    // test
                    internalMessage.setAutoTrack(false);

                    // verify
                    assertFalse(internalMessage.getAutoTrack());
                });
    }

    // ========================================================================================
    // Message show, dismiss, and trigger tests
    // ========================================================================================
    @Test
    public void test_messageShow() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }
                    // test
                    internalMessage.show();

                    // verify fullscreen message show called
                    verify(mockInAppPresentable, times(1)).show();
                });
    }

    @Test
    public void test_messageDismiss() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }
                    // test
                    internalMessage.dismiss();

                    // verify fullscreen message dismiss called
                    verify(mockInAppPresentable, times(1)).dismiss();
                });
    }

    @Test
    public void test_messageTrigger() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        mockPropositionInfo);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }
                    PresentableMessageMapper.InternalMessage spyInternalMessage =
                            Mockito.spy(internalMessage);

                    // test
                    spyInternalMessage.trigger();

                    // verify tracking event data
                    verify(spyInternalMessage, times(1))
                            .track(eq(null), eq(MessagingEdgeEventType.TRIGGER));
                    verify(spyInternalMessage, times(1))
                            .recordEventHistory(eq(null), eq(MessagingEdgeEventType.TRIGGER));
                });
    }

    @Test
    public void test_messageTrigger_autoTrackFalse() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // test
                    internalMessage.setAutoTrack(false);
                    internalMessage.trigger();

                    // verify no tracking event
                    verify(mockMessagingExtension, times(0)).sendPropositionInteraction(any());
                });
    }

    // ========================================================================================
    // Message track tests
    // ========================================================================================
    @Test
    public void test_messageTrack() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    ArgumentCaptor<Map<String, Object>> interactionXdmCapture =
                            ArgumentCaptor.forClass(Map.class);
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        mockPropositionInfo);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // test
                    internalMessage.track("mock track", MessagingEdgeEventType.INTERACT);

                    // verify tracking event
                    verify(mockMessagingExtension, times(1))
                            .sendPropositionInteraction(interactionXdmCapture.capture());
                    Map<String, Object> interactionXdm = interactionXdmCapture.getValue();
                    assertEquals(
                            MessagingEdgeEventType.INTERACT.toString(),
                            interactionXdm.get(
                                    MessagingTestConstants.EventDataKeys.Messaging.XDMDataKeys
                                            .EVENT_TYPE));
                });
    }

    @Test
    public void test_messageTrack_MissingMessagingEdgeEventType() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // test
                    internalMessage.track("mock track", null);

                    // verify no tracking event
                    verify(mockMessagingExtension, times(0)).sendPropositionInteraction(any());
                });
    }

    @Test
    public void test_messageTrack_MissingPropositionInfo() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // test
                    internalMessage.track("mock track", MessagingEdgeEventType.INTERACT);

                    // verify no tracking event
                    verify(mockMessagingExtension, times(0)).sendPropositionInteraction(any());
                });
    }

    @Test
    public void test_messageTrack_MissingMessagingExtension() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        null,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        mockPropositionInfo);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // test
                    internalMessage.track("mock track", MessagingEdgeEventType.INTERACT);

                    // verify no tracking event
                    verify(mockMessagingExtension, times(0)).sendPropositionInteraction(any());
                });
    }

    // ========================================================================================
    // recordEventHistory
    // ========================================================================================

    // TODO: - event history no longer uses Messaging extension's API - it uses MobileCore.dispatch
    //    @Test
    //    public void test_recordEventHistory_withValidParameters_recordsEventHistory() {
    //        // setup
    //        runUsingMockedServiceProvider(
    //                () -> {
    //                    ArgumentCaptor<Event> recordEventCapture =
    // ArgumentCaptor.forClass(Event.class);
    //                    try {
    //                        internalMessage =
    //                                (PresentableMessageMapper.InternalMessage)
    //                                        PresentableMessageMapper.getInstance()
    //                                                .createMessage(
    //                                                        mockMessagingExtension,
    //                                                        createPropositionItem(),
    //                                                        new HashMap<>(),
    //                                                        mockPropositionInfo);
    //                    } catch (Exception exception) {
    //                        fail(exception.getMessage());
    //                    }
    //
    //                    // test
    //                    internalMessage.recordEventHistory(
    //                            "mock track", MessagingEdgeEventType.INTERACT);
    //
    //                    // verify tracking event
    //                    verify(mockExtensionApi, times(1)).dispatch(recordEventCapture.capture());
    //                    Event recordEvent = recordEventCapture.getValue();
    //                    assertEquals(
    //                            MessagingTestConstants.EventName.EVENT_HISTORY_WRITE,
    //                            recordEvent.getName());
    //                    assertEquals(MessagingTestConstants.EventType.MESSAGING,
    // recordEvent.getType());
    //                    assertEquals(
    //                            MessagingTestConstants.EventSource.EVENT_HISTORY_WRITE,
    //                            recordEvent.getSource());
    //                    Map<String, Object> eventData = recordEvent.getEventData();
    //                    Map<String, String> inAppHistoryData =
    //                            (Map<String, String>)
    //
    // eventData.get(MessagingTestConstants.EventDataKeys.IAM_HISTORY);
    //                    assertNotNull(inAppHistoryData);
    //                    assertEquals(
    //                            MessagingEdgeEventType.INTERACT.getPropositionEventType(),
    //
    // inAppHistoryData.get(MessagingTestConstants.EventMask.Keys.EVENT_TYPE));
    //                    assertEquals(
    //                            mockPropositionInfo.activityId,
    //
    // inAppHistoryData.get(MessagingTestConstants.EventMask.Keys.MESSAGE_ID));
    //                    assertEquals(
    //                            "mock track",
    //                            inAppHistoryData.get(
    //                                    MessagingTestConstants.EventMask.Keys.TRACKING_ACTION));
    //                    final String[] mask = {
    //                        MessagingTestConstants.EventMask.Mask.EVENT_TYPE,
    //                        MessagingTestConstants.EventMask.Mask.MESSAGE_ID,
    //                        MessagingTestConstants.EventMask.Mask.TRACKING_ACTION
    //                    };
    //                    assertEquals(mask, recordEvent.getMask());
    //                });
    //    }
    //
    //    // TODO: - event history no longer uses Messaging extension's API - it uses
    // MobileCore.dispatch
    //    @Test
    //    public void test_recordEventHistory_MissingMessagingEdgeEventType() {
    //        // setup
    //        runUsingMockedServiceProvider(
    //                () -> {
    //                    try {
    //                        internalMessage =
    //                                (PresentableMessageMapper.InternalMessage)
    //                                        PresentableMessageMapper.getInstance()
    //                                                .createMessage(
    //                                                        mockMessagingExtension,
    //                                                        createPropositionItem(),
    //                                                        new HashMap<>(),
    //                                                        null);
    //                    } catch (Exception exception) {
    //                        fail(exception.getMessage());
    //                    }
    //
    //                    // test
    //                    internalMessage.recordEventHistory("mock track", null);
    //
    //                    // verify no tracking event
    //                    verify(mockExtensionApi, times(0)).dispatch(any());
    //                });
    //    }

    @Test
    public void test_recordEventHistory_MissingPropositionInfo() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // test
                    internalMessage.recordEventHistory(
                            "mock track", MessagingEdgeEventType.INTERACT);

                    // verify no tracking event
                    verify(mockExtensionApi, times(0)).dispatch(any());
                });
    }

    @Test
    public void test_recordEventHistory_MissingMessagingExtension() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        null,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        mockPropositionInfo);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // test
                    internalMessage.recordEventHistory(
                            "mock track", MessagingEdgeEventType.INTERACT);

                    // verify no tracking event
                    verify(mockExtensionApi, times(0)).dispatch(any());
                });
    }

    // ========================================================================================
    // getMessageFromPresentableId
    // ========================================================================================
    @Test
    public void test_getMessageFromPresentableId() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    try {
                        internalMessage =
                                (PresentableMessageMapper.InternalMessage)
                                        PresentableMessageMapper.getInstance()
                                                .createMessage(
                                                        mockMessagingExtension,
                                                        createPropositionItem(),
                                                        new HashMap<>(),
                                                        null);
                    } catch (Exception exception) {
                        fail(exception.getMessage());
                    }

                    // test
                    Message message =
                            PresentableMessageMapper.getInstance()
                                    .getMessageFromPresentableId("mockId");

                    // verify
                    assertEquals(internalMessage, message);
                });
    }

    @Test
    public void test_getMessageFromPresentableId_MessageNotAvailable() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Message message =
                            PresentableMessageMapper.getInstance()
                                    .getMessageFromPresentableId("newMockId");

                    // verify
                    assertNull(message);
                });
    }

    @Test
    public void test_getMessageFromPresentableId_PresentableIdIsNull() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Message message =
                            PresentableMessageMapper.getInstance()
                                    .getMessageFromPresentableId(null);

                    // verify
                    assertNull(message);
                });
    }

    @Test
    public void test_getMessageFromPresentableId_PresentableIdIsEmpty() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    Message message =
                            PresentableMessageMapper.getInstance().getMessageFromPresentableId("");

                    // verify
                    assertNull(message);
                });
    }
}
