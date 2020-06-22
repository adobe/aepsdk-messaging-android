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
 * Class {@code Acopprod1}
 * 
 *
 * XDM Property Java Object Generated 2020-06-17 16:58:12.488293 -0700 PDT m=+9.740346842 by XDMTool
 */
@SuppressWarnings("unused")
public class Acopprod1 implements Property {
	private Track track;

	public Acopprod1() {}

	@Override
	public Map<String, Object> serializeToXdm() {
		Map<String, Object> map = new HashMap<>();
		if (this.track != null) { map.put("track", this.track.serializeToXdm()); }

		return map;
	}
	
	/**
	 * Returns the Track property
	 * 
	 * @return {@link Track} value or null if the property is not set
	 */
	public Track getTrack() {
		return this.track;
	}

	/**
	 * Sets the Track property
	 * 
	 * @param newValue the new Track value
	 */
	public void setTrack(final Track newValue) {
		this.track = newValue;
	} 
}
