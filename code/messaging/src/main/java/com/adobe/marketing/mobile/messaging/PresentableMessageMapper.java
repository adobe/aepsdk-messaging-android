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


import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.Message;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.InAppMessage;
import com.adobe.marketing.mobile.services.ui.Presentable;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.services.ui.message.InAppMessageSettings;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DefaultPresentationUtilityProvider;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

class PresentableMessageMapper {

    private static final Map<String, Message> presentableMessageMap = new HashMap<>();

    private static class PresentableMessageMapperSingleton {
        private static final PresentableMessageMapper INSTANCE = new PresentableMessageMapper();
    }

    /**
     * Singleton method to get the instance of PresentableMessageMapper
     *
     * @return the {@link PresentableMessageMapper} singleton
     */
    static PresentableMessageMapper getInstance() {
        return PresentableMessageMapper.PresentableMessageMapperSingleton.INSTANCE;
    }

    private PresentableMessageMapper() {}

     /**
      * Creates a {@link Message} object
      * <p>
      * Every {@code InternalMessage} requires a {@link RuleConsequence#getId()}, and must be of type "cjmiam".
      * <p>
      * The consequence {@code Map} for a {@code InternalMessage} is required to have valid values for the following fields:
      * <ul>
      *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_ID} - {@code String} containing the message ID</li>
      *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_TYPE} - {@code String} containing the consequence type</li>
      *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_DETAIL} - {@code Map<String, Object>} containing details of the Message to be displayed</li>
      * </ul>
      *
      * @param messagingExtension             {@link MessagingExtension} instance that created this Message
      * @param propositionItem                {@link PropositionItem} instance containing item data of type {@link InAppSchemaData}
      * @param assetMap           {@code Map<String, Object>} containing a mapping of a remote image asset URL and it's cached location
      * @throws MessageRequiredFieldMissingException if the consequence {@code Map} fails validation.
      * @throws IllegalStateException if {@link UIService} is unavailable
      */
    Message createMessage(final MessagingExtension messagingExtension,
                          final PropositionItem propositionItem,
                          final Map<String, String> assetMap,
                          final PropositionInfo propositionInfo) throws MessageRequiredFieldMissingException, IllegalStateException {
        final Message existingInternalMessage = findExistingInternalMessage(propositionItem);
        if (existingInternalMessage != null) {
            return existingInternalMessage;
        }
        final InternalMessage internalMessage = new InternalMessage(messagingExtension, propositionItem, assetMap, propositionInfo);
        presentableMessageMap.put(internalMessage.aepMessage.getPresentation().getId(), internalMessage);
        return internalMessage;
    }

    private Message findExistingInternalMessage(final PropositionItem propositionItem) {
        if (propositionItem == null) {
            return null;
        }
        for (final Message message : presentableMessageMap.values()) {
            if (message.getId().equals(propositionItem.getItemId())) {
                return message;
            }
        }
        return null;
    }

    @VisibleForTesting
    void clearPresentableMessageMap() {
        presentableMessageMap.clear();
    }

    /**
     * Retrieves a {@code Message} object associated with the given presentableId from the internally maintained map.
     *
     * @param presentableId The ID of the {@link Presentable}  message to retrieve.
     * @return The {@link Message} object associated with the given presentableId, or null if no such message exists or the presentableId is null or empty.
     */
    @Nullable
    Message getMessageFromPresentableId(final String presentableId) {
        if (StringUtils.isNullOrEmpty(presentableId)) {
            return null;
        }
        return presentableMessageMap.get(presentableId);
    }

    static class InternalMessage implements Message {
        private final static String SELF_TAG = "Message";
        private final static int FILL_SCREEN = 100;
        private final String id;
        private final MessagingExtension messagingExtension;
        private final Presentable<InAppMessage> aepMessage;

        private boolean autoTrack = true;
        // package private
        PropositionInfo propositionInfo; // contains XDM data necessary for tracking in-app interactions with Adobe Journey Optimizer

         /**
          * Constructor.
          * <p>
          * Every {@link InternalMessage} requires a {@link #id}, and must be of type "cjmiam".
          * <p>
          * The consequence {@code Map} for a {@code InternalMessage} is required to have valid values for the following fields:
          * <ul>
          *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_ID} - {@code String} containing the message ID</li>
          *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_TYPE} - {@code String} containing the consequence type</li>
          *     <li>{@value MessagingConstants.EventDataKeys.RulesEngine#MESSAGE_CONSEQUENCE_DETAIL} - {@code Map<String, Object>} containing details of the Message to be displayed</li>
          * </ul>
          *
          * @param parent             {@link MessagingExtension} instance that created this Message
          * @param propositionItem                {@link PropositionItem} instance containing item data of type {@link InAppSchemaData}
          * @param assetMap           {@code Map<String, Object>} containing a mapping of a remote image asset URL and it's cached location
          * @throws MessageRequiredFieldMissingException if the consequence {@code Map} fails validation.
         */
        private InternalMessage(final MessagingExtension parent,
                                final PropositionItem propositionItem,
                                final Map<String, String> assetMap,
                                final PropositionInfo propositionInfo) throws MessageRequiredFieldMissingException, IllegalStateException {
            messagingExtension = parent;
            this.propositionInfo = propositionInfo;

            if(propositionItem == null) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to create an in-app message, PropositionItem is null.");
                throw new MessageRequiredFieldMissingException("Required field: \"PropositionItem\" is null.");
            }

