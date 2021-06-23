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

import java.util.HashMap;
import java.util.Map;

/**
 * {@code MessagingConsequenceSerializer} can be used to serialize a {@code MessagingConsequence} instance to a {@code Variant}
 * and to deserialize a {@code Variant} to a {@code MessagingConsequence} instance.
 */
final class MessagingConsequenceSerializer implements VariantSerializer<MessagingConsequence> {

    /**
     * Serializes the given {@code MessagingConsequence} instance to a {@code Variant}.
     *
     * @param consequence {@link MessagingConsequence} instance to serialize
     * @return {@link Variant} representing {@code MessagingConsequence}, or the null variant if {@code consequence} is null
     */
    @Override
    public Variant serialize(final MessagingConsequence consequence) {
        if (consequence == null) {
            Log.debug(MessagingConstant.LOG_TAG, "serialize - MessagingConsequence is null, so returning null Variant.");
            return Variant.fromNull();
        }

        Map<String, Variant> map = new HashMap<>();

        String id = consequence.getId();
        map.put(MessagingConstant.EventDataKeys.MessagingRuleEngine.MESSAGE_CONSEQUENCE_ID, (id == null) ? Variant.fromNull() :
                Variant.fromString(consequence.getId()));

        String type = consequence.getType();
        map.put(MessagingConstant.EventDataKeys.MessagingRuleEngine.MESSAGE_CONSEQUENCE_TYPE, (type == null) ? Variant.fromNull() :
                Variant.fromString(consequence.getType()));

        Map<String, Variant> detailMap = consequence.getDetails();
        map.put(MessagingConstant.EventDataKeys.MessagingRuleEngine.MESSAGE_CONSEQUENCE_DETAIL,
                (detailMap == null) ? Variant.fromNull() :
                        Variant.fromVariantMap(consequence.getDetails()));

        return Variant.fromVariantMap(map);
    }

    /**
     * Deserializes the given {@code Variant} to a {@code MessagingConsequence} instance.
     *
     * @param variant {@link Variant} to deserialize
     * @return a {@link MessagingConsequence} instance that was deserialized from the variant. Can be null.
     *
     * @throws IllegalArgumentException if variant is null
     * @throws VariantSerializationFailedException if variant serialization failed
     */
    @Override
    public MessagingConsequence deserialize(final Variant variant) throws VariantSerializationFailedException {
        if (variant == null) {
            throw new IllegalArgumentException("Variant for deserialization is null.");
        }

        if (variant.getKind() == VariantKind.NULL) {
            Log.trace(MessagingConstant.LOG_TAG,
                    "deserialize -  Variant kind is null, null Consequence is returned.");
            return null;
        }

        final Map<String, Variant> consequenceMap = variant.optVariantMap(null);

        if (consequenceMap == null || consequenceMap.isEmpty()) {
            throw new VariantSerializationFailedException("deserialize -  Consequence Map is null or empty.");
        }

        // id - required field
        final String id = Variant.optVariantFromMap(consequenceMap,
                MessagingConstant.EventDataKeys.MessagingRuleEngine.MESSAGE_CONSEQUENCE_ID).optString(null);

        if (StringUtils.isNullOrEmpty(id)) {
            Log.debug(MessagingConstant.LOG_TAG,
                    "deserialize -  Unable to find field \"id\" in Messaging rules consequence. This a required field.");
            throw new VariantSerializationFailedException("Consequence \"id\" is null or empty.");
        }

        // type - required field
        final String type = Variant.optVariantFromMap(consequenceMap,
                MessagingConstant.EventDataKeys.MessagingRuleEngine.MESSAGE_CONSEQUENCE_TYPE).optString(null);

        if (StringUtils.isNullOrEmpty(type)) {
            Log.warning(MessagingConstant.LOG_TAG,
                    "No valid field \"type\" in Messaging rules consequence. This is a required field.");
            throw new VariantSerializationFailedException("Consequence \"type\" is null or empty.");
        }

        // detail - required field
        final Map<String, Variant> detail = Variant.optVariantFromMap(consequenceMap,
                MessagingConstant.EventDataKeys.MessagingRuleEngine.MESSAGE_CONSEQUENCE_DETAIL).optVariantMap(null);

        if (detail == null || detail.isEmpty()) {
            Log.warning(MessagingConstant.LOG_TAG,
                    "No valid field \"detail\" in Messaging rules consequence. This a required field.");
            throw new VariantSerializationFailedException("Consequence \"detail\" is null or empty.");
        }

        return new MessagingConsequence(id, type, detail);
    }
}