package com.adobe.marketing.mobile;

import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({ExtensionApi.class, ExtensionUnexpectedError.class, MessagingState.class, PlatformServices.class, LocalStorageService.class, ExperiencePlatform.class, ExperiencePlatformEvent.class, App.class})
public class MessagingInternalTest {

    private MessagingInternal messagingInternal;

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
    @Mock
    ExtensionUnexpectedError mockExtensionUnexpectedError;
    @Mock
    MessagingState messagingState;
    @Mock
    PlatformServices mockPlatformServices;
    @Mock
    LocalStorageService mockLocalStorageService;
    @Mock
    NetworkService mockNetworkService;
    @Mock
    Map<String, Object> mockConfigData;
    @Mock
    ConcurrentLinkedQueue<Event> mockEventQueue;
    @Mock
    Application mockApplication;

    @Before
    public void setup() {
        PowerMockito.mockStatic(ExperiencePlatform.class);
        PowerMockito.mockStatic(ExperiencePlatformEvent.class);
        PowerMockito.mockStatic(App.class);
        messagingInternal = new MessagingInternal(mockExtensionApi);
    }

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    public void test_Constructor() {
        // verify 3 listeners are registered
        verify(mockExtensionApi, times(1)).registerListener(eq(EventType.CONFIGURATION),
                eq(EventSource.RESPONSE_CONTENT), eq(ConfigurationResponseContentListener.class));
        verify(mockExtensionApi, times(1)).registerListener(eq(EventType.GENERIC_DATA),
                eq(EventSource.OS), eq(GenericDataOSListener.class));
        verify(mockExtensionApi, times(1)).registerListener(eq(EventType.GENERIC_IDENTITY),
                eq(EventSource.REQUEST_CONTENT), eq(IdentityRequestContentListener.class));
    }

    // ========================================================================================
    // getName
    // ========================================================================================
    @Test
    public void test_getName() {
        // test
        String moduleName = messagingInternal.getName();
        assertEquals("getName should return the correct module name", MessagingConstant.EXTENSION_NAME, moduleName);
    }

    // ========================================================================================
    // getVersion
    // ========================================================================================
    @Test
    public void test_getVersion() {
        // test
        String moduleVersion = messagingInternal.getVersion();
        assertEquals("getVesion should return the correct module version", MessagingConstant.EXTENSION_VERSION,
                moduleVersion);
    }

    // ========================================================================================
    // onUnexpectedError
    // ========================================================================================
    @Test
    public void test_onUnexpectedError() {
        // test
        messagingInternal.onUnexpectedError(mockExtensionUnexpectedError);
        verify(mockExtensionApi, times(1)).clearSharedEventStates(null);
    }

    // ========================================================================================
    // onUnregistered
    // ========================================================================================

    @Test
    public void test_onUnregistered() {
        // test
        messagingInternal.onUnregistered();
        verify(mockExtensionApi, times(1)).clearSharedEventStates(null);
    }

    // ========================================================================================
    // queueEvent
    // ========================================================================================
    @Test
    public void test_QueueEvent() {
        // test 1
        assertNotNull("EventQueue instance is should never be null", messagingInternal.getEventQueue());

        // test 2
        Event sampleEvent = new Event.Builder("event 1", "eventType", "eventSource").build();
        messagingInternal.queueEvent(sampleEvent);
        assertEquals("The size of the eventQueue should be correct", 1, messagingInternal.getEventQueue().size());

        // test 3
        messagingInternal.queueEvent(null);
        assertEquals("The size of the eventQueue should be correct", 1, messagingInternal.getEventQueue().size());

        // test 4
        Event anotherEvent = new Event.Builder("event 2", "eventType", "eventSource").build();
        messagingInternal.queueEvent(anotherEvent);
        assertEquals("The size of the eventQueue should be correct", 2, messagingInternal.getEventQueue().size());
    }

    // ========================================================================================
    // processEvents
    // ========================================================================================
    @Test
    public void test_processEvents_when_noEventInQueue() {
        // Mocks
        ExtensionErrorCallback<ExtensionError> mockCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {

            }
        };
        Event mockEvent = new Event.Builder("event 2", "eventType", "eventSource").build();

        // test
        messagingInternal.processEvents();

