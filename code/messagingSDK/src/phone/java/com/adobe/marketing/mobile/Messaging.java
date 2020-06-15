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

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;


public final class Messaging {

    private Messaging() {}

    private static final String EXTENSION_VERSION = "1.0.0";

    public static final String extensionVersion(){
        return EXTENSION_VERSION;
    }

    public static final void registerExtension(){
        if(MobileCore.getCore() == null || MobileCore.getCore().eventHub == null){
            Log.error(LOG_TAG, "Unable to register Messaging SDK since MobileCore is not initialized properly. For more details refer to https://aep-sdks.gitbook.io/docs/using-mobile-extensions/mobile-core");
        }

        try {
            MobileCore.getCore().eventHub.registerModule(com.adobe.marketing.mobile.MessagingModule.class);
        } catch (InvalidModuleException e) {
            Log.error(LOG_TAG, "Unable to register Messaging SDK.");
        }
    }
}
