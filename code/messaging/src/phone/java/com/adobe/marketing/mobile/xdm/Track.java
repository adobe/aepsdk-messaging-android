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
 * Class {@code Track}
 * 
 *
 * XDM Property Java Object Generated 2020-06-17 16:58:12.488375 -0700 PDT m=+9.740429281 by XDMTool
 */
@SuppressWarnings("unused")
public class Track implements Property {
	private boolean applicationOpened;
	private CustomAction customAction;
	private String id;

	public Track() {}

	@Override
	public Map<String, Object> serializeToXdm() {
		Map<String, Object> map = new HashMap<>();
		map.put("applicationOpened", this.applicationOpened);
		if (this.customAction != null) { map.put("customAction", this.customAction.serializeToXdm()); }
		if (this.id != null) { map.put("id", this.id); }

		return map;
	}
	
	/**
	 * Returns the Application Opened property
	 * 
	 * @return boolean value
	 */
	public boolean getApplicationOpened() {
		return this.applicationOpened;
	}

	/**
	 * Sets the Application Opened property
	 * 
	 * @param newValue the new Application Opened value
	 */
	public void setApplicationOpened(final boolean newValue) {
		this.applicationOpened = newValue;
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
	public String getId() {
		return this.id;
	}

	/**
	 * Sets the Id property
	 * 
	 * @param newValue the new Id value
	 */
	public void setId(final String newValue) {
		this.id = newValue;
	} 
}