        // verify
        verify(mockExtensionApi, times(0)).getSharedEventState(MessagingConstant.SharedState.Configuration.EXTENSION_NAME, mockEvent, mockCallback);
        verify(mockExtensionApi, times(0)).getSharedEventState(MessagingConstant.SharedState.Identity.EXTENSION_NAME, mockEvent, mockCallback);
    }

    @Test
    public void test_processEvents_when_handlingPushToken_withPrivacyOptIn() {
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        Whitebox.setInternalState(messagingInternal, "platformServices", mockPlatformServices);

        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_token");
        Event mockEvent = new Event.Builder("handlePushToken", EventType.GENERIC_IDENTITY.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        // when configState containsKey return true
        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class),
                any(ExtensionErrorCallback.class))).thenReturn(mockConfigData);
        when(mockConfigData.containsKey(anyString())).thenReturn(true);

        // when getLocalStorageService() return mockLocalStorageService
        LocalStorageService.DataStore mockDataStore = mock(LocalStorageService.DataStore.class);
        when(mockPlatformServices.getLocalStorageService()).thenReturn(mockLocalStorageService);
        when(mockLocalStorageService.getDataStore(anyString())).thenReturn(mockDataStore);

        // when get privacy status retirn opt in
        when(messagingState.getPrivacyStatus()).thenReturn(MobilePrivacyStatus.OPT_IN);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify
        verify(mockPlatformServices, times(1)).getLocalStorageService();
        verify(mockPlatformServices, times(1)).getNetworkService();
        verify(mockLocalStorageService, times(1)).getDataStore("AdobeMobile_ExperienceMessage");
        verify(mockDataStore, times(1)).setString(anyString(), anyString());
    }

    @Test
    public void test_processEvents_when_handlingTrackingInfo() {
        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_event_type");
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_message_id");
        Event mockEvent = new Event.Builder("handleTrackingInfo", EventType.GENERIC_DATA.getName(), EventSource.OS.getName()).setEventData(eventData).build();

        // when configState containsKey return true
        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class),
                any(ExtensionErrorCallback.class))).thenReturn(mockConfigData);
        when(mockConfigData.containsKey(anyString())).thenReturn(true);

        // when getExperienceEventDatasetId return mock datasetId
        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_dataset_id");

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify
        PowerMockito.verifyStatic(ExperiencePlatform.class, times(1));
        ExperiencePlatform.sendEvent(any(ExperiencePlatformEvent.class), any(ExperiencePlatformCallback.class));
    }

    // ========================================================================================
    // processConfigurationResponse
    // ========================================================================================
    @Test
    public void test_processConfigurationResponse_when_NullEvent() {
        // test
        messagingInternal.processConfigurationResponse(null);

        // verify
        verify(messagingState, times(0)).setState(any(EventData.class), any(EventData.class));
    }

    @Test
    public void test_processConfigurationResponse_when_privacyOptOut() {
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        Whitebox.setInternalState(messagingInternal, "platformServices", mockPlatformServices);
        Whitebox.setInternalState(messagingInternal, "eventQueue", mockEventQueue);


        // Mocks
        Event mockEvent = new Event.Builder("event1", EventType.CONFIGURATION.getName(), EventSource.RESPONSE_CONTENT.getName()).setEventData(mockConfigData).build();
        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class))).thenReturn(mockEvent.getData());

        // when
        when(messagingState.getPrivacyStatus()).thenReturn(MobilePrivacyStatus.OPT_OUT);

        // when getLocalStorageService() return mockLocalStorageService
        LocalStorageService.DataStore mockDataStore = mock(LocalStorageService.DataStore.class);
        when(mockPlatformServices.getLocalStorageService()).thenReturn(mockLocalStorageService);
        when(mockLocalStorageService.getDataStore(anyString())).thenReturn(mockDataStore);

        //test
        messagingInternal.processConfigurationResponse(mockEvent);

        // verify
        verify(messagingState, times(1)).setState(mockEvent.getData(), mockEvent.getData());
        verify(messagingState, times(1)).getPrivacyStatus();
        verify(mockPlatformServices, times(1)).getLocalStorageService();
        verify(mockLocalStorageService, times(1)).getDataStore("AdobeMobile_ExperienceMessage");
        verify(mockDataStore, times(1)).remove("pushIdentifier");
        verify(mockEventQueue, times(1)).clear();
    }

    @Test
    public void test_processConfigurationResponse_when_privacyOptIn() {
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        Whitebox.setInternalState(messagingInternal, "platformServices", mockPlatformServices);
        Whitebox.setInternalState(messagingInternal, "eventQueue", mockEventQueue);

        // Mocks
        Event mockEvent = new Event.Builder("event1", EventType.CONFIGURATION.getName(), EventSource.RESPONSE_CONTENT.getName()).setEventData(mockConfigData).build();
        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class))).thenReturn(mockEvent.getData());
        // Mocks
        ExtensionErrorCallback<ExtensionError> mockCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {

            }
        };

        // when
        when(messagingState.getPrivacyStatus()).thenReturn(MobilePrivacyStatus.OPT_IN);

        // when getLocalStorageService() return mockLocalStorageService
        LocalStorageService.DataStore mockDataStore = mock(LocalStorageService.DataStore.class);
        when(mockPlatformServices.getLocalStorageService()).thenReturn(mockLocalStorageService);
        when(mockLocalStorageService.getDataStore(anyString())).thenReturn(mockDataStore);

        //test
        messagingInternal.processConfigurationResponse(mockEvent);

        // verify
        verify(messagingState, times(1)).setState(mockEvent.getData(), mockEvent.getData());
        verify(messagingState, times(1)).getPrivacyStatus();
        verify(mockPlatformServices, times(0)).getLocalStorageService();
        verify(mockEventQueue, times(0)).clear();
        verify(mockExtensionApi, times(0)).getSharedEventState(MessagingConstant.SharedState.Configuration.EXTENSION_NAME, mockEvent, mockCallback);
        verify(mockExtensionApi, times(0)).getSharedEventState(MessagingConstant.SharedState.Identity.EXTENSION_NAME, mockEvent, mockCallback);
    }

    // ========================================================================================
    // handlePushToken
    // ========================================================================================
    @Test
    public void test_handlePushToken_when_WrongEventType() {
        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_push_token");
        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_DATA.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        Whitebox.setInternalState(messagingInternal, "platformServices", mockPlatformServices);

        //test
        messagingInternal.handlePushToken(mockEvent);

        // verify
        verify(messagingState, times(0)).getPrivacyStatus();
    }

    @Test
    public void test_handlePushToken_when_privacyOptOut() {
        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_push_token");
        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_IDENTITY.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        Whitebox.setInternalState(messagingInternal, "platformServices", mockPlatformServices);

        // when
        when(messagingState.getPrivacyStatus()).thenReturn(MobilePrivacyStatus.OPT_OUT);

        //test
        messagingInternal.handlePushToken(mockEvent);

        // verify
        verify(messagingState, times(2)).getPrivacyStatus();
        verify(mockPlatformServices, times(0)).getLocalStorageService();
        verify(mockPlatformServices, times(0)).getNetworkService();
    }

    @Test
    public void test_handlePushToken_when_privacyOptIn() {
        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_push_token");
        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_IDENTITY.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();
        String mockECID = "mock_ecid";
        String mockDccsUrl = "mock_dccs_url";
        String mockExperienceCloudOrg = "mock_exp_org";
        String mockProfileDatasetId = "mock_profileDatasetId";

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        Whitebox.setInternalState(messagingInternal, "platformServices", mockPlatformServices);

        // when
        when(messagingState.getPrivacyStatus()).thenReturn(MobilePrivacyStatus.OPT_IN);
        when(messagingState.getEcid()).thenReturn(mockECID);
        when(messagingState.getDccsURL()).thenReturn(mockDccsUrl);
        when(messagingState.getProfileDatasetId()).thenReturn(mockProfileDatasetId);
        when(messagingState.getExperienceCloudOrg()).thenReturn(mockExperienceCloudOrg);

        // when getLocalStorageService() return mockLocalStorageService
        LocalStorageService.DataStore mockDataStore = mock(LocalStorageService.DataStore.class);
        when(mockPlatformServices.getLocalStorageService()).thenReturn(mockLocalStorageService);
        when(mockLocalStorageService.getDataStore(anyString())).thenReturn(mockDataStore);

        // when getNetworkService() return mockNetworkService
        when(mockPlatformServices.getNetworkService()).thenReturn(mockNetworkService);

        // when App.getApplication().getPackageName() return mock packageName
        when(App.getApplication()).thenReturn(mockApplication);
        when(mockApplication.getPackageName()).thenReturn("mock_package");

        //test
        messagingInternal.handlePushToken(mockEvent);

        // verify
        verify(messagingState, times(2)).getPrivacyStatus();
        verify(mockPlatformServices, times(1)).getLocalStorageService();
        verify(mockPlatformServices, times(1)).getNetworkService();
        verify(mockNetworkService, times(1)).connectUrl(anyString(), any(NetworkService.HttpCommand.class), any(byte[].class), ArgumentMatchers.<String, String>anyMap(), anyInt(), anyInt());
    }
}