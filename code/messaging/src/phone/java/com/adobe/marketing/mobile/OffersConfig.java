/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.adobe.marketing.mobile.messaging.BuildConfig;

/**
 * This class determines which configuration is in use for retrieving offers.
 */
final class OffersConfig {
    private final static String SELF_TAG = "OffersConfig";
    String activityId;
    String placementId;
    String applicationId;

    OffersConfig() {
        // for E2E functional test use the specified placement and activity id
        if (BuildConfig.IS_E2E_TEST.get()) {
            placementId = "xcore:offer-placement:142ae10d1d2fd883";
            activityId = "xcore:offer-activity:14b556c11d4c2433";
            return;
        }

        // for other functional tests use the specified placement and activity id
        if (BuildConfig.IS_FUNCTIONAL_TEST.get()) {
            placementId = "mock_placement";
            activityId = "mock_activity";
            return;
        }

        ApplicationInfo applicationInfo = null;
        try {
            final Application application = App.getApplication();
            applicationInfo = application.getPackageManager().getApplicationInfo(application.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException exception) {
            Log.warning(LOG_TAG, "%s - An exception occurred when retrieving the manifest metadata: %s", SELF_TAG, exception.getLocalizedMessage());
        }

        // use retrieved activity id and placement id if they are present in the manifest metadata
        if (applicationInfo.metaData != null) {
            final String retrievedActivityId = applicationInfo.metaData.getString(MessagingConstants.ManifestMetadataKeys.ACTIVITY_ID);
            final String retrievedPlacementId = applicationInfo.metaData.getString(MessagingConstants.ManifestMetadataKeys.PLACEMENT_ID);
            if (!StringUtils.isNullOrEmpty(retrievedActivityId) && !StringUtils.isNullOrEmpty(retrievedPlacementId)) {
                activityId = retrievedActivityId;
                placementId = retrievedPlacementId;
                return;
            }
        }

        // otherwise use the application identifier if manifest metadata is not present
        applicationId = App.getAppContext().getPackageName();

        // TODO: for manual testing, remove
        // activityId = "xcore:offer-activity:14090235e6b6757a";
        // 4byte char testing
        // placementId = "xcore:offer-placement:142be72cd583bd40";
        // sw demo
        // placementId = "xcore:offer-placement:142426be131dce37";
    }
}
