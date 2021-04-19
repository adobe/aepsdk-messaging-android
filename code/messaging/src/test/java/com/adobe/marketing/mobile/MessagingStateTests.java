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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
public class MessagingStateTests {
    private MessagingState messagingState;

    // mocks
    private final String MOCK_EXP_EVENT_DATASET = "mock_exp_event_dataset";
    private final String MOCK_VID = "mock_vid";

    @Before
    public void before() {
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
        Assert.assertNull(messagingState.getEcid());
        Assert.assertNull(messagingState.getExperienceEventDatasetId());
    }

    @Test
    public void test_setState_when_paramsArePresent() {
        //mocks
        EventData mockConfigEventData = getMockConfigEventData();
        EventData mockIdentityEventData = getMockEdgeIdentityEventData();
        
        // test
        messagingState.setState(mockConfigEventData.toObjectMap(), mockIdentityEventData.toObjectMap());

        Assert.assertEquals(messagingState.getEcid(), MOCK_VID);
        Assert.assertEquals(messagingState.getExperienceEventDatasetId(), MOCK_EXP_EVENT_DATASET);
    }

    private EventData getMockConfigEventData() {
        EventData configEventData = new EventData();
        configEventData.putString(MessagingConstant.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID, MOCK_EXP_EVENT_DATASET);
        return configEventData;
    }

    private EventData getMockEdgeIdentityEventData() {
        EventData identityEventData = new EventData();
        Map<String, Variant> identityMap = new HashMap<>();
        List<Variant> ecids = new ArrayList<>();
        ecids.add(Variant.fromStringMap(Collections.singletonMap(MessagingConstant.SharedState.EdgeIdentity.ID, MOCK_VID)));
        identityMap.put(MessagingConstant.SharedState.EdgeIdentity.ECID, Variant.fromVariantList(ecids));
        identityEventData.putVariantMap(MessagingConstant.SharedState.EdgeIdentity.IDENTITY_MAP, identityMap);
        return identityEventData;
    }
}
