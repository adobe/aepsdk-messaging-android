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

package com.adobe.marketing.mobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A third party extension class aiding for assertion against dispatched events, shared state
 * and XDM shared state.
 */
class MonitorExtension extends Extension {
	private static final String LOG_TAG = "MonitorExtension";

	private static final Map<EventSpec, List<Event>> receivedEvents = new HashMap<>();
	private static final Map<EventSpec, ADBCountDownLatch> expectedEvents = new HashMap<>();

	protected MonitorExtension(ExtensionApi extensionApi) {
		super(extensionApi);

		extensionApi.registerWildcardListener(
		MonitorListener.class, new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(ExtensionError extensionError) {
				MobileCore.log(LoggingMode.ERROR, LOG_TAG,
							   "There was an error registering Extension Listener: " +
							   extensionError.getErrorName());
			}
		});
	}

	@Override
	protected String getName() {
		return "MonitorExtension";
	}

	public static void registerExtension() {
		MobileCore.registerExtension(MonitorExtension.class, new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(ExtensionError extensionError) {
				MobileCore.log(LoggingMode.ERROR, LOG_TAG,
							   "There was an error registering the Monitor extension: " + extensionError.getErrorName());
			}
		});
	}

	/**
	 * Unregister the Monitor Extension from the EventHub.
	 */
	public static void unregisterExtension() {
		Event event = new Event.Builder("Unregister Monitor Extension Request", TestConstants.EventType.MONITOR,
										TestConstants.EventSource.UNREGISTER)
		.build();
		MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(ExtensionError extensionError) {
				MobileCore.log(LoggingMode.ERROR, LOG_TAG, "Failed to unregister Monitor extension.");
			}
		});
	}

	/**
	 * Add an event to the list of expected events.
	 * @param type the type of the event.
	 * @param source the source of the event.
	 * @param count the number of events expected to be received.
	 */
	public static void setExpectedEvent(final String type, final String source, final int count) {
		EventSpec eventSpec = new EventSpec(source, type);
		expectedEvents.put(eventSpec, new ADBCountDownLatch(count));
	}

	public static Map<EventSpec, ADBCountDownLatch> getExpectedEvents() {
		return expectedEvents;
	}

	public static Map<EventSpec, List<Event>> getReceivedEvents() {
		return receivedEvents;
	}

	/**
	 * Resets the map of received and expected events.
	 */
	public static void reset() {
		MobileCore.log(LoggingMode.VERBOSE, LOG_TAG, "Reset expected and received events.");
		receivedEvents.clear();
		expectedEvents.clear();
	}

	/**
	 * Processor for all heard events.
	 * If the event type is of this Monitor Extension, then
	 * the action is performed per the event source.
	 * All other events are added to the map of received events. If the event is in the map
	 * of expected events, its latch is counted down.
	 *
	 * @param event
	 */
	public void wildcardProcessor(final Event event) {
		if (TestConstants.EventType.MONITOR.equalsIgnoreCase(event.getType())) {
			if (TestConstants.EventSource.SHARED_STATE_REQUEST.equalsIgnoreCase(event.getSource())) {
				processSharedStateRequest(event);
			} else if (TestConstants.EventSource.XDM_SHARED_STATE_REQUEST.equalsIgnoreCase(event.getSource())) {
				processXDMSharedStateRequest(event);
			} else if (TestConstants.EventSource.UNREGISTER.equalsIgnoreCase(event.getSource())) {
				processUnregisterRequest(event);
			}

			return;
		}

		EventSpec eventSpec = new EventSpec(event.getSource(), event.getType());

		MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Received and processing event " + eventSpec);

		if (!receivedEvents.containsKey(eventSpec)) {
			receivedEvents.put(eventSpec, new ArrayList<Event>());
		}

		receivedEvents.get(eventSpec).add(event);


		if (expectedEvents.containsKey(eventSpec)) {
			expectedEvents.get(eventSpec).countDown();
		}
	}

	/**
	 * Processor which unregisters this extension.
	 * @param event
	 */
	private void processUnregisterRequest(final Event event) {
		MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Unregistering the Monitor Extension.");
		getApi().unregisterExtension();
	}

	/**
	 * Processor which retrieves and dispatches the XDM shared state for the state owner specified
	 * in the request.
	 * @param event
	 */
	private void processXDMSharedStateRequest(final Event event) {
		EventData eventData = event.getData();

		if (eventData == null) {
			return;
		}

		String stateOwner = eventData.optString(TestConstants.EventDataKey.STATE_OWNER, null);

		if (stateOwner == null) {
			return;
		}

		EventData sharedState = getApi().getXDMSharedEventState(stateOwner, event);

		Event responseEvent = new Event.Builder("Get Shared State Response", TestConstants.EventType.MONITOR,
												TestConstants.EventSource.XDM_SHARED_STATE_RESPONSE)
		.setEventData(sharedState == null ? null : sharedState.toObjectMap())
		.setPairID(event.getResponsePairID())
		.build();

		MobileCore.dispatchResponseEvent(responseEvent, event, null);
	}

	/**
	 * Processor which retrieves and dispatches the shared state for the state owner specified
	 * in the request.
	 * @param event
	 */
	private void processSharedStateRequest(final Event event) {
		EventData eventData = event.getData();

		if (eventData == null) {
			return;
		}

		String stateOwner = eventData.optString(TestConstants.EventDataKey.STATE_OWNER, null);

		if (stateOwner == null) {
			return;
		}

		EventData sharedState = getApi().getSharedEventState(stateOwner, event);

		Event responseEvent = new Event.Builder("Get Shared State Response", TestConstants.EventType.MONITOR,
												TestConstants.EventSource.SHARED_STATE_RESPONSE)
		.setEventData(sharedState == null ? null : sharedState.toObjectMap())
		.setPairID(event.getResponsePairID())
		.build();

		MobileCore.dispatchResponseEvent(responseEvent, event, null);
	}

	/**
	 * Listener class
	 */
	public static class MonitorListener extends ExtensionListener {

		protected MonitorListener(ExtensionApi extension, String type, String source) {
			super(extension, type, source);
		}

		@Override
		public void hear(Event event) {
			MonitorExtension extension = getParentExtension();

			if (extension != null) {
				extension.wildcardProcessor(event);
			}
		}

		@Override
		protected MonitorExtension getParentExtension() {
			return (MonitorExtension) super.getParentExtension();
		}
	}

	/**
	 * Class defining {@link Event} specifications, contains Event's source and type.
	 */
	public static class EventSpec {
		final String source;
		final String type;

		public EventSpec(final String source, final String type) {
			if (source == null || source.isEmpty()) {
				throw new IllegalArgumentException("Event Source cannot be null or empty.");
			}

			if (type == null || type.isEmpty()) {
				throw new IllegalArgumentException("Event Type cannot be null or empty.");
			}

			// Normalize strings
			this.source = source.toLowerCase();
			this.type = type.toLowerCase();
		}

		@Override
		public String toString() {
			return "type '" + type + "' and source '" + source + "'";
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			EventSpec eventSpec = (EventSpec) o;
			return Objects.equals(source, eventSpec.source) &&
				   Objects.equals(type, eventSpec.type);
		}

		@Override
		public int hashCode() {
			return Objects.hash(source, type);
		}
	}
}