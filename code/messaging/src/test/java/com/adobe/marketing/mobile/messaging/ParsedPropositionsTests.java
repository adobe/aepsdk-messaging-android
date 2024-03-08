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

import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ParsedPropositionsTests {

    private Surface mockSurface;

    private PropositionItem mockInAppPropositionItem;
    private Proposition mockInAppProposition;

    private Surface mockInAppSurface;
    private final String mockInAppConsequenceId = "mockInAppConsequenceId";

    private PropositionItem mockFeedPropositionItem;
    private Proposition mockFeedProposition;
    private Surface mockFeedSurface;
    private final String mockFeedMessageId = "183639c4-cb37-458e-a8ef-4e130d767ebf";
    private Map<String, Object> mockFeedContent;

    private PropositionItem mockCodeBasedPropositionItem;
    private Proposition mockCodeBasedProposition;
    private Surface mockCodeBasedSurface;
    private Map<String, Object> mockCodeBasedContent;
    private Map<String, Object> inappPropositionContent;

    private final Map<String, Object> mockScopeDetails = new HashMap<String, Object>() {{
        put("key", "value");
    }};

    @Mock
    private ExtensionApi mockExtensionApi;

    @Before
    public void setup() throws JSONException {

        mockSurface = Surface.fromUriString("mobileapp://some.not.matching.surface/path");
        mockInAppSurface = Surface.fromUriString("mobileapp://mockPackageName/inapp2");
        inappPropositionContent = MessagingTestUtils.getMapFromFile("inAppRule.json");
        mockInAppPropositionItem = new PropositionItem("inapp2", SchemaType.RULESET, inappPropositionContent);
        mockInAppProposition = new Proposition("inapp2",
                mockInAppSurface.getUri(),
                mockScopeDetails,
                new ArrayList<PropositionItem>() {{
                    add(mockInAppPropositionItem);
                }});

        mockFeedSurface = Surface.fromUriString("mobileapp://mockPackageName/feed");
        mockFeedContent = MessagingTestUtils.getMapFromFile("feedPropositionContent.json");
        mockFeedPropositionItem = new PropositionItem("feed", SchemaType.RULESET, mockFeedContent);
        mockFeedProposition = new Proposition("feed",
                mockFeedSurface.getUri(),
                mockScopeDetails,
                new ArrayList<PropositionItem>() {{
                    add(mockFeedPropositionItem);
                }});

        mockCodeBasedSurface = Surface.fromUriString("mobileapp://mockPackageName/codebased");
        mockCodeBasedContent = MessagingTestUtils.getMapFromFile("codeBasedPropositionContent.json");
        mockCodeBasedPropositionItem = new PropositionItem("codebased", SchemaType.JSON_CONTENT, mockCodeBasedContent);
        mockCodeBasedProposition = new Proposition("codebased",
                mockCodeBasedSurface.getUri(),
                mockScopeDetails,
                new ArrayList<PropositionItem>() {{
                    add(mockCodeBasedPropositionItem);
                }});
    }

    @Test
    public void test_parsedPropositionConstructor_WithEmptyPropositions() {
        // setup
        Map<Surface, List<Proposition>> propositions = new HashMap<Surface, List<Proposition>>() {{
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
        Assert.assertEquals(0, parsedPropositions.surfaceRulesBySchemaType.size());
    }

    @Test
    public void test_parsedPropositionConstructor_WithPropositionScopeNotMatchingRequestedSurfaces() {
        // setup
        Map<Surface, List<Proposition>> propositions = new HashMap<Surface, List<Proposition>>() {{
            put(mockFeedSurface, new ArrayList<Proposition>() {{
                add(mockFeedProposition);
            }});
            put(mockCodeBasedSurface, new ArrayList<Proposition>() {{
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
        Assert.assertEquals(0, parsedPropositions.surfaceRulesBySchemaType.size());
    }

    @Test
    public void test_parsedPropositionConstructor_WithInAppProposition() {
        // setup
        Map<Surface, List<Proposition>> propositions = new HashMap<Surface, List<Proposition>>() {{
            put(mockInAppSurface, new ArrayList<Proposition>() {{
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
        PropositionInfo iamPropositionInfo = parsedPropositions.propositionInfoToCache.get(mockInAppConsequenceId);
        Assert.assertNotNull(iamPropositionInfo);
        Assert.assertEquals("inapp2", iamPropositionInfo.id);
        Assert.assertEquals(0, parsedPropositions.propositionsToCache.size());
        Assert.assertEquals(1, parsedPropositions.propositionsToPersist.size());
        List<Proposition> iamPersist = parsedPropositions.propositionsToPersist.get(mockInAppSurface);
        Assert.assertNotNull(iamPersist);
        Assert.assertEquals(1, iamPersist.size());
        Assert.assertEquals("inapp2", iamPersist.get(0).getUniqueId());
        Assert.assertEquals(1, parsedPropositions.surfaceRulesBySchemaType.size());
        Map<Surface, List<LaunchRule>> iamRules = parsedPropositions.surfaceRulesBySchemaType.get(SchemaType.INAPP);
        Assert.assertNotNull(iamRules);
        Assert.assertEquals(1, iamRules.size());
    }

    @Test
    public void test_parsedPropositionConstructor_WithFeedProposition() {
        // setup
        Map<Surface, List<Proposition>> propositions = new HashMap<Surface, List<Proposition>>() {{
            put(mockFeedSurface, new ArrayList<Proposition>() {{
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
        Assert.assertEquals(1, parsedPropositions.surfaceRulesBySchemaType.size());
        Map<Surface, List<LaunchRule>> feedRules = parsedPropositions.surfaceRulesBySchemaType.get(SchemaType.FEED);
        Assert.assertNotNull(feedRules);
        Assert.assertEquals(1, feedRules.size());
    }

    @Test
    public void test_parsedPropositionConstructor_WithCodeBasedProposition() {
        // setup
        Map<Surface, List<Proposition>> propositions = new HashMap<Surface, List<Proposition>>() {{
            put(mockCodeBasedSurface, new ArrayList<Proposition>() {{
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
        Proposition codeBasedProp = parsedPropositions.propositionsToCache.get(mockCodeBasedSurface).get(0);
        Assert.assertEquals(mockCodeBasedContent, codeBasedProp.getItems().get(0).getData());
        Assert.assertEquals(0, parsedPropositions.propositionsToPersist.size());
        Assert.assertEquals(0, parsedPropositions.surfaceRulesBySchemaType.size());
    }

    @Test
    public void test_parsedPropositionConstructor_PropositionItemEmptyMap() {
        // setup
        mockInAppPropositionItem = new PropositionItem("inapp", SchemaType.RULESET, null);
        mockInAppProposition = new Proposition("inapp",
                mockInAppSurface.getUri(),
                mockScopeDetails,
                new ArrayList<PropositionItem>() {{
                    add(mockInAppPropositionItem);
                }});
        Map<Surface, List<Proposition>> propositions = new HashMap<Surface, List<Proposition>>() {{
            put(mockInAppSurface, new ArrayList<Proposition>() {{
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
        Assert.assertEquals(0, parsedPropositions.surfaceRulesBySchemaType.size());
    }

    @Test
    public void test_parsedPropositionConstructor_PropositionRuleWithNoConsequence() {
        // setup
        final Map<String, Object> ruleWithNoConsequenceContent = MessagingTestUtils.getMapFromFile("ruleWithNoConsequence.json");
        mockInAppPropositionItem = new PropositionItem("inapp", SchemaType.RULESET, ruleWithNoConsequenceContent);
        mockInAppProposition = new Proposition("inapp",
                mockInAppSurface.getUri(),
                mockScopeDetails,
                new ArrayList<PropositionItem>() {{
                    add(mockInAppPropositionItem);
                }});
        Map<Surface, List<Proposition>> propositions = new HashMap<Surface, List<Proposition>>() {{
            put(mockInAppSurface, new ArrayList<Proposition>() {{
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
        Assert.assertEquals(0, parsedPropositions.surfaceRulesBySchemaType.size());
    }

    @Test
    public void test_parsedPropositionConstructor_PropositionRuleWithDefaultSchema() {
        // setup
        mockInAppPropositionItem = new PropositionItem("inapp", SchemaType.DEFAULT_CONTENT, inappPropositionContent);
        mockInAppProposition = new Proposition("inapp",
                mockInAppSurface.getUri(),
                mockScopeDetails,
                new ArrayList<PropositionItem>() {{
                    add(mockInAppPropositionItem);
                }});
        Map<Surface, List<Proposition>> propositions = new HashMap<Surface, List<Proposition>>() {{
            put(mockInAppSurface, new ArrayList<Proposition>() {{
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
        Assert.assertEquals(1, parsedPropositions.propositionsToCache.size());
        Assert.assertEquals(0, parsedPropositions.propositionsToPersist.size());
        Assert.assertEquals(0, parsedPropositions.surfaceRulesBySchemaType.size());
        Map<Surface, List<LaunchRule>> defaultRules = parsedPropositions.surfaceRulesBySchemaType.get(SchemaType.DEFAULT_CONTENT);
        Assert.assertNull(defaultRules);
    }

    @Test
    public void test_parsedPropositionConstructor_PropositionRuleWithUnknownSchema() {
        // setup
        mockInAppPropositionItem = new PropositionItem("inapp", SchemaType.UNKNOWN, inappPropositionContent);
        mockInAppProposition = new Proposition("inapp",
                mockInAppSurface.getUri(),
                mockScopeDetails,
                new ArrayList<PropositionItem>() {{
                    add(mockInAppPropositionItem);
                }});
        Map<Surface, List<Proposition>> propositions = new HashMap<Surface, List<Proposition>>() {{
            put(mockInAppSurface, new ArrayList<Proposition>() {{
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
        Assert.assertEquals(0, parsedPropositions.surfaceRulesBySchemaType.size());
        Map<Surface, List<LaunchRule>> unknownRules = parsedPropositions.surfaceRulesBySchemaType.get(SchemaType.UNKNOWN);
        Assert.assertNull(unknownRules);
    }
}
