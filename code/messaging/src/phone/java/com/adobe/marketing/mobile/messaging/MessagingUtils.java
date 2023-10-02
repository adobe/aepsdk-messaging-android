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

import androidx.annotation.RestrictTo;

import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagingUtils {
    // ========================================================================================
    // Collection utils
    // ========================================================================================

    /**
     * Checks if the given {@code collection} is null or empty.
     *
     * @param collection input {@code Collection<?>} to be tested.
     * @return {@code boolean} result indicating whether the provided {@code collection} is null or empty.
     */
    private static boolean isNullOrEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Returns a mutable {@code List<T>} list containing a single element.
     *
     * @param element A {@link T} to be added to the mutable list
     * @return the mutable {@link List<T>} list
     */
    private static <T> List<T> createMutableList(final T element) {
        return new ArrayList<T>() {
            {
                add(element);
            }
        };
    }

    /**
     * Returns a mutable {@code List<T>} list containing a single element.
     *
     * @param list A {@link List<T>} to be converted to a mutable list
     * @return the mutable {@link List<T>} list
     */
    private static <T> List<T> createMutableList(final List<T> list) {
        return new ArrayList<>(list);
    }

    /**
     * Updates the provided {@code Map<Surface, List<Proposition>>} with the provided {@code Surface} and {@code List<Proposition>} objects.
     *
     * @param surface           A {@link Surface} key used to update a {@link List<Proposition>} value in the provided {@link Map<Surface, List<Proposition>>}
     * @param propositionsToAdd A {@link List<Proposition>} list to add in the provided {@code Map<Surface, List<Proposition>>}
     * @param mapToUpdate       The {@code Map<Surface, List<Proposition>>} to be updated with the provided {@code Surface} and {@code List<Proposition>} objects
     * @return the updated {@link Map<Surface, List<Proposition>>} map
     */
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public static Map<Surface, List<Proposition>> updatePropositionMapForSurface(final Surface surface, final List<Proposition> propositionsToAdd, Map<Surface, List<Proposition>> mapToUpdate) {
        if (isNullOrEmpty(propositionsToAdd)) {
            return mapToUpdate;
        }
        final Map<Surface, List<Proposition>> updatedMap = new HashMap<>(mapToUpdate);
        final List<Proposition> list = updatedMap.get(surface) != null ? updatedMap.get(surface) : createMutableList(propositionsToAdd);
        if (updatedMap.get(surface) != null) {
            list.addAll(propositionsToAdd);
        }
        updatedMap.put(surface, list);
        return updatedMap;
    }

    /**
     * Updates the provided {@code Map<Surface, List<Proposition>>} map with the provided {@code Surface} and {@code Proposition} objects.
     *
     * @param surface     A {@link Surface} key used to update a {@link List<Proposition>} value in the provided {@link Map<Surface, List<Proposition>>}
     * @param proposition A {@link Proposition} object to add in the provided {@code Map<Surface, List<Proposition>>}
     * @param mapToUpdate The {@code Map<Surface, List<Proposition>>} to be updated with the provided {@code Surface} and {@code List<Proposition>} objects
     * @return the updated {@link Map<Surface, List<Proposition>>} map
     */
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public static Map<Surface, List<Proposition>> updatePropositionMapForSurface(final Surface surface, final Proposition proposition, Map<Surface, List<Proposition>> mapToUpdate) {
        if (proposition == null) {
            return mapToUpdate;
        }
        final Map<Surface, List<Proposition>> updatedMap = new HashMap<>(mapToUpdate);
        final List<Proposition> list = updatedMap.get(surface) != null ? updatedMap.get(surface) : createMutableList(proposition);
        if (updatedMap.get(surface) != null) {
            list.add(proposition);
        }
        updatedMap.put(surface, list);
        return updatedMap;
    }

    // ========================================================================================
    // Proposition and Surface object creation wrappers
    // ========================================================================================

    /**
     * Wraps the internal {@link Proposition#fromEventData(Map)} method for use by the {@link com.adobe.marketing.mobile.Messaging} public API class
     *
     * @param propositionData A {@link Map<String, Object>} map containing {@link Proposition} data
     * @return the created {@code Proposition}
     */
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public static Proposition eventDataToProposition(final Map<String, Object> propositionData) {
        if (MapUtils.isNullOrEmpty(propositionData)) {
            return null;
        }
        return Proposition.fromEventData(propositionData);
    }

    /**
     * Wraps the internal {@link Surface#fromUriString(String)} method for use by the {@link com.adobe.marketing.mobile.Messaging} public API class
     *
     * @param scope A {@link String} containing a {@link Proposition} scope
     * @return the created {@link Surface}
     */
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public static Surface scopeToSurface(final String scope) {
        if (StringUtils.isNullOrEmpty(scope)) {
            return null;
        }
        return Surface.fromUriString(scope);
    }

    /**
     * Wraps the internal {@link Surface#toEventData()} method for use by the {@link com.adobe.marketing.mobile.Messaging} public API class
     *
     * @param surface A {@link Surface} containing a surface to be converted to an event data {@link Map}
     * @return the created event data {@code Map}
     */
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public static Map<String, Object> surfaceToEventData(final Surface surface) {
        if (surface == null) {
            return null;
        }
        return surface.toEventData();
    }
}