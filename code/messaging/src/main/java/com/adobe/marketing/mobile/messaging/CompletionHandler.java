/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging;

import com.adobe.marketing.mobile.AdobeCallback;

/** Class used to manage callbacks between disconnected but related events. */
public class CompletionHandler {
    final String originatingEventId;
    String edgeRequestEventId;
    final AdobeCallback<Boolean> handle;

    public CompletionHandler(final String originatingEventId, final AdobeCallback<Boolean> handle) {
        this.originatingEventId = originatingEventId;
        this.handle = handle;
    }
}
