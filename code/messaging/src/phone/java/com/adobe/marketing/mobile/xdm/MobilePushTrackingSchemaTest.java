/*
 Copyright 2020 Adobe. All rights reserved.

 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
*/
package com.adobe.marketing.mobile.xdm;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Class {@code MobilePushTrackingSchemaTest}
 * 
 * <p/>
 * XDM Schema Java Object Generated 2020-06-17 16:58:12.487261 -0700 PDT m=+9.739315253 by XDMTool
 *
 * Title		:	Mobile Push Tracking Schema Test
 * Version		:	1.1
 * ID			:	https://ns.adobe.com/acopprod1/schemas/29ff4646f70ceb167d6dce058a89e04689cd4ed4762de9a9
 * Alt ID		:	_acopprod1.schemas.29ff4646f70ceb167d6dce058a89e04689cd4ed4762de9a9
 * Type			:	schemas
 * IMS Org		:	3E2A28175B8ED3720A495E23@AdobeOrg
 */
@SuppressWarnings("unused")
public class MobilePushTrackingSchemaTest implements Schema {
	private Acopprod1 Acopprod1;
	private String eventMergeId;
	private String eventType;
	private IdentityMap identityMap;
	private java.util.Date timestamp;

	public MobilePushTrackingSchemaTest() {}

	/**
	 * Returns the version number of this schema.
	 *
	 * @return the schema version number
	 */
	@Override
	public String getSchemaVersion() {
		return "1.1";
	}

	/**
	 * Returns the unique schema identifier.
	 *
	 * @return the schema ID
	 */
	@Override
	public String getSchemaIdentifier() {
		return "https://ns.adobe.com/acopprod1/schemas/29ff4646f70ceb167d6dce058a89e04689cd4ed4762de9a9";
	}

	/**
	 * Returns the unique dataset identifier.
	 *
	 * @return the dataset ID
	 */
	 //@Override
	 public String getDatasetIdentifier() {
		 return "5eeaab530b5abf191521377b";
	 }

	@Override
	public Map<String, Object> serializeToXdm() {
		Map<String, Object> map = new HashMap<>();
		if (this.Acopprod1 != null) { map.put("_acopprod1", this.Acopprod1.serializeToXdm()); }
		if (this.eventMergeId != null) { map.put("eventMergeId", this.eventMergeId); }
		if (this.eventType != null) { map.put("eventType", this.eventType); }
		if (this.identityMap != null) { map.put("identityMap", this.identityMap.serializeToXdm()); }
		if (this.timestamp != null) { map.put("timestamp", Serializer.serializeToISO8601String(this.timestamp)); }

		return map;
	}

	
	/**
	 * Returns the Acopprod1 property
	 * 
	 * @return {@link Acopprod1} value or null if the property is not set
	 */
	public Acopprod1 getAcopprod1() {
		return this.Acopprod1;
	}

	/**
	 * Sets the Acopprod1 property
	 * 
	 * @param newValue the new Acopprod1 value
	 */
	public void setAcopprod1(final Acopprod1 newValue) {
		this.Acopprod1 = newValue;
	} 
	/**
	 * Returns the ExperienceEvent merge ID property
	 * An ID to correlate or merge multiple Experience events together that are essentially the same event or should be merged. This is intended to be populated by the data producer prior to ingestion.
	 * @return {@link String} value or null if the property is not set
	 */
	public String getEventMergeId() {
		return this.eventMergeId;
	}

	/**
	 * Sets the ExperienceEvent merge ID property
	 * An ID to correlate or merge multiple Experience events together that are essentially the same event or should be merged. This is intended to be populated by the data producer prior to ingestion.
	 * @param newValue the new ExperienceEvent merge ID value
	 */
	public void setEventMergeId(final String newValue) {
		this.eventMergeId = newValue;
	} 
	/**
	 * Returns the Event Type property
	 * The primary event type for this time-series record.
	 * @return {@link String} value or null if the property is not set
	 */
	public String getEventType() {
		return this.eventType;
	}

	/**
	 * Sets the Event Type property
	 * The primary event type for this time-series record.
	 * @param newValue the new Event Type value
	 */
	public void setEventType(final String newValue) {
		this.eventType = newValue;
	} 
	/**
	 * Returns the IdentityMap property
	 * 
	 * @return {@link IdentityMap} value or null if the property is not set
	 */
	public IdentityMap getIdentityMap() {
		return this.identityMap;
	}

	/**
	 * Sets the IdentityMap property
	 * 
	 * @param newValue the new IdentityMap value
	 */
	public void setIdentityMap(final IdentityMap newValue) {
		this.identityMap = newValue;
	} 
	/**
	 * Returns the Timestamp property
	 * The time when an event or observation occurred.
	 * @return {@link java.util.Date} value or null if the property is not set
	 */
	public java.util.Date getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Sets the Timestamp property
	 * The time when an event or observation occurred.
	 * @param newValue the new Timestamp value
	 */
	public void setTimestamp(final java.util.Date newValue) {
		this.timestamp = newValue;
	} 
}

