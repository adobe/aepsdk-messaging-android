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

package com.adobe.marketing.mobile;

public interface Message {
    /**
     * Dispatch tracking information via a Messaging request content event.
     *
     * @param interaction {@code String} containing the interaction which occurred
     * @param eventType {@link MessagingEdgeEventType} enum containing the Event Type to be used for
     *     the ensuing Edge Event
     */
    void track(final String interaction, final MessagingEdgeEventType eventType);

    /** Shows this {@link Message}. */
    void show();

    /** Removes this {@link Message} from view. */
    void dismiss();

    /** Returns the {@link Message}'s id. */
    String getId();

    /**
     * Sets the {@link Message}'s auto tracking preference.
     *
     * @param enabled {@code boolean} if true, tracking is done automatically for {@code Message}
     *     show, dismiss, and triggered events.
     */
    void setAutoTrack(final boolean enabled);

    /**
     * Gets the {@link Message}'s auto tracking preference.
     *
     * @return {@code boolean} containing the auto tracking preference.
     */
    default boolean getAutoTrack() {
        return true;
    }
}
