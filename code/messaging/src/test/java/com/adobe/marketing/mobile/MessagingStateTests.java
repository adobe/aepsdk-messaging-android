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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.utils.Asserts;

import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobilePrivacyStatus.class})
public class MessagingStateTests {
    private MessagingState messagingState;

    // mocks
    private final String MOCK_DCCS_URL = "mock_dccs_url";
    private final String MOCK_PRIVACY_STATUS = "optedin";
    private final String MOCK_PROFILE_DATASET = "mock_profile_dataset";
    private final String MOCK_EXP_EVENT_DATASET = "mock_exp_event_dataset";
    private final String MOCK_EXP_ORG = "mock_exp_org";
    private final String MOCK_VID = "mock_vid";

    @Before
    public void before() {
        PowerMockito.mockStatic(MobilePrivacyStatus.class);
        messagingState = new MessagingState();
    }

    // ========================================================================================
    // setState
    // ========================================================================================
    @Test
    public void test_setState_when_null_params() {
        // test
        messagingState.setState(null, null);

        // verify
        PowerMockito.verifyStatic(MobilePrivacyStatus.class, Mockito.times(0));
        MobilePrivacyStatus.fromString(ArgumentMatchers.anyString());
    }

    @Test
    public void test_setState_when_paramsArePresent() {
        //mocks
        EventData mockConfigEventData = getMockConfigEventData();
        EventData mockIdentityEventData = getMockIdentityEventData();

        // when
        Mockito.when(MobilePrivacyStatus.fromString(ArgumentMatchers.anyString())).thenReturn(MobilePrivacyStatus.OPT_IN);

        // test
        messagingState.setState(mockConfigEventData, mockIdentityEventData);

        // verify
        PowerMockito.verifyStatic(MobilePrivacyStatus.class, Mockito.times(1));
        MobilePrivacyStatus.fromString(ArgumentMatchers.anyString());

        Assert.assertEquals(messagingState.getDccsURL(), MOCK_DCCS_URL);
        Assert.assertEquals(messagingState.getEcid(), MOCK_VID);
        Assert.assertEquals(messagingState.getExperienceCloudOrg(), MOCK_EXP_ORG);
        Assert.assertEquals(messagingState.getExperienceEventDatasetId(), MOCK_EXP_EVENT_DATASET);
        Assert.assertEquals(messagingState.getProfileDatasetId(), MOCK_PROFILE_DATASET);

        Assert.assertEquals(messagingState.getPrivacyStatus(), MobilePrivacyStatus.OPT_IN);
    }

    private EventData getMockConfigEventData() {
        EventData configEventData = new EventData();
        configEventData.putString(MessagingConstant.EventDataKeys.Configuration.GLOBAL_PRIVACY_STATUS, MOCK_PRIVACY_STATUS);
        configEventData.putString(MessagingConstant.EventDataKeys.Configuration.PROFILE_DATASET_ID, MOCK_PROFILE_DATASET);
        configEventData.putString(MessagingConstant.EventDataKeys.Configuration.EXPERIENCE_EVENT_DATASET_ID, MOCK_EXP_EVENT_DATASET);
        configEventData.putString(MessagingConstant.EventDataKeys.Configuration.DCCS_URL, MOCK_DCCS_URL);
        configEventData.putString(MessagingConstant.EventDataKeys.Configuration.EXPERIENCE_CLOUD_ORG, MOCK_EXP_ORG);
        return configEventData;
    }

    private EventData getMockIdentityEventData() {
        EventData identityEventData = new EventData();
        identityEventData.putString(MessagingConstant.EventDataKeys.Identity.VISITOR_ID_MID, MOCK_VID);
        return identityEventData;
    }
}
