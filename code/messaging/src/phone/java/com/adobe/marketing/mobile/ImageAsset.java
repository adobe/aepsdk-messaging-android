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

class ImageAsset {
    private final String assetUrl;
    private final int beginIndex;
    private final int endIndex;

    ImageAsset(final String assetUrl, final int beginIndex, final int endIndex) {
        this.assetUrl = assetUrl;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    String getAssetUrl() {
        return assetUrl;
    }

    int getBeginIndex() {
        return beginIndex;
    }

    int getEndIndex() {
        return endIndex;
    }

    String getAssetExtension() {
        final int i = assetUrl.lastIndexOf('.');
        if (i < 0) {
            Log.trace(MessagingConstants.LOG_TAG,
                    "Unable to find a file extension for: %s", assetUrl);
            return null;
        }
        return assetUrl.substring(i);
    }
}
