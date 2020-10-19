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
 * Class {@code PushNotificationTracking}
 * 
 *
 * XDM Property Java Object Generated 2020-06-17 16:58:12.488293 -0700 PDT m=+9.740346842 by XDMTool
 */
@SuppressWarnings("unused")
public class PushNotificationTracking implements Property {
	private CustomAction customAction;
	private String pushProviderMessageID;
	private String pushProvider;

	public PushNotificationTracking() {}

	@Override
	public Map<String, Object> serializeToXdm() {
		Map<String, Object> map = new HashMap<>();
		if (this.customAction != null) { map.put("customAction", this.customAction.serializeToXdm()); }
		if (this.pushProviderMessageID != null) { map.put("pushProviderMessageID", this.pushProviderMessageID); }
		if (this.pushProvider != null) { map.put("pushProvider", this.pushProvider); }

		return map;
	}

	/**
	 * Returns the Custom Action property
	 *
	 * @return {@link CustomAction} value or null if the property is not set
	 */
	public CustomAction getCustomAction() {
		return this.customAction;
	}

	/**
	 * Sets the Custom Action property
	 *
	 * @param newValue the new Custom Action value
	 */
	public void setCustomAction(final CustomAction newValue) {
		this.customAction = newValue;
	}
	/**
	 * Returns the Id property
	 *
	 * @return {@link String} value or null if the property is not set
	 */
	public String getPushProviderMessageID() {
		return this.pushProviderMessageID;
	}

	/**
	 * Sets the Id property
	 *
	 * @param newValue the new Id value
	 */
	public void setPushProviderMessageID(final String newValue) {
		this.pushProviderMessageID = newValue;
	}

	/**
	 * Returns the push provider property
	 *
	 * @return {@link String} value or null if the property is not set
	 */
	public String getPushProvider() {
		return this.pushProvider;
	}

	/**
	 * Sets the push provider property
	 *
	 * @param newValue the push provider value
	 */
	public void setPushProvider(final String newValue) {
		this.pushProvider = newValue;
	}
}
