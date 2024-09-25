/*
  Copyright 2021 Adobe. All rights reserved.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.Logging;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.ConflictingPresentation;
import com.adobe.marketing.mobile.services.ui.InAppMessage;
import com.adobe.marketing.mobile.services.ui.Presentable;
import com.adobe.marketing.mobile.services.ui.PresentationError;
import com.adobe.marketing.mobile.services.ui.SuppressedByAppDeveloper;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.services.ui.message.InAppMessageEventHandler;
import com.adobe.marketing.mobile.services.uri.UriOpening;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingFullscreenEventListenerTests {

    @Mock ServiceProvider mockServiceProvider;
    @Mock Logging mockLogging;
    @Mock UIService mockUIService;
    @Mock UriOpening mockUriOpening;
    @Mock PresentableMessageMapper mockPresentableMessageMapper;
    @Mock PresentableMessageMapper.InternalMessage mockMessage;
    @Mock Presentable<InAppMessage> mockInAppPresentable;
    @Mock InAppMessage mockPresentation;
    @Mock PresentationError mockPresentationError;
    @Mock ConflictingPresentation mockConflictingPresentationError;
    @Mock SuppressedByAppDeveloper mockSuppressedByAppDeveloperError;
    @Mock InAppMessageEventHandler mockEventHandler;
    @Captor ArgumentCaptor<String> urlStringCaptor;

    private MessagingFullscreenEventListener eventListener;

    @Before
    public void setup() {
        try {
            eventListener = new MessagingFullscreenEventListener();
        } catch (Exception exception) {
            fail(exception.getMessage());
        }
        Log.setLogLevel(LoggingMode.VERBOSE);
    }

    @After
    public void tearDown() {
        reset(mockServiceProvider);
        reset(mockLogging);
        reset(mockUIService);
        reset(mockUriOpening);
        reset(mockPresentableMessageMapper);
        reset(mockMessage);
        reset(mockInAppPresentable);
        reset(mockPresentation);
        reset(mockPresentationError);
        reset(mockConflictingPresentationError);
        reset(mockSuppressedByAppDeveloperError);
        reset(mockEventHandler);
    }

    void runWithMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                        Mockito.mockStatic(ServiceProvider.class);
                MockedStatic<PresentableMessageMapper> presentableMessageMapperMockedStatic =
                        Mockito.mockStatic(PresentableMessageMapper.class)) {
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getLoggingService()).thenReturn(mockLogging);
            when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
            when(mockServiceProvider.getUriService()).thenReturn(mockUriOpening);
            presentableMessageMapperMockedStatic
                    .when(PresentableMessageMapper::getInstance)
                    .thenReturn(mockPresentableMessageMapper);
            when(mockPresentableMessageMapper.getMessageFromPresentableId(anyString()))
                    .thenReturn(mockMessage);
            when(mockInAppPresentable.getPresentation()).thenReturn(mockPresentation);
            when(mockPresentation.getId()).thenReturn("mockId");
            when(mockConflictingPresentationError.getReason()).thenReturn("Conflict");
            when(mockSuppressedByAppDeveloperError.getReason()).thenReturn("SuppressedByAppDeveloper");
            runnable.run();
        }
    }

    @Test
    public void test_onMessageShow_withTracking() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockMessage.getAutoTrack()).thenReturn(true);

                    // test
                    eventListener.onShow(mockInAppPresentable);

                    // verify
                    verify(mockMessage, times(1)).track(null, MessagingEdgeEventType.DISPLAY);
                    verify(mockMessage, times(1))
                            .recordEventHistory(null, MessagingEdgeEventType.DISPLAY);
                });
    }

    @Test
    public void test_onMessageShow_withAutoTrackDisabled() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockMessage.getAutoTrack()).thenReturn(false);

                    // test
                    eventListener.onShow(mockInAppPresentable);

                    // verify
                    verify(mockMessage, times(0)).track(null, MessagingEdgeEventType.DISPLAY);
                    verify(mockMessage, times(1))
                            .recordEventHistory(null, MessagingEdgeEventType.DISPLAY);
                });
    }

    @Test
    public void test_onMessageShow_presentableMessageNotInMap() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockPresentableMessageMapper.getMessageFromPresentableId(anyString()))
                            .thenReturn(null);

                    // test
                    eventListener.onShow(mockInAppPresentable);

                    // verify
                    verify(mockMessage, times(0)).track(null, MessagingEdgeEventType.DISPLAY);
                    verify(mockMessage, times(0))
                            .recordEventHistory(null, MessagingEdgeEventType.DISPLAY);
                });
    }

    @Test
    public void test_onMessageDismiss_withAutoTrack() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockMessage.getAutoTrack()).thenReturn(true);

                    // test
                    eventListener.onDismiss(mockInAppPresentable);

                    // verify
                    verify(mockMessage, times(1)).track(null, MessagingEdgeEventType.DISMISS);
                    verify(mockMessage, times(1))
                            .recordEventHistory(null, MessagingEdgeEventType.DISMISS);
                });
    }

    @Test
    public void test_onMessageDismiss_withAutoTrackDisabled() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockMessage.getAutoTrack()).thenReturn(false);

                    // test
                    eventListener.onDismiss(mockInAppPresentable);

                    // verify
                    verify(mockMessage, times(0)).track(null, MessagingEdgeEventType.DISMISS);
                    verify(mockMessage, times(1))
                            .recordEventHistory(null, MessagingEdgeEventType.DISMISS);
                });
    }

    @Test
    public void test_onMessageDismiss_presentableMessageNotInMap() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockPresentableMessageMapper.getMessageFromPresentableId(anyString()))
                            .thenReturn(null);

                    // test
                    eventListener.onDismiss(mockInAppPresentable);

                    // verify
                    verify(mockMessage, times(0)).track(null, MessagingEdgeEventType.DISMISS);
                    verify(mockMessage, times(0))
                            .recordEventHistory(null, MessagingEdgeEventType.DISMISS);
                });
    }

    @Test
    public void test_onMessageError() {
        runWithMockedServiceProvider(
                () -> {
                    try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
                        // test
                        eventListener.onError(mockInAppPresentable, mockPresentationError);

                        // verify
                        verify(mockMessage, times(0)).track(anyString(), any());
                        verify(mockMessage, times(0))
                                .recordEventHistory(anyString(), any());
                        logMockedStatic.verify(
                                () -> Log.debug(anyString(), anyString(), anyString()), times(1));
                    }
                });
    }

    @Test
    public void test_onMessageError_ConflictingPresentationError_AutoTrackDisabled() {
        runWithMockedServiceProvider(
                () -> {
                    try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
                        // test
                        eventListener.onError(mockInAppPresentable, mockConflictingPresentationError);

                        // verify
                        verify(mockMessage, times(0)).track("Conflict", MessagingEdgeEventType.SUPPRESSED_DISPLAY);
                        verify(mockMessage, times(1))
                                .recordEventHistory("Conflict", MessagingEdgeEventType.SUPPRESSED_DISPLAY);
                        logMockedStatic.verify(
                                () -> Log.debug(anyString(), anyString(), anyString()), times(1));
                    }
                });
    }

    @Test
    public void test_onMessageError_ConflictingPresentationError_AutoTrackEnabled() {
        runWithMockedServiceProvider(
                () -> {
                    try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
                        // setup
                        when(mockMessage.getAutoTrack()).thenReturn(true);
                        // test
                        eventListener.onError(mockInAppPresentable, mockConflictingPresentationError);

                        // verify
                        verify(mockMessage, times(1)).track("Conflict", MessagingEdgeEventType.SUPPRESSED_DISPLAY);
                        verify(mockMessage, times(1))
                                .recordEventHistory("Conflict", MessagingEdgeEventType.SUPPRESSED_DISPLAY);
                        logMockedStatic.verify(
                                () -> Log.debug(anyString(), anyString(), anyString()), times(1));
                    }
                });
    }

    @Test
    public void test_onMessageError_SuppressedByAppDeveloperError_AutoTrackDisabled() {
        runWithMockedServiceProvider(
                () -> {
                    try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
                        // test
                        eventListener.onError(mockInAppPresentable, mockSuppressedByAppDeveloperError);

                        // verify
                        verify(mockMessage, times(0)).track("SuppressedByAppDeveloper", MessagingEdgeEventType.SUPPRESSED_DISPLAY);
                        verify(mockMessage, times(1))
                                .recordEventHistory("SuppressedByAppDeveloper", MessagingEdgeEventType.SUPPRESSED_DISPLAY);
                        logMockedStatic.verify(
                                () -> Log.debug(anyString(), anyString(), anyString()), times(1));
                    }
                });
    }

    @Test
    public void test_onMessageError_SuppressedByAppDeveloperError_AutoTrackEnabled() {
        runWithMockedServiceProvider(
                () -> {
                    try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
                        // setup
                        when(mockMessage.getAutoTrack()).thenReturn(true);
                        // test
                        eventListener.onError(mockInAppPresentable, mockSuppressedByAppDeveloperError);

                        // verify
                        verify(mockMessage, times(1)).track("SuppressedByAppDeveloper", MessagingEdgeEventType.SUPPRESSED_DISPLAY);
                        verify(mockMessage, times(1))
                                .recordEventHistory("SuppressedByAppDeveloper", MessagingEdgeEventType.SUPPRESSED_DISPLAY);
                        logMockedStatic.verify(
                                () -> Log.debug(anyString(), anyString(), anyString()), times(1));
                    }
                });
    }

    @Test
    public void test_openUrlWithAdbDeeplink() {
        // setup
        runWithMockedServiceProvider(
                () -> {
                    // test
                    eventListener.openUrl("adb_deeplink://signup");

                    // verify the ui service is called to handle the deeplink
                    verify(mockUriOpening, times(1)).openUri(urlStringCaptor.capture());
                    assertEquals("adb_deeplink://signup", urlStringCaptor.getValue());
                });
    }

    @Test
    public void test_openUrlWithWebLink() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    eventListener.openUrl("https://www.adobe.com");

                    // verify the ui service is called to show the url
                    verify(mockUriOpening, times(1)).openUri(urlStringCaptor.capture());
                    assertEquals("https://www.adobe.com", urlStringCaptor.getValue());
                });
    }

    @Test
    public void test_openUrlWithNullUrl() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    eventListener.openUrl(null);

                    // verify no internal open url method is called
                    verify(mockUriOpening, times(0)).openUri(urlStringCaptor.capture());
                });
    }

    @Test
    public void test_onUrlLoading_nullUrlString() {
        runWithMockedServiceProvider(
                () -> {
                    // setup

                    // test
                    boolean result = eventListener.onUrlLoading(mockInAppPresentable, null);

                    // verify no message tracking call
                    assertTrue(result);
                    verify(mockPresentableMessageMapper, times(0))
                            .getMessageFromPresentableId(anyString());
                    verify(mockMessage, times(0)).track(any(), any(MessagingEdgeEventType.class));
                });
    }

    @Test
    public void test_onUrlLoading_emptyUrlString() {
        runWithMockedServiceProvider(
                () -> {
                    // setup

                    // test
                    boolean result = eventListener.onUrlLoading(mockInAppPresentable, "");

                    // verify no message tracking call
                    assertTrue(result);
                    verify(mockPresentableMessageMapper, times(0))
                            .getMessageFromPresentableId(anyString());
                    verify(mockMessage, times(0)).track(any(), any(MessagingEdgeEventType.class));
                });
    }

    @Test
    public void test_onUrlLoading_withInvalidUri() {
        runWithMockedServiceProvider(
                () -> {
                    // setup

                    // test
                    boolean result = eventListener.onUrlLoading(mockInAppPresentable, "{invalid}");

                    // verify no message tracking call
                    assertTrue(result);
                    verify(mockPresentableMessageMapper, times(0))
                            .getMessageFromPresentableId(anyString());
                    verify(mockMessage, times(0)).track(any(), any(MessagingEdgeEventType.class));
                });
    }

    @Test
    public void test_onUrlLoading_withInvalidScheme() {
        runWithMockedServiceProvider(
                () -> {
                    // setup

                    // test
                    boolean result =
                            eventListener.onUrlLoading(
                                    mockInAppPresentable, "notadbinapp://com.adobe.com");

                    // verify no message tracking call and message settings weren't created
                    Assert.assertFalse(result);
                    verify(mockMessage, times(0)).track(any(), any(MessagingEdgeEventType.class));
                    verify(mockPresentableMessageMapper, times(0))
                            .getMessageFromPresentableId(anyString());
                });
    }

    @Test
    public void test_overrideUrlLoad_URLWithNoQueryParameters() {
        runWithMockedServiceProvider(
                () -> {
                    // setup

                    // test
                    boolean result =
                            eventListener.onUrlLoading(mockInAppPresentable, "adbinapp://dismiss");

                    // verify no message tracking call and message settings weren't created
                    Assert.assertFalse(result);
                    verify(mockMessage, times(0)).track(any(), any(MessagingEdgeEventType.class));
                    verify(mockPresentableMessageMapper, times(0))
                            .getMessageFromPresentableId(anyString());
                });
    }

    @Test
    public void test_overrideUrlLoad_MissingInteractionInURLQueryParams() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    boolean result =
                            eventListener.onUrlLoading(
                                    mockInAppPresentable, "adbinapp://dismiss?q=1");

                    // verify no message tracking call and message settings weren't created
                    Assert.assertTrue(result);
                    verify(mockPresentableMessageMapper, times(1))
                            .getMessageFromPresentableId(anyString());
                    verify(mockMessage, times(0)).track(any(), any(MessagingEdgeEventType.class));
                });
    }

    @Test
    public void test_overrideUrlLoad_InternalMessageForPresentableNotAvailable() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockPresentableMessageMapper.getMessageFromPresentableId(anyString()))
                            .thenReturn(null);

                    // test
                    boolean result =
                            eventListener.onUrlLoading(
                                    mockInAppPresentable,
                                    "adbinapp://dismiss?interaction=deeplink");

                    // verify no message tracking call and message settings weren't created
                    Assert.assertTrue(result);
                    verify(mockPresentableMessageMapper, times(1))
                            .getMessageFromPresentableId(anyString());
                    verify(mockMessage, times(0)).track(any(), any(MessagingEdgeEventType.class));
                });
    }

    @Test
    public void test_onUrlLoading_withJavascriptPayload() {
        runWithMockedServiceProvider(
                () -> {
                    // setup

                    when(mockPresentation.getEventHandler()).thenReturn(mockEventHandler);

                    // test
                    eventListener.onUrlLoading(
                            mockInAppPresentable,
                            "adbinapp://dismiss?interaction=javascript&link=js%3D%28function%28%29+%7B+return+%27javascript+value%27%3B+%7D%29%28%29%3B");

                    // verify no message tracking call and message settings weren't created
                    verify(mockMessage, times(1))
                            .track(eq("javascript"), eq(MessagingEdgeEventType.INTERACT));
                    ArgumentCaptor<String> stringArgumentCaptor =
                            ArgumentCaptor.forClass(String.class);
                    verify(mockEventHandler, times(1))
                            .evaluateJavascript(stringArgumentCaptor.capture(), any());
                    assertEquals(
                            "js=(function() { return 'javascript value'; })();",
                            stringArgumentCaptor.getValue());
                });
    }

    @Test
    public void test_onUrlLoading_withDeeplink() {
        runWithMockedServiceProvider(
                () -> {
                    // setup

                    when(mockPresentation.getEventHandler()).thenReturn(mockEventHandler);

                    // test
                    eventListener.onUrlLoading(
                            mockInAppPresentable,
                            "adbinapp://dismiss?interaction=deeplink&link=scheme%3A%2F%2Fparameters%3Fparam1%3Dvalue1%26param2%3Dvalue2");

                    // verify no message tracking call and message settings weren't created
                    verify(mockMessage, times(0))
                            .track(eq("deeplink"), eq(MessagingEdgeEventType.DISMISS));
                    ArgumentCaptor<String> stringArgumentCaptor =
                            ArgumentCaptor.forClass(String.class);
                    verify(mockUriOpening, times(1)).openUri(stringArgumentCaptor.capture());
                    assertEquals(
                            "scheme://parameters?param1=value1&param2=value2",
                            stringArgumentCaptor.getValue());
                });
    }

    @Test
    public void test_onBackPressed_WithAutoTrack() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockMessage.getAutoTrack()).thenReturn(true);

                    // test
                    eventListener.onBackPressed(mockInAppPresentable);

                    // verify
                    verify(mockMessage, times(1))
                            .track(
                                    MessagingFullscreenEventListener.INTERACTION_BACK_PRESS,
                                    MessagingEdgeEventType.INTERACT);
                    verify(mockMessage, times(1))
                            .recordEventHistory(
                                    MessagingFullscreenEventListener.INTERACTION_BACK_PRESS,
                                    MessagingEdgeEventType.INTERACT);
                });
    }

    @Test
    public void test_onBackPressed_WithAutoTrackDisabled() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockMessage.getAutoTrack()).thenReturn(false);

                    // test
                    eventListener.onBackPressed(mockInAppPresentable);

                    // verify
                    verify(mockMessage, times(0))
                            .track(
                                    MessagingFullscreenEventListener.INTERACTION_BACK_PRESS,
                                    MessagingEdgeEventType.DISMISS);
                    verify(mockMessage, times(1))
                            .recordEventHistory(
                                    MessagingFullscreenEventListener.INTERACTION_BACK_PRESS,
                                    MessagingEdgeEventType.INTERACT);
                });
    }

    @Test
    public void test_onBackPressed_InternalMessageForPresentableNotAvailable() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockPresentableMessageMapper.getMessageFromPresentableId(anyString()))
                            .thenReturn(null);
                    when(mockMessage.getAutoTrack()).thenReturn(true);

                    // test
                    eventListener.onBackPressed(mockInAppPresentable);

                    // verify
                    verify(mockMessage, times(0))
                            .track(
                                    MessagingFullscreenEventListener.INTERACTION_BACK_PRESS,
                                    MessagingEdgeEventType.DISMISS);
                    verify(mockMessage, times(0))
                            .recordEventHistory(
                                    MessagingFullscreenEventListener.INTERACTION_BACK_PRESS,
                                    MessagingEdgeEventType.INTERACT);
                });
    }
}
