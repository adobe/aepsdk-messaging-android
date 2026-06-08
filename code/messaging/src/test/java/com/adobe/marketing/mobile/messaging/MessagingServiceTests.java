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

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import androidx.core.app.NotificationManagerCompat;
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MessagingPushPayload;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.google.firebase.messaging.RemoteMessage;
import java.lang.reflect.Field;
import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingServiceTests {

    @Mock RemoteMessage remoteMessage;
    @Mock Context context;
    @Mock Application application;
    @Mock NotificationManagerCompat notificationManager;
    @Mock Notification notification;
    @Mock ServiceProvider serviceProvider;
    @Mock NamedCollection namedCollection;

    MockedStatic<MobileCore> mobileCore;
    MockedStatic<NotificationManagerCompat> notificationManagerCompat;
    MockedStatic<MessagingPushBuilder> pushBuilder;
    MockedStatic<ServiceProvider> serviceProviderStatic;

    @Before
    public void before() throws Exception {
        // Reset the static selfInitTried + MessagingExtension.registered flags between tests via
        // reflection — otherwise state leaks from one test to the next.
        resetStaticField(MessagingService.class, "selfInitTried", false);
        resetStaticField(MessagingExtension.class, "registered", false);

        // Reset Messaging.handlePushReceived's LRU dedup cache — without this, a messageId
        // tracked by an earlier test would be deduped (silently dropped) by later tests.
        final Field cacheField =
                com.adobe.marketing.mobile.Messaging.class.getDeclaredField(
                        "recentlyTrackedMessageIds");
        cacheField.setAccessible(true);
        ((java.util.Set<String>) cacheField.get(null)).clear();

        // Default: a typical AJO data payload with an _xdm field — the AJO-notification gate
        // returns true on this. Individual tests can override remoteMessage.getData() to test
        // negative cases.
        when(remoteMessage.getMessageId()).thenReturn("test-message-id");
        when(remoteMessage.getData())
                .thenReturn(
                        new HashMap<String, String>() {
                            {
                                put("_xdm", "{\"cjm\":{\"_experience\":{}}}");
                                put("adb_title", "Title");
                                put("adb_body", "Body");
                            }
                        });

        // The Application instance is the same object returned from context.getApplicationContext()
        // so the (context.getApplicationContext() instanceof Application) check in selfInit passes.
        when(context.getApplicationContext()).thenReturn(application);

        // Mock NotificationManager — notify() is a no-op; we verify it was called with the
        // built notification.
        notificationManagerCompat = mockStatic(NotificationManagerCompat.class);
        notificationManagerCompat
                .when(() -> NotificationManagerCompat.from(any(Context.class)))
                .thenReturn(notificationManager);
        doNothing().when(notificationManager).notify(anyInt(), any());

        // Mock MobileCore — initialize / setApplication / setPushIdentifier / dispatchEvent
        // are all no-ops by default; tests can capture args via ArgumentCaptor.
        mobileCore = mockStatic(MobileCore.class);

        // Mock the push notification builder — always returns the same notification object.
        pushBuilder = mockStatic(MessagingPushBuilder.class);
        pushBuilder
                .when(
                        () ->
                                MessagingPushBuilder.build(
                                        any(MessagingPushPayload.class), any(Context.class)))
                .thenReturn(notification);

        // Mock ServiceProvider so that the NamedCollection returned by the data store service
        // is the mocked collection — tests can stub it to return a cached appId or null.
        serviceProviderStatic = mockStatic(ServiceProvider.class);
        serviceProviderStatic.when(ServiceProvider::getInstance).thenReturn(serviceProvider);
        when(serviceProvider.getDataStoreService())
                .thenReturn(
                        new com.adobe.marketing.mobile.services.DataStoring() {
                            @Override
                            public NamedCollection getNamedCollection(final String name) {
                                return namedCollection;
                            }
                        });
    }

    @After
    public void clean() {
        mobileCore.close();
        pushBuilder.close();
        notificationManagerCompat.close();
        serviceProviderStatic.close();
    }

    /**
     * Helper: reset a private/package-private static field on a class so each test starts with a
     * clean slate. Required because {@code MessagingService.selfInitTried} and {@code
     * MessagingExtension.registered} persist across tests in the same JVM run.
     */
    private static void resetStaticField(
            final Class<?> clazz, final String name, final Object value) throws Exception {
        final Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    // =====================================================================
    // onNewToken
    // =====================================================================

    @Test
    public void test_onNewToken_forwardsTokenToMobileCore() {
        final String validToken = "fcm-token-abc";
        final MessagingService service = new MessagingService();

        service.onNewToken(validToken);

        mobileCore.verify(() -> MobileCore.setPushIdentifier(validToken));
    }

    // =====================================================================
    // onMessageReceived
    // =====================================================================

    @Test
    public void test_onMessageReceived_delegatesToHandleRemoteMessage() {
        try (MockedStatic<MessagingService> messagingServiceMockedStatic =
                Mockito.mockStatic(MessagingService.class)) {
            messagingServiceMockedStatic
                    .when(
                            () ->
                                    MessagingService.handleRemoteMessage(
                                            any(Context.class), any(RemoteMessage.class)))
                    .thenReturn(true);

            new MessagingService().onMessageReceived(remoteMessage);

            messagingServiceMockedStatic.verify(
                    () ->
                            MessagingService.handleRemoteMessage(
                                    any(Context.class), eq(remoteMessage)));
        }
    }

    // =====================================================================
    // handleRemoteMessage — AJO-notification gate (isAJONotification)
    // =====================================================================

    @Test
    public void test_handleRemoteMessage_nonAjoPayload_returnsFalseAndDoesNothing() {
        when(remoteMessage.getData())
                .thenReturn(
                        new HashMap<String, String>() {
                            {
                                put("custom_key", "custom_value");
                            }
                        });

        final boolean handled = MessagingService.handleRemoteMessage(context, remoteMessage);

        assertFalse(handled);
        verify(notificationManager, never()).notify(anyInt(), any(Notification.class));
        mobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());
    }

    @Test
    public void test_handleRemoteMessage_titleOnlyPayload_treatedAsAjo() {
        // Per isAJONotification: an "adb_title" key alone qualifies (Assurance-spoofed pushes).
        when(remoteMessage.getData())
                .thenReturn(
                        new HashMap<String, String>() {
                            {
                                put("adb_title", "Assurance spoofed title");
                            }
                        });

        final boolean handled = MessagingService.handleRemoteMessage(context, remoteMessage);

        assertTrue(handled);
    }

    // =====================================================================
    // handleRemoteMessage — messageId null/empty guard (PR review #7)
    // =====================================================================

    @Test
    public void test_handleRemoteMessage_nullMessageId_returnsFalse() {
        when(remoteMessage.getMessageId()).thenReturn(null);

        final boolean handled = MessagingService.handleRemoteMessage(context, remoteMessage);

        assertFalse(handled);
        verify(notificationManager, never()).notify(anyInt(), any(Notification.class));
        mobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());
    }

    @Test
    public void test_handleRemoteMessage_emptyMessageId_returnsFalse() {
        when(remoteMessage.getMessageId()).thenReturn("");

        final boolean handled = MessagingService.handleRemoteMessage(context, remoteMessage);

        assertFalse(handled);
        verify(notificationManager, never()).notify(anyInt(), any(Notification.class));
        mobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());
    }

    // =====================================================================
    // handleRemoteMessage — fast path (extension already registered)
    // =====================================================================

    @Test
    public void test_handleRemoteMessage_extensionRegistered_dispatchesSynchronously()
            throws Exception {
        // Simulate that MessagingExtension is already registered with EventHub.
        resetStaticField(MessagingExtension.class, "registered", true);
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        final boolean handled = MessagingService.handleRemoteMessage(context, remoteMessage);

        assertTrue(handled);

        // Notification must be displayed.
        verify(notificationManager, times(1)).notify(anyInt(), eq(notification));

        // The push-received Edge event is dispatched synchronously — self-init is never engaged.
        mobileCore.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
        final Event dispatched = eventCaptor.getValue();
        assertNotNull(dispatched);
        assertEquals(EventType.MESSAGING, dispatched.getType());
        assertEquals(EventSource.REQUEST_CONTENT, dispatched.getSource());
        assertEquals("Push notification received", dispatched.getName());
        assertEquals("test-message-id", dispatched.getEventData().get("messageId"));
        assertEquals(true, dispatched.getEventData().get("pushnotificationreceived"));
        assertEquals("pushTracking.receive", dispatched.getEventData().get("eventType"));

        // MobileCore.initialize must NOT be called because the extension is already registered.
        mobileCore.verify(
                () -> MobileCore.initialize(any(Application.class), anyString(), any()), never());
    }

    // =====================================================================
    // handleRemoteMessage — slow path (extension not registered → selfInit)
    // =====================================================================

    @Test
    public void test_handleRemoteMessage_extensionNotRegistered_cachedAppIdPresent_runsSelfInit() {
        // Cached appId is present → self-init proceeds to MobileCore.initialize.
        when(namedCollection.getString(eq("config.appID"), any())).thenReturn("cached-app-id");
        final ArgumentCaptor<AdobeCallback<?>> callbackCaptor =
                ArgumentCaptor.forClass(AdobeCallback.class);

        final boolean handled = MessagingService.handleRemoteMessage(context, remoteMessage);

        assertTrue(handled);

        // Notification still displays — synchronous, independent of self-init.
        verify(notificationManager, times(1)).notify(anyInt(), eq(notification));

        // setApplication and initialize both called.
        mobileCore.verify(() -> MobileCore.setApplication(application));
        mobileCore.verify(
                () ->
                        MobileCore.initialize(
                                eq(application), anyString(), callbackCaptor.capture()));

        // The dispatch is deferred — no Edge event fires until the initialize callback runs.
        mobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());

        // Invoke the captured callback (simulating Core completing initialization).
        callbackCaptor.getValue().call(null);

        // Now the push-received event fires.
        mobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), times(1));
    }

    @Test
    public void test_handleRemoteMessage_extensionNotRegistered_noCachedAppId_dropsDispatch() {
        // No cached appId → self-init aborts before calling MobileCore.initialize, and the
        // deferred dispatch never fires.
        when(namedCollection.getString(eq("config.appID"), any())).thenReturn(null);

        final boolean handled = MessagingService.handleRemoteMessage(context, remoteMessage);

        assertTrue(handled);

        // Notification still displays even when there's no cached appId.
        verify(notificationManager, times(1)).notify(anyInt(), eq(notification));

        // MobileCore.initialize must not be called.
        mobileCore.verify(
                () -> MobileCore.initialize(any(Application.class), anyString(), any()), never());

        // No dispatch — the receive event is dropped (deliberately, since there's nothing to
        // bootstrap from).
        mobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());
    }

    @Test
    public void test_handleRemoteMessage_extensionNotRegistered_emptyCachedAppId_dropsDispatch() {
        // Empty string cached appId is treated the same as null.
        when(namedCollection.getString(eq("config.appID"), any())).thenReturn("");

        final boolean handled = MessagingService.handleRemoteMessage(context, remoteMessage);

        assertTrue(handled);
        verify(notificationManager, times(1)).notify(anyInt(), eq(notification));
        mobileCore.verify(
                () -> MobileCore.initialize(any(Application.class), anyString(), any()), never());
    }

    @Test
    public void test_handleRemoteMessage_contextNotApplication_abortsSelfInit() {
        // context.getApplicationContext() returns something that is NOT an Application instance.
        when(context.getApplicationContext()).thenReturn(context);

        final boolean handled = MessagingService.handleRemoteMessage(context, remoteMessage);

        assertTrue(handled);
        // Notification still displays.
        verify(notificationManager, times(1)).notify(anyInt(), eq(notification));
        // Self-init does not call setApplication or initialize.
        mobileCore.verify(() -> MobileCore.setApplication(any(Application.class)), never());
        mobileCore.verify(
                () -> MobileCore.initialize(any(Application.class), anyString(), any()), never());
    }

    @Test
    public void test_handleRemoteMessage_extensionNotRegistered_nullNamedCollection_dropsDispatch() {
        when(serviceProvider.getDataStoreService())
                .thenReturn(
                        new com.adobe.marketing.mobile.services.DataStoring() {
                            @Override
                            public NamedCollection getNamedCollection(final String name) {
                                return null;
                            }
                        });

        final boolean handled = MessagingService.handleRemoteMessage(context, remoteMessage);

        assertTrue(handled);
        verify(notificationManager, times(1)).notify(anyInt(), eq(notification));
        mobileCore.verify(
                () -> MobileCore.initialize(any(Application.class), anyString(), any()), never());
        mobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());
    }

    @Test
    public void
            test_handleRemoteMessage_extensionNotRegistered_readCachedAppIdThrows_dropsDispatch() {
        when(serviceProvider.getDataStoreService())
                .thenReturn(
                        new com.adobe.marketing.mobile.services.DataStoring() {
                            @Override
                            public NamedCollection getNamedCollection(final String name) {
                                throw new RuntimeException("datastore unavailable");
                            }
                        });

        final boolean handled = MessagingService.handleRemoteMessage(context, remoteMessage);

        assertTrue(handled);
        verify(notificationManager, times(1)).notify(anyInt(), eq(notification));
        mobileCore.verify(
                () -> MobileCore.initialize(any(Application.class), anyString(), any()), never());
        mobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());
    }

    @Test
    public void test_handleRemoteMessage_selfInitTriedAlready_runsDeferredDispatchImmediately()
            throws Exception {
        // First call: cached appId present → self-init runs and flips selfInitTried to true.
        when(namedCollection.getString(eq("config.appID"), any())).thenReturn("cached-app-id");
        final ArgumentCaptor<AdobeCallback<?>> firstCallbackCaptor =
                ArgumentCaptor.forClass(AdobeCallback.class);
        MessagingService.handleRemoteMessage(context, remoteMessage);
        mobileCore.verify(
                () ->
                        MobileCore.initialize(
                                eq(application), anyString(), firstCallbackCaptor.capture()));
        firstCallbackCaptor.getValue().call(null);
        mobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), times(1));

        // Second call (same process, same messageId still false-registered): selfInitTried is now
        // true, so self-init's early-return path fires the runnable immediately without calling
        // MobileCore.initialize a second time.
        when(remoteMessage.getMessageId()).thenReturn("test-message-id-2");
        MessagingService.handleRemoteMessage(context, remoteMessage);

        // initialize was only called once across both pushes.
        mobileCore.verify(
                () -> MobileCore.initialize(any(Application.class), anyString(), any()), times(1));
        // dispatchEvent fires for both pushes.
        mobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), times(2));
    }
}
