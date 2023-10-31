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

import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;

import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ParsedPropositionsTests {

    private Surface mockSurface;

    private MessagingPropositionItem mockInAppPropositionItem;
    private MessagingProposition mockInAppProposition;
    private Surface mockInAppSurface;
    private final String mockInAppMessageId = "6ac78390-84e3-4d35-b798-8e7080e69a66";

    private MessagingPropositionItem mockInAppPropositionItemV2;
    private MessagingProposition mockInAppPropositionV2;
    private Surface mockInAppSurfaceV2;
    private final String mockInAppMessageIdV2 = "6ac78390-84e3-4d35-b798-8e7080e69a67";

    private MessagingPropositionItem mockFeedPropositionItem;
    private MessagingProposition mockFeedProposition;
    private Surface mockFeedSurface;
    private final String mockFeedMessageId = "183639c4-cb37-458e-a8ef-4e130d767ebf";
    private String mockFeedContent;

    private MessagingPropositionItem mockCodeBasedPropositionItem;
    private MessagingProposition mockCodeBasedProposition;
    private Surface mockCodeBasedSurface;
    private String mockCodeBasedContent;

    private final Map<String, Object> mockScopeDetails = new HashMap<String, Object>() {{
        put("key", "value");
    }};

    @Mock
    private ExtensionApi mockExtensionApi;

    @Before
    public void setup() throws JSONException {

        mockSurface = Surface.fromUriString("mobileapp://some.not.matching.surface/path");

        mockInAppSurface = Surface.fromUriString("mobileapp://mockPackageName/inapp");
        final String inappPropositionV1Content = MessagingTestUtils.loadStringFromFile("inappPropositionV1Content.json");
        mockInAppPropositionItem = new MessagingPropositionItem("inapp", "inapp", inappPropositionV1Content);
        mockInAppProposition = new MessagingProposition("inapp",
                mockInAppSurface.getUri(),
                mockScopeDetails,
                new ArrayList<MessagingPropositionItem>() {{
                    add(mockInAppPropositionItem);
                }});

        mockInAppSurfaceV2 = Surface.fromUriString("mobileapp://mockPackageName/inapp2");
        final String inappPropositionV2Content = MessagingTestUtils.loadStringFromFile("inappPropositionV2Content.json");
        mockInAppPropositionItemV2 = new MessagingPropositionItem("inapp2", "inapp2", inappPropositionV2Content);
        mockInAppPropositionV2 = new MessagingProposition("inapp2",
                mockInAppSurfaceV2.getUri(),
                mockScopeDetails,
                new ArrayList<MessagingPropositionItem>() {{
                    add(mockInAppPropositionItemV2);
                }});

        mockFeedSurface = Surface.fromUriString("mobileapp://mockPackageName/feed");
        mockFeedContent = MessagingTestUtils.loadStringFromFile("feedPropositionContent.json");
        mockFeedPropositionItem = new MessagingPropositionItem("feed", "feed", mockFeedContent);
        mockFeedProposition = new MessagingProposition("feed",
                mockFeedSurface.getUri(),
                mockScopeDetails,
                new ArrayList<MessagingPropositionItem>() {{
                    add(mockFeedPropositionItem);
                }});

        mockCodeBasedSurface = Surface.fromUriString("mobileapp://mockPackageName/codebased");
        mockCodeBasedContent = MessagingTestUtils.loadStringFromFile("codeBasedPropositionContent.json");
        mockCodeBasedPropositionItem = new MessagingPropositionItem("codebased", "codebased", mockCodeBasedContent);
        mockCodeBasedProposition = new MessagingProposition("codebased",
                mockCodeBasedSurface.getUri(),
                mockScopeDetails,
                new ArrayList<MessagingPropositionItem>() {{
                    add(mockCodeBasedPropositionItem);
                }});
    }

    @Test
    public void test_parsedPropositionConstructor_WithEmptyPropositions() {
        // setup
        Map<Surface, List<MessagingProposition>> propositions = new HashMap<Surface, List<MessagingProposition>>() {{
            put(mockSurface, new ArrayList<>());
        }};

        // test
        ParsedPropositions parsedPropositions = new ParsedPropositions(
                propositions,
                new ArrayList<Surface>() {{
                    add(mockSurface);
                }},
                mockExtensionApi);

        // verify
        Assert.assertNotNull(parsedPropositions);
        Assert.assertEquals(0, parsedPropositions.propositionInfoToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToPersist.size());
        Assert.assertEquals(0, parsedPropositions.surfaceRulesByInboundType.size());
    }

    @Test
    public void test_parsedPropositionConstructor_WithPropositionScopeNotMatchingRequestedSurfaces() {
        // setup
        Map<Surface, List<MessagingProposition>> propositions = new HashMap<Surface, List<MessagingProposition>>() {{
            put(mockInAppSurface, new ArrayList<MessagingProposition>() {{
                add(mockInAppProposition);
            }});
            put(mockFeedSurface, new ArrayList<MessagingProposition>() {{
                add(mockFeedProposition);
            }});
            put(mockCodeBasedSurface, new ArrayList<MessagingProposition>() {{
                add(mockCodeBasedProposition);
            }});
        }};

        // test
        ParsedPropositions parsedPropositions = new ParsedPropositions(
                propositions,
                new ArrayList<Surface>() {{
                    add(mockSurface);
                }},
                mockExtensionApi);

        // verify
        Assert.assertNotNull(parsedPropositions);
        Assert.assertEquals(0, parsedPropositions.propositionInfoToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToPersist.size());
        Assert.assertEquals(0, parsedPropositions.surfaceRulesByInboundType.size());
    }

    @Test
    public void test_parsedPropositionConstructor_WithInAppPropositionV1() {
        // setup
        Map<Surface, List<MessagingProposition>> propositions = new HashMap<Surface, List<MessagingProposition>>() {{
            put(mockInAppSurface, new ArrayList<MessagingProposition>() {{
                add(mockInAppProposition);
            }});
        }};

        // test
        ParsedPropositions parsedPropositions = new ParsedPropositions(
                propositions,
                new ArrayList<Surface>() {{
                    add(mockInAppSurface);
                }},
                mockExtensionApi);


        //verify
        Assert.assertNotNull(parsedPropositions);
        Assert.assertEquals(1, parsedPropositions.propositionInfoToCache.size());
        PropositionInfo iamPropositionInfo = parsedPropositions.propositionInfoToCache.get(mockInAppMessageId);
        Assert.assertNotNull(iamPropositionInfo);
        Assert.assertEquals("inapp", iamPropositionInfo.id);
        Assert.assertEquals(0, parsedPropositions.propositionsToCache.size());
        Assert.assertEquals(1, parsedPropositions.propositionsToPersist.size());
        List<MessagingProposition> iamPersist = parsedPropositions.propositionsToPersist.get(mockInAppSurface);
        Assert.assertNotNull(iamPersist);
        Assert.assertEquals(1, iamPersist.size());
        Assert.assertEquals("inapp", iamPersist.get(0).getUniqueId());
        Assert.assertEquals(1, parsedPropositions.surfaceRulesByInboundType.size());
        Map<Surface, List<LaunchRule>> iamRules = parsedPropositions.surfaceRulesByInboundType.get(InboundType.INAPP);
        Assert.assertNotNull(iamRules);
        Assert.assertEquals(1, iamRules.size());
    }

    @Test
    public void test_parsedPropositionConstructor_WithInAppPropositionV2() {
        // setup
        Map<Surface, List<MessagingProposition>> propositions = new HashMap<Surface, List<MessagingProposition>>() {{
            put(mockInAppSurfaceV2, new ArrayList<MessagingProposition>() {{
                add(mockInAppPropositionV2);
            }});
        }};

        // test
        ParsedPropositions parsedPropositions = new ParsedPropositions(
                propositions,
                new ArrayList<Surface>() {{
                    add(mockInAppSurfaceV2);
                }},
                mockExtensionApi);


        //verify
        Assert.assertNotNull(parsedPropositions);
        Assert.assertEquals(1, parsedPropositions.propositionInfoToCache.size());
        PropositionInfo iamPropositionInfo = parsedPropositions.propositionInfoToCache.get(mockInAppMessageIdV2);
        Assert.assertNotNull(iamPropositionInfo);
        Assert.assertEquals("inapp2", iamPropositionInfo.id);
        Assert.assertEquals(0, parsedPropositions.propositionsToCache.size());
        Assert.assertEquals(1, parsedPropositions.propositionsToPersist.size());
        List<MessagingProposition> iamPersist = parsedPropositions.propositionsToPersist.get(mockInAppSurfaceV2);
        Assert.assertNotNull(iamPersist);
        Assert.assertEquals(1, iamPersist.size());
        Assert.assertEquals("inapp2", iamPersist.get(0).getUniqueId());
        Assert.assertEquals(1, parsedPropositions.surfaceRulesByInboundType.size());
        Map<Surface, List<LaunchRule>> iamRules = parsedPropositions.surfaceRulesByInboundType.get(InboundType.INAPP);
        Assert.assertNotNull(iamRules);
        Assert.assertEquals(1, iamRules.size());
    }

    @Test
    public void test_parsedPropositionConstructor_WithMultipleInAppPropositionTypes() {
        // setup
        Map<Surface, List<MessagingProposition>> propositions = new HashMap<Surface, List<MessagingProposition>>() {{
            put(mockInAppSurface, new ArrayList<MessagingProposition>() {{
                add(mockInAppProposition);
            }});
            put(mockInAppSurfaceV2, new ArrayList<MessagingProposition>() {{
                add(mockInAppPropositionV2);
            }});
        }};

        // test
        ParsedPropositions parsedPropositions = new ParsedPropositions(
                propositions,
                new ArrayList<Surface>() {{
                    add(mockInAppSurface);
                    add(mockInAppSurfaceV2);
                }},
                mockExtensionApi);


        //verify
        Assert.assertNotNull(parsedPropositions);
        Assert.assertEquals(2, parsedPropositions.propositionInfoToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToCache.size());
        Assert.assertEquals(2, parsedPropositions.propositionsToPersist.size());
        Assert.assertEquals(1, parsedPropositions.surfaceRulesByInboundType.size());
        Map<Surface, List<LaunchRule>> iamRules = parsedPropositions.surfaceRulesByInboundType.get(InboundType.INAPP);
        Assert.assertNotNull(iamRules);
        Assert.assertEquals(2, iamRules.size());
    }

    @Test
    public void test_parsedPropositionConstructor_WithFeedProposition() {
        // setup
        Map<Surface, List<MessagingProposition>> propositions = new HashMap<Surface, List<MessagingProposition>>() {{
            put(mockFeedSurface, new ArrayList<MessagingProposition>() {{
                add(mockFeedProposition);
            }});
        }};

        // test
        ParsedPropositions parsedPropositions = new ParsedPropositions(
                propositions,
                new ArrayList<Surface>() {{
                    add(mockFeedSurface);
                }},
                mockExtensionApi);


        //verify
        Assert.assertNotNull(parsedPropositions);
        Assert.assertEquals(1, parsedPropositions.propositionInfoToCache.size());
        PropositionInfo feedPropositionInfo = parsedPropositions.propositionInfoToCache.get(mockFeedMessageId);
        Assert.assertNotNull(feedPropositionInfo);
        Assert.assertEquals("feed", feedPropositionInfo.id);
        Assert.assertEquals(0, parsedPropositions.propositionsToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToPersist.size());
        Assert.assertEquals(1, parsedPropositions.surfaceRulesByInboundType.size());
        Map<Surface, List<LaunchRule>> feedRules = parsedPropositions.surfaceRulesByInboundType.get(InboundType.FEED);
        Assert.assertNotNull(feedRules);
        Assert.assertEquals(1, feedRules.size());
    }

    @Test
    public void test_parsedPropositionConstructor_WithCodeBasedProposition() {
        // setup
        Map<Surface, List<MessagingProposition>> propositions = new HashMap<Surface, List<MessagingProposition>>() {{
            put(mockCodeBasedSurface, new ArrayList<MessagingProposition>() {{
                add(mockCodeBasedProposition);
            }});
        }};

        // test
        ParsedPropositions parsedPropositions = new ParsedPropositions(
                propositions,
                new ArrayList<Surface>() {{
                    add(mockCodeBasedSurface);
                }},
                mockExtensionApi);


        //verify
        Assert.assertNotNull(parsedPropositions);
        Assert.assertEquals(0, parsedPropositions.propositionInfoToCache.size());
        Assert.assertEquals(1, parsedPropositions.propositionsToCache.size());
        MessagingProposition codeBasedProp = parsedPropositions.propositionsToCache.get(mockCodeBasedSurface).get(0);
        Assert.assertEquals(mockCodeBasedContent, codeBasedProp.getItems().get(0).getContent());
        Assert.assertEquals(0, parsedPropositions.propositionsToPersist.size());
        Assert.assertEquals(0, parsedPropositions.surfaceRulesByInboundType.size());
    }

    @Test
    public void test_parsedPropositionConstructor_PropositionItemEmptyContentString() {
        // setup
        mockInAppPropositionItem = new MessagingPropositionItem("inapp", "inapp", "");
        mockInAppProposition = new MessagingProposition("inapp",
                mockInAppSurface.getUri(),
                mockScopeDetails,
                new ArrayList<MessagingPropositionItem>() {{
                    add(mockInAppPropositionItem);
                }});
        Map<Surface, List<MessagingProposition>> propositions = new HashMap<Surface, List<MessagingProposition>>() {{
            put(mockInAppSurface, new ArrayList<MessagingProposition>() {{
                add(mockInAppProposition);
            }});
        }};

        // test
        ParsedPropositions parsedPropositions = new ParsedPropositions(
                propositions,
                new ArrayList<Surface>() {{
                    add(mockInAppSurface);
                }},
                mockExtensionApi);


        //verify
        Assert.assertNotNull(parsedPropositions);
        Assert.assertEquals(0, parsedPropositions.propositionInfoToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToPersist.size());
        Assert.assertEquals(0, parsedPropositions.surfaceRulesByInboundType.size());
    }

    @Test
    public void test_parsedPropositionConstructor_PropositionRuleWithNoConsequence() {
        // setup
        final String ruleWithNoConsequenceContent = MessagingTestUtils.loadStringFromFile("ruleWithNoConsequence.json");
        mockInAppPropositionItem = new MessagingPropositionItem("inapp", "inapp", ruleWithNoConsequenceContent);
        mockInAppProposition = new MessagingProposition("inapp",
                mockInAppSurface.getUri(),
                mockScopeDetails,
                new ArrayList<MessagingPropositionItem>() {{
                    add(mockInAppPropositionItem);
                }});
        Map<Surface, List<MessagingProposition>> propositions = new HashMap<Surface, List<MessagingProposition>>() {{
            put(mockInAppSurface, new ArrayList<MessagingProposition>() {{
                add(mockInAppProposition);
            }});
        }};

        // test
        ParsedPropositions parsedPropositions = new ParsedPropositions(
                propositions,
                new ArrayList<Surface>() {{
                    add(mockInAppSurface);
                }},
                mockExtensionApi);


        //verify
        Assert.assertNotNull(parsedPropositions);
        Assert.assertEquals(0, parsedPropositions.propositionInfoToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToPersist.size());
        Assert.assertEquals(0, parsedPropositions.surfaceRulesByInboundType.size());
    }

    @Test
    public void test_parsedPropositionConstructor_PropositionRuleWithUnknownSchema() {
        // setup
        final String ruleWithUnknownConsequenceSchema = MessagingTestUtils.loadStringFromFile("ruleWithUnknownConsequenceSchema.json");
        mockInAppPropositionItem = new MessagingPropositionItem("inapp", "inapp", ruleWithUnknownConsequenceSchema);
        mockInAppProposition = new MessagingProposition("inapp",
                mockInAppSurface.getUri(),
                mockScopeDetails,
                new ArrayList<MessagingPropositionItem>() {{
                    add(mockInAppPropositionItem);
                }});
        Map<Surface, List<MessagingProposition>> propositions = new HashMap<Surface, List<MessagingProposition>>() {{
            put(mockInAppSurface, new ArrayList<MessagingProposition>() {{
                add(mockInAppProposition);
            }});
        }};

        // test
        ParsedPropositions parsedPropositions = new ParsedPropositions(
                propositions,
                new ArrayList<Surface>() {{
                    add(mockInAppSurface);
                }},
                mockExtensionApi);


        //verify
        Assert.assertNotNull(parsedPropositions);
        Assert.assertEquals(1, parsedPropositions.propositionInfoToCache.size());
        Assert.assertEquals(1, parsedPropositions.propositionsToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToPersist.size());
        Assert.assertEquals(1, parsedPropositions.surfaceRulesByInboundType.size());
        Map<Surface, List<LaunchRule>> unknownRules = parsedPropositions.surfaceRulesByInboundType.get(InboundType.UNKNOWN);
        Assert.assertNotNull(unknownRules);
        Assert.assertEquals(1, unknownRules.size());
    }
}