            id = propositionItem.getItemId();

            final SchemaType schemaType = propositionItem.getSchema();
            if (!(SchemaType.INAPP == schemaType)) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Required field \"schema\" is (%s) should be of type (%S).", schemaType, MessagingConstants.SchemaValues.SCHEMA_IAM);
                throw new MessageRequiredFieldMissingException("Required field: \"schema\" is not equal to \"https://ns.adobe.com/personalization/message/in-app\".");
            }

            final InAppSchemaData inAppSchemaData = propositionItem.getInAppSchemaData();
            if (inAppSchemaData == null) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "In-app message proposition item data is null or empty.");
                throw new MessageRequiredFieldMissingException("Required field: in-app message proposition item \"data\" is null or empty.");
            }

            try {
                final String html = (String) inAppSchemaData.getContent();
                if (StringUtils.isNullOrEmpty(html)) {
                    Log.warning(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to create an in-app message, the html payload is null or empty.");
                    throw new MessageRequiredFieldMissingException("Required field: in-app message \"content\" is null or empty.");
                }

                final InAppMessageSettings settings = InAppMessageSettingsFromMap(inAppSchemaData.getMobileParameters(), html, assetMap);

                final UIService uiService = ServiceProvider.getInstance().getUIService();
                if (uiService == null) {
                    Log.warning(MessagingConstants.LOG_TAG, SELF_TAG, "The UIService is unavailable. Aborting in-app message creation.");
                    throw new IllegalStateException("The UIService is unavailable");
                }

                aepMessage = uiService.create(new InAppMessage(settings, new MessagingFullscreenEventListener()), new DefaultPresentationUtilityProvider());
            } catch (final ClassCastException exception) {
                Log.warning(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to create an in-app message, in-app message content is not of type String.");
                throw new MessageRequiredFieldMissingException("Required field: in-app message content is not of type String.");
            }
        }

        /**
         * Dispatch tracking information via a Messaging request content event.
         *
         * @param interaction {@code String} containing the interaction which occurred
         * @param eventType   {@link MessagingEdgeEventType} enum containing the Event Type to be used for the ensuing Edge Event
         */
        public void track(final String interaction, final MessagingEdgeEventType eventType) {
            if (eventType == null) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to send a proposition interaction, MessagingEdgeEventType was null.");
                return;
            }
            if (propositionInfo == null) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to send a proposition interaction (%s), PropositionInfo is not found for message (%s)", eventType.getPropositionEventType(), id);
                return;
            }
            if (messagingExtension == null) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to send a proposition interaction (%s), MessagingExtension is not found for message (%s)", eventType.getPropositionEventType(), id);
                return;
            }
            final PropositionInteraction propositionInteraction = new PropositionInteraction(eventType,
                    interaction == null ? "" : interaction, propositionInfo, null, null);
            final Map<String, Object> propositionInteractionXdm = propositionInteraction.getPropositionInteractionXDM();
            messagingExtension.sendPropositionInteraction(propositionInteractionXdm);
        }

        // ui management
        @Override
        public void show() {
            if (aepMessage != null) {
                aepMessage.show();
            }
        }

        public void dismiss() {
            if (aepMessage != null) {
                aepMessage.dismiss();
            }
        }

        /**
         * Called when a {@code Message} is triggered - i.e. it's conditional criteria have been met.
         */
        void trigger() {
            if (aepMessage != null) {
                if (autoTrack) {
                    track(null, MessagingEdgeEventType.TRIGGER);
                }
                recordEventHistory(null, MessagingEdgeEventType.TRIGGER);
            }
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean getAutoTrack() {
            return autoTrack;
        }

        @Override
        public void setAutoTrack(final boolean useAutoTrack) {
            this.autoTrack = useAutoTrack;
        }

        /**
         * Dispatches an event to be recorded in Event History.
         *
         * @param interaction {@code String}  if provided, adds a custom interaction to the hash
         * @param eventType {@link MessagingEdgeEventType} to be recorded
         */
        void recordEventHistory(final String interaction, final MessagingEdgeEventType eventType) {
            if (eventType == null) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to write event history event, MessagingEdgeEventType was null for message (%s).", id);
                return;
            }
            if (propositionInfo == null) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to write event history event (%s), PropositionInfo is not found for message (%s)", eventType.getPropositionEventType(), id);
                return;
            }
            if (messagingExtension == null) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to send a proposition interaction (%s), MessagingExtension is not found for message (%s)", eventType.getPropositionEventType(), id);
                return;
            }
            // create maps for event history
            final Map<String, String> iamHistoryMap = new HashMap<>();
            iamHistoryMap.put(MessagingConstants.EventMask.Keys.EVENT_TYPE, eventType.getPropositionEventType());
            iamHistoryMap.put(MessagingConstants.EventMask.Keys.MESSAGE_ID, propositionInfo.activityId);
            iamHistoryMap.put(MessagingConstants.EventMask.Keys.TRACKING_ACTION, (StringUtils.isNullOrEmpty(interaction) ? "" : interaction));

            // Create the mask for storing event history
            final Map<String, Object> eventHistoryData = new HashMap<>();
            eventHistoryData.put(MessagingConstants.EventDataKeys.IAM_HISTORY, iamHistoryMap);
            final String[] mask = {MessagingConstants.EventMask.Mask.EVENT_TYPE, MessagingConstants.EventMask.Mask.MESSAGE_ID, MessagingConstants.EventMask.Mask.TRACKING_ACTION};

            InternalMessagingUtils.sendEvent(MessagingConstants.EventName.EVENT_HISTORY_WRITE,
                    MessagingConstants.EventType.MESSAGING,
                    MessagingConstants.EventSource.EVENT_HISTORY_WRITE,
                    eventHistoryData,
                    mask,
                    messagingExtension.getApi());
        }

        /**
         * Sample mobile parameters payload represented by a InAppMessageSettings object:
         * {
         * "mobileParameters": {
         * "schemaVersion": "1.0",
         * "width": 80,
         * "height": 50,
         * "verticalAlign": "center",
         * "verticalInset": 0,
         * "horizontalAlign": "center",
         * "horizontalInset": 0,
         * "uiTakeover": true,
         * "displayAnimation": "top",
         * "dismissAnimation": "top",
         * "backdropColor": "000000", // RRGGBB
         * "backdropOpacity: 0.3,
         * "cornerRadius": 15,
         * "gestures": {
         * "swipeUp": "adbinapp://dismiss",
         * "swipeDown": "adbinapp://dismiss",
         * "swipeLeft": "adbinapp://dismiss?interaction=negative",
         * "swipeRight": "adbinapp://dismiss?interaction=positive",
         * "tapBackground": "adbinapp://dismiss"
         * }
         * }
         * }
         */
        private InAppMessageSettings InAppMessageSettingsFromMap(final Map<String, Object> rawSettings,
                                                                 final String content,
                                                                 final Map<String, String> assetMap) {

            final int width = DataReader.optInt(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.WIDTH, FILL_SCREEN);
            final int height = DataReader.optInt(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.HEIGHT, FILL_SCREEN);
            final InAppMessageSettings.MessageAlignment verticalAlign = InAppMessageSettings.MessageAlignment.valueOf((DataReader.optString(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.VERTICAL_ALIGN, "center").toUpperCase()));
            final int verticalInset = DataReader.optInt(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.VERTICAL_INSET, 0);
            final InAppMessageSettings.MessageAlignment horizontalAlign = InAppMessageSettings.MessageAlignment.valueOf((DataReader.optString(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.HORIZONTAL_ALIGN, "center").toUpperCase()));
            final int horizontalInset = DataReader.optInt(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.HORIZONTAL_INSET, 0);
            final InAppMessageSettings.MessageAnimation displayAnimation = InAppMessageSettings.MessageAnimation.valueOf((DataReader.optString(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.DISPLAY_ANIMATION, "none").toUpperCase()));
            final InAppMessageSettings.MessageAnimation dismissAnimation = InAppMessageSettings.MessageAnimation.valueOf((DataReader.optString(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.DISMISS_ANIMATION, "none").toUpperCase()));
            final String backdropColor = DataReader.optString(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.BACKDROP_COLOR, "#FFFFFF");
            final float backdropOpacity = DataReader.optFloat(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.BACKDROP_OPACITY, 0.0f);
            final float cornerRadius = DataReader.optFloat(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.CORNER_RADIUS, 0.0f);
            final boolean uiTakeover = DataReader.optBoolean(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.UI_TAKEOVER, true);

            // we need to convert key strings present in the gestures map to InAppInAppMessageSettings.MessageGesture enum keys
            final Map<String, String> gestureMap = DataReader.optStringMap(rawSettings, MessagingConstants.EventDataKeys.MobileParametersKeys.GESTURES, new HashMap<>());

            return new InAppMessageSettings.Builder()
                    .content(content)
                    .width(width)
                    .height(height)
                    .verticalInset(verticalInset)
                    .horizontalInset(horizontalInset)
                    .verticalAlignment(verticalAlign)
                    .horizontalAlignment(horizontalAlign)
                    .displayAnimation(displayAnimation)
                    .dismissAnimation(dismissAnimation)
                    .backgroundColor(backdropColor)
                    .backdropOpacity(backdropOpacity)
                    .cornerRadius(cornerRadius)
                    .shouldTakeOverUi(uiTakeover)
                    .gestureMap(gestureMap)
                    .assetMap(assetMap)
                    .build();
        }
    }
}
