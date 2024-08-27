/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging

import org.junit.Assert.assertEquals
import org.junit.Test

class EnvironmentTests {

    @Test
    fun testEnvironmentValues() {
        val environments = Environment.values()
        assertEquals(4, environments.size)
        assertEquals(Environment.PROD_VA7, environments[0])
        assertEquals(Environment.PROD_AUS5, environments[1])
        assertEquals(Environment.PROD_NLD2, environments[2])
        assertEquals(Environment.STAGE_VA7, environments[3])
    }

    @Test
    fun testEnvironmentValueOf() {
        assertEquals(Environment.PROD_VA7, Environment.valueOf("PROD_VA7"))
        assertEquals(Environment.PROD_AUS5, Environment.valueOf("PROD_AUS5"))
        assertEquals(Environment.PROD_NLD2, Environment.valueOf("PROD_NLD2"))
        assertEquals(Environment.STAGE_VA7, Environment.valueOf("STAGE_VA7"))
    }

    @Test
    fun testGetAppId() {
        Environment.buildConfigEnvironment = "prodVA7"
        assertEquals("3149c49c3910/d255d2ca2e85/launch-750429361c0c-development", Environment.getAppId())

        Environment.buildConfigEnvironment = "prodAUS5"
        assertEquals("3269cfd2f1f9/13bf39b5c459/launch-e6e27a440c61-development", Environment.getAppId())

        Environment.buildConfigEnvironment = "prodNLD2"
        assertEquals("bf7248f92b53/ed0ea2d62097/launch-a0faa600f503-development", Environment.getAppId())

        Environment.buildConfigEnvironment = "stageVA7"
        assertEquals("staging/1b50a869c4a2/0ae7a3b5fdbf/launch-55942f2836d4-development", Environment.getAppId())
    }

    @Test
    fun testGetAppId_InvalidConfigEnvironment() {
        // invalid config environments will default to use the prodVA7 App ID
        Environment.buildConfigEnvironment = "invalid"
        assertEquals("3149c49c3910/d255d2ca2e85/launch-750429361c0c-development", Environment.getAppId())
    }

    @Test
    fun testConfigurationUpdates() {
        Environment.buildConfigEnvironment = "stageVA7"
        assertEquals(mapOf("edge.environment" to "int"), Environment.configurationUpdates())

        Environment.buildConfigEnvironment = "prodVA7"
        assertEquals(mapOf("edge.environment" to ""), Environment.configurationUpdates())

        Environment.buildConfigEnvironment = "prodNLD2"
        assertEquals(mapOf("edge.environment" to ""), Environment.configurationUpdates())

        Environment.buildConfigEnvironment = "prodAUS5"
        assertEquals(mapOf("edge.environment" to ""), Environment.configurationUpdates())
    }

    @Test
    fun testConfigurationUpdates_InvalidConfigEnvironment() {
        // invalid config environments will default to use the prodVA7 configuration
        Environment.buildConfigEnvironment = "invalid"
        assertEquals(mapOf("edge.environment" to ""), Environment.configurationUpdates())
    }
}