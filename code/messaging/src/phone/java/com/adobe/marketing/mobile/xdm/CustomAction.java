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
 * Class {@code CustomAction}
 * 
 *
 * XDM Property Java Object Generated 2020-06-17 16:58:12.48855 -0700 PDT m=+9.740603686 by XDMTool
 */
@SuppressWarnings("unused")
public class CustomAction implements Property {
	private String actionId;
	private double value;

	public CustomAction() {}

	@Override
	public Map<String, Object> serializeToXdm() {
		Map<String, Object> map = new HashMap<>();
		if (this.actionId != null) { map.put("actionID", this.actionId); }
		map.put("value", this.value);

		return map;
	}
	
	/**
	 * Returns the Action Id property
	 * 
	 * @return {@link String} value or null if the property is not set
	 */
	public String getActionId() {
		return this.actionId;
	}

	/**
	 * Sets the Action Id property
	 * 
	 * @param newValue the new Action Id value
	 */
	public void setActionId(final String newValue) {
		this.actionId = newValue;
	} 
	/**
	 * Returns the Value property
	 * 
	 * @return double value
	 */
	public double getValue() {
		return this.value;
	}

	/**
	 * Sets the Value property
	 * 
	 * @param newValue the new Value value
	 */
	public void setValue(final double newValue) {
		this.value = newValue;
	} 
}
