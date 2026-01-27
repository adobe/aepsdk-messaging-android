package com.adobe.marketing.mobile.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.MockedStatic;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingRuleEngineInterceptorTests {

    @Mock
    LaunchRulesEngine.CompletionCallback mockCallback;

    @Mock
    Event mockEvent;

    @Before
    public void setup() {
        // Any setup if needed
    }

    @Test
    public void test_onReevaluationTriggered_dispatchesRefreshEvent_andRegistersHandler() {
        // Arrange
        MessagingRuleEngineInterceptor interceptor = new MessagingRuleEngineInterceptor();

        try (MockedStatic<MobileCore> mobileCoreMockedStatic = mockStatic(MobileCore.class);
             MockedStatic<MessagingExtension> messagingExtensionMockedStatic = mockStatic(MessagingExtension.class)) {
            // Act
            interceptor.onReevaluationTriggered(
                mockEvent,
                Collections.emptyList(),
                mockCallback
            );

            // Assert
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(any(Event.class)), times(1));
            messagingExtensionMockedStatic.verify(() -> MessagingExtension.addCompletionHandler(any(CompletionHandler.class)), times(1));

            // Capture and verify event details
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            Event sentEvent = eventCaptor.getValue();

            // Check event type, source, name, and data
            assert sentEvent != null;
            assert EventType.MESSAGING.equals(sentEvent.getType());
            assert EventSource.REQUEST_CONTENT.equals(sentEvent.getSource());
            assert "Refresh in-app messages".equals(sentEvent.getName());
            assert sentEvent.getEventData().containsKey("refreshmessages");
            assert Boolean.TRUE.equals(sentEvent.getEventData().get("refreshmessages"));
        }
    }

    @Test
    public void test_onReevaluationTriggered_callbackIsCalledOnCompletion() {
        // Arrange
        MessagingRuleEngineInterceptor interceptor = new MessagingRuleEngineInterceptor();

        try (MockedStatic<MobileCore> mobileCoreMockedStatic = mockStatic(MobileCore.class);
             MockedStatic<MessagingExtension> messagingExtensionMockedStatic = mockStatic(MessagingExtension.class)) {
            // Act
            interceptor.onReevaluationTriggered(
                mockEvent,
                Collections.emptyList(),
                mockCallback
            );

            // Capture completion handler registered
            ArgumentCaptor<CompletionHandler> handlerCaptor = ArgumentCaptor.forClass(CompletionHandler.class);
            messagingExtensionMockedStatic.verify(() -> MessagingExtension.addCompletionHandler(handlerCaptor.capture()), times(1));
            CompletionHandler handler = handlerCaptor.getValue();

            // Simulate completion
            handler.handle.call(true);
            // Assert callback was called
            verify(mockCallback, times(1)).onComplete();
        }
    }

    @Test
    public void test_onReevaluationTriggered_handlesNullCallback() {
        // Arrange
        MessagingRuleEngineInterceptor interceptor = new MessagingRuleEngineInterceptor();

        try (MockedStatic<MobileCore> mobileCoreMockedStatic = mockStatic(MobileCore.class);
             MockedStatic<MessagingExtension> messagingExtensionMockedStatic = mockStatic(MessagingExtension.class)) {
            // Act
            interceptor.onReevaluationTriggered(
                mockEvent,
                Collections.emptyList(),
                null // callback is null
            );
            // Assert: no crash, event still dispatched
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(any(Event.class)), times(1));
            messagingExtensionMockedStatic.verify(() -> MessagingExtension.addCompletionHandler(any(CompletionHandler.class)), times(1));
        }
    }
}