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

public interface AdobeAlert {

    /**
     * Gets the {@code AdobeAlert} title text.
     *
     * @return {@link String} containing the {@link AdobeAlert} title text.
     */
    String getTitle();

    /**
     * Gets the {@code AdobeAlert} message body text.
     *
     * @return {@link String} containing the {@link AdobeAlert} message body text.
     */
    String getMessage();

    /**
     * Gets the {@code AdobeAlert} default button text.
     *
     * @return {@link String} containing the {@link AdobeAlert} default button text.
     */
    String getDefaultButton();

    /**
     * Gets the {@code AdobeAlert} default button url.
     *
     * @return {@link String} containing the {@link AdobeAlert} default button url.
     */
    String getDefaultButtonUrl();

    /**
     * Gets the {@code AdobeAlert} cancel button text.
     *
     * @return {@link String} containing the {@link AdobeAlert} cancel button text.
     */
    String getCancelButton();

    /**
     * Gets the {@code AdobeAlert} cancel button url.
     *
     * @return {@link String} containing the {@link AdobeAlert} cancel button url.
     */
    String getCancelButtonUrl();

    /**
     * Gets the {@code AdobeAlert} style.
     *
     * @return {@link String} containing the {@link AdobeAlert} style.
     */
    String getStyle();
}
