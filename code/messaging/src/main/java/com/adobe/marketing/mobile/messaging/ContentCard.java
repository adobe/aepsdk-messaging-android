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

import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;

/**
 * A {@link ContentCard} object encapsulates the information necessary for a non-disruptive yet
 * interactive offer. Customers can use the Messaging SDK to render the content card in a
 * pre-defined format or implement their own rendering.
 */
public class ContentCard {
    private static final String SELF_TAG = "ContentCard";

    // Plain-text title for the content card
    private String title;
    // Plain-text body representing the content for the content card
    private String body;
    // String representing a URI that contains an image to be used for this content card
    private String imageUrl;
    // Contains a URL to be opened if the user interacts with the content card
    private String actionUrl;
    // Required if actionUrl is provided. Text to be used in title of button or link in content card
    private String actionTitle;
    // Reference to parent ContentCardSchemaData instance
    ContentCardSchemaData parent;

    /**
     * Private constructor.
     *
     * <p>Use {@link Builder} to create {@link ContentCard} object.
     */
    private ContentCard() {}

    /** {@code FeedItem} Builder. */
    public static class Builder {
        private boolean didBuild;
        private final ContentCard contentCard;

        /**
         * Builder constructor with required {@code ContentCard} attributes as parameters.
         *
         * <p>It sets default values for the remaining {@link ContentCard} attributes.
         *
         * @param title required {@link String} plain-text title for the content card
         * @param body required {@link String} plain-text body representing the content for the
         *     content card
         */
        public Builder(final String title, final String body) {
            contentCard = new ContentCard();
            contentCard.title = StringUtils.isNullOrEmpty(title) ? "" : title;
            contentCard.body = StringUtils.isNullOrEmpty(body) ? "" : body;
            contentCard.imageUrl = "";
            contentCard.actionUrl = "";
            contentCard.actionTitle = "";
            didBuild = false;
        }

        /**
         * Sets the image url for this {@code ContentCard}.
         *
         * @param imageUrl {@link String} containing the {@link ContentCard} image url.
         * @return this ContentCard {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link
         *     Builder#build()}.
         */
        public Builder setImageUrl(final String imageUrl) {
            throwIfAlreadyBuilt();

            contentCard.imageUrl = imageUrl;
            return this;
        }

        /**
         * Sets the action url for this {@code ContentCard}.
         *
         * @param actionUrl {@link String} containing the {@link ContentCard} action url.
         * @return this ContentCard {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link
         *     Builder#build()}.
         */
        public Builder setActionUrl(final String actionUrl) {
            throwIfAlreadyBuilt();

            contentCard.actionUrl = actionUrl;
            return this;
        }

        /**
         * Sets the action title for this {@code ContentCard}.
         *
         * @param actionTitle {@link String} containing the {@link ContentCard} action title.
         * @return this ContentCard {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link
         *     Builder#build()}.
         */
        public Builder setActionTitle(final String actionTitle) {
            throwIfAlreadyBuilt();

            contentCard.actionTitle = actionTitle;
            return this;
        }

        /**
         * Sets the {@code ContentCardSchemaData} parent object.
         *
         * @param parent {@link ContentCardSchemaData} object which is the parent for this {@link
         *     ContentCard}.
         * @return this FeedItem {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link
         *     Builder#build()}.
         */
        public Builder setParent(final ContentCardSchemaData parent) {
            throwIfAlreadyBuilt();

            contentCard.parent = parent;
            return this;
        }

        /**
         * Builds and returns the {@code ContentCard} object.
         *
         * @return {@link ContentCard} object or null.
         */
        public ContentCard build() {
            // title and body are required
            if (StringUtils.isNullOrEmpty(contentCard.title)
                    || StringUtils.isNullOrEmpty(contentCard.body)) {
                return null;
            }

            throwIfAlreadyBuilt();
            didBuild = true;

            return contentCard;
        }

        private void throwIfAlreadyBuilt() {
            if (didBuild) {
                throw new UnsupportedOperationException(
                        "Attempted to call methods on FeedItem.Builder after build() was invoked.");
            }
        }
    }

    /**
     * Gets the {@code ContentCard} title.
     *
     * @return {@link String} containing the {@link ContentCard} title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the {@code ContentCard} body.
     *
     * @return {@link String} containing the {@link ContentCard} body.
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the {@code ContentCard} image url.
     *
     * @return {@link String} containing the {@link ContentCard} image url.
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Gets the {@code ContentCard} action url.
     *
     * @return {@link String} containing the {@link ContentCard} action url.
     */
    public String getActionUrl() {
        return actionUrl;
    }

    /**
     * Gets the {@code ContentCard} action title.
     *
     * @return {@link String} containing the {@link ContentCard} action title.
     */
    public String getActionTitle() {
        return actionTitle;
    }

    /**
     * Tracks interaction with the given content card.
     *
     * @param interaction {@link String} describing the interaction.
     * @param eventType enum of type {@link MessagingEdgeEventType} specifying event type for the
     *     interaction.
     */
    public void track(final String interaction, final MessagingEdgeEventType eventType) {
        if (parent == null) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Unable to track ContentCard, " + "parent schema object is unavailable.");
            return;
        }
        parent.track(interaction, eventType);
    }
}
