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
package com.adobe.marketing.mobile.messaging;

import static org.hamcrest.core.Is.is;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingUtilsTests {

    private Surface mockSurface;
    private Map<Surface, List<MessagingProposition>> mockMapToUpdate = new HashMap<>();
    private List<MessagingProposition> mockPropositionsToAdd = new ArrayList<>();
    private MessagingProposition mockPropositionToAdd1;
    private MessagingProposition mockPropositionToAdd2;
    private final Map<String, Object> mockScopeDetails = new HashMap<String, Object>() {{
        put("key", "value");
    }};

    @Before
    public void setup() {
        mockSurface = Surface.fromUriString("mobileapp://mockApp/feeds/testFeed");
        final Map<String, Object> propositionItemMap = MessagingTestUtils.getMapFromFile("propositionItemFeed.json");
        final MessagingPropositionItem mockPropositionItem = MessagingPropositionItem.fromEventData(propositionItemMap);
        MessagingProposition mockProposition = new MessagingProposition("mockId",
                mockSurface.getUri(),
                mockScopeDetails,
                new ArrayList<MessagingPropositionItem>() {{
                    add(mockPropositionItem);
                }});
        mockMapToUpdate.put(mockSurface, new ArrayList<MessagingProposition>() {{
            add(mockProposition);
        }});

        final Map<String, Object> content = MessagingTestUtils.getMapFromFile("feedPropositionContent.json");
        MessagingPropositionItem mockPropositionItemToAdd1 = new MessagingPropositionItem("mockId1", SchemaType.DEFAULT, content);
        mockPropositionToAdd1 = new MessagingProposition("mockId1",
                mockSurface.getUri(),
                mockScopeDetails,
                new ArrayList<MessagingPropositionItem>() {{
                    add(mockPropositionItemToAdd1);
                }});
        Map<String, Object> propositionItemMap2 = MessagingTestUtils.getMapFromFile("propositionItemFeed2.json");
        MessagingPropositionItem messagingPropositionItemToAdd2 = MessagingPropositionItem.fromEventData(propositionItemMap2);
        mockPropositionToAdd2 = new MessagingProposition("mockId2",
                mockSurface.getUri(),
                mockScopeDetails,
                new ArrayList<MessagingPropositionItem>() {{
                    add(messagingPropositionItemToAdd2);
                }});
        mockPropositionsToAdd.add(mockPropositionToAdd1);
        mockPropositionsToAdd.add(mockPropositionToAdd2);
    }

    @Test
    public void testIsNullOrEmpty_nullList() {
        // test
        Assert.assertTrue(MessagingUtils.isNullOrEmpty(null));
    }

    @Test
    public void testIsNullOrEmpty_emptyList() {
        // test
        Assert.assertTrue(MessagingUtils.isNullOrEmpty(new ArrayList<>()));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyList() {
        // test
        final List<Object> list = new ArrayList<>();
        list.add("someString");

        Assert.assertFalse(MessagingUtils.isNullOrEmpty(list));
    }

    @Test
    public void test_createMutableList_WothElementArgument() {
        // setup
        List<String> mockList = new ArrayList<>();
        mockList.add("element1");

        // test
        List<String> newList = MessagingUtils.createMutableList(mockList.get(0));

        // verify
        Assert.assertNotSame(mockList, newList);
    }

    @Test
    public void test_createMutableList_WithListArgument() {
        // setup
        List<String> mockList = new ArrayList<>();
        mockList.add("element1");
        mockList.add("element2");

        // test
        List<String> newList = MessagingUtils.createMutableList(mockList);

        // verify
        Assert.assertNotSame(mockList, newList);
    }

    @Test
    public void test_updatePropositionMapForSurface_PropositionsListToAddIsNull() {
        // test
        Assert.assertEquals(mockMapToUpdate,
                MessagingUtils.updatePropositionMapForSurface(mockSurface, (List<MessagingProposition>) null, mockMapToUpdate));
    }

    @Test
    public void test_updatePropositionMapForSurface_MapToUpdateIsNull() {
        // test
        Assert.assertNull(MessagingUtils.updatePropositionMapForSurface(mockSurface, mockPropositionsToAdd, null));
    }

    @Test
    public void test_updatePropositionMapForSurface_ValidPropositionList() {
        // test
        Map<Surface, List<MessagingProposition>> updatedMap = MessagingUtils.updatePropositionMapForSurface(
                mockSurface,
                mockPropositionsToAdd,
                mockMapToUpdate);

        // verify
        Assert.assertEquals(1, updatedMap.size());
        Assert.assertEquals(3, updatedMap.get(mockSurface).size());
    }

    @Test
    public void test_updatePropositionMapForSurface_SurfaceDoesNotExist_WithValidPropositionList() {
        // setup
        Surface mockSurface2 = Surface.fromUriString("mobileapp://mockApp/feeds/testFeed2");

        // test
        Map<Surface, List<MessagingProposition>> updatedMap = MessagingUtils.updatePropositionMapForSurface(
                mockSurface2,
                mockPropositionsToAdd,
                mockMapToUpdate);

        // verify
        Assert.assertEquals(2, updatedMap.size());
        Assert.assertEquals(1, updatedMap.get(mockSurface).size());
        Assert.assertEquals(2, updatedMap.get(mockSurface2).size());
    }

    @Test
    public void test_updatePropositionMapForSurface_PropositionToAddIsNull() {
        // test
        Assert.assertEquals(mockMapToUpdate,
                MessagingUtils.updatePropositionMapForSurface(mockSurface, (MessagingProposition) null, mockMapToUpdate));
    }

    @Test
    public void test_updatePropositionMapForSurface_MapToUpdateIsNullWithValidProposition() {
        // test
        Assert.assertNull(MessagingUtils.updatePropositionMapForSurface(mockSurface, mockPropositionToAdd1, null));
    }

    @Test
    public void test_updatePropositionMapForSurface_ValidProposition() {
        // test
        Map<Surface, List<MessagingProposition>> updatedMap = MessagingUtils.updatePropositionMapForSurface(
                mockSurface,
                mockPropositionToAdd1,
                mockMapToUpdate);

        // verify
        Assert.assertEquals(1, updatedMap.size());
        Assert.assertEquals(2, updatedMap.get(mockSurface).size());
    }

    @Test
    public void test_updatePropositionMapForSurface_SurfaceDoesNotExist_WithValidProposition() {
        // setup
        Surface mockSurface2 = Surface.fromUriString("mobileapp://mockApp/feeds/testFeed2");

        // test
        Map<Surface, List<MessagingProposition>> updatedMap = MessagingUtils.updatePropositionMapForSurface(
                mockSurface2,
                mockPropositionToAdd1,
                mockMapToUpdate);

        // verify
        Assert.assertEquals(2, updatedMap.size());
        Assert.assertEquals(1, updatedMap.get(mockSurface).size());
        Assert.assertEquals(1, updatedMap.get(mockSurface2).size());
    }

    @Test
    public void test_scopeToSurface_scopeIsNull() {
        // test
        Assert.assertNull(MessagingUtils.scopeToSurface(null));
    }

    @Test
    public void test_scopeToSurface_scopeIsEmpty() {
        // test
        Assert.assertNull(MessagingUtils.scopeToSurface(""));
    }

    @Test
    public void test_scopeToSurface_validScope() {
        // test
        Surface surface = MessagingUtils.scopeToSurface("mobileapp://mockApp/feeds/testFeed");

        // verify
        Assert.assertNotNull(surface);
        Assert.assertEquals("mobileapp://mockApp/feeds/testFeed", surface.getUri());
    }

    @Test
    public void test_getMessageForPresentable_presentableIsNull() {
        // test
        Assert.assertNull(MessagingUtils.getMessageForPresentable(null));
    }
}
