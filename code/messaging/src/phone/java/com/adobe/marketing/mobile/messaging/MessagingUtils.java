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

import com.adobe.marketing.mobile.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
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
    static boolean isNullOrEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Returns a mutable {@code List<T>} list containing a single element.
     *
     * @param element A {@link T} to be added to the mutable list
     * @return the mutable {@link List<T>} list
     */
    static <T> List<T> createMutableList(final T element) {
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
    static <T> List<T> createMutableList(final List<T> list) {
        return new ArrayList<>(list);
    }

    /**
     * Updates the provided {@code Map<Surface, List<Proposition>>} with the provided {@code Surface} and {@code List<Proposition>} objects.
     *
     * @param surface           A {@link Surface} key used to update a {@link List<  Proposition  >} value in the provided {@link Map<Surface, List<  Proposition  >>}
     * @param propositionsToAdd A {@link List<  Proposition  >} list to add in the provided {@code Map<Surface, List<Proposition>>}
     * @param mapToUpdate       The {@code Map<Surface, List<Proposition>>} to be updated with the provided {@code Surface} and {@code List<Proposition>} objects
     * @return the updated {@link Map<Surface, List<  Proposition  >>} map
     */
    public static Map<Surface, List<Proposition>> updatePropositionMapForSurface(final Surface surface, final List<Proposition> propositionsToAdd, final Map<Surface, List<Proposition>> mapToUpdate) {
        if (isNullOrEmpty(propositionsToAdd) || mapToUpdate == null) {
            return mapToUpdate;
        }
        final Map<Surface, List<Proposition>> updatedMap = new HashMap<>(mapToUpdate);
        final List<Proposition> existingList = updatedMap.get(surface);
        final List<Proposition> updatedList = existingList != null ? existingList : createMutableList(propositionsToAdd);
        if (existingList != null) {
            for (final Proposition proposition : propositionsToAdd) {
                if (!updatedList.contains(proposition)) {
                    updatedList.add(proposition);
                }
            }
        }
        updatedMap.put(surface, updatedList);
        return updatedMap;
    }

    /**
     * Updates the provided {@code Map<Surface, List<Proposition>>} map with the provided {@code Surface} and {@code Proposition} objects.
     *
     * @param surface     A {@link Surface} key used to update a {@link List<  Proposition  >} value in the provided {@link Map<Surface, List<  Proposition  >>}
     * @param proposition A {@link Proposition} object to add in the provided {@code Map<Surface, List<Proposition>>}
     * @param mapToUpdate The {@code Map<Surface, List<Proposition>>} to be updated with the provided {@code Surface} and {@code List<Proposition>} objects
     * @return the updated {@link Map<Surface, List<  Proposition  >>} map
     */
    public static Map<Surface, List<Proposition>> updatePropositionMapForSurface(final Surface surface, final Proposition proposition, final Map<Surface, List<Proposition>> mapToUpdate) {
        if (proposition == null || mapToUpdate == null) {
            return mapToUpdate;
        }
        final Map<Surface, List<Proposition>> updatedMap = new HashMap<>(mapToUpdate);
        final List<Proposition> existingList = updatedMap.get(surface);
        final List<Proposition> updatedList = existingList != null ? existingList : createMutableList(proposition);
        if (existingList != null) {
            updatedList.add(proposition);
        }
        updatedMap.put(surface, updatedList);
        return updatedMap;
    }

    // ========================================================================================
    // Surface object creation wrapper
    // ========================================================================================

    /**
     * Wraps the internal {@link Surface#fromUriString(String)} method for use by the {@link com.adobe.marketing.mobile.Messaging} public API class
     *
     * @param scope A {@link String} containing a {@link Proposition} scope
     * @return the created {@link Surface}
     */
    public static Surface scopeToSurface(final String scope) {
        if (StringUtils.isNullOrEmpty(scope)) {
            return null;
        }
        return Surface.fromUriString(scope);
    }
}