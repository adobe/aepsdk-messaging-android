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

import androidx.annotation.VisibleForTesting

// Environment: Production
// Org: AEM Assets Departmental - Campaign (906E3A095DC834230A495FD6@AdobeOrg)
// Sandbox: Prod (VA7)
// Data Collection tag: Android Messaging E2E Test ProdVA7
// App Surface: android messaging functional tests (com.adobe.marketing.mobile.messaging.e2etest)
// Datastream: android messaging functional test datastream (63b13590-156b-427e-8981-548d711644ca)
// AppID for SDK configuration: 3149c49c3910/d255d2ca2e85/launch-750429361c0c-development

// Environment: Production
// Org: CJM Prod AUS5 (404C2CDA605B7E960A495FCE@AdobeOrg)
// Sandbox: Prod (AUS5)
// Data Collection tag: Android Messaging E2E Test AUS5
// App Surface: AJO - IAM E2E Automated tests (com.adobe.marketing.mobile.messaging.e2etest)
// Datastream: Android Messaging E2E Test ProdAUS5 (e027f69b-3bcf-4505-b100-01db317a16d1)
// AppID for SDK configuration: 3269cfd2f1f9/13bf39b5c459/launch-e6e27a440c61-development

// Environment: Production
// Org: CJM Prod NLD2 (4DA0571C5FDC4BF70A495FC2@AdobeOrg)
// Sandbox: Prod (NLD2)
// Data Collection tag: Android Messaging E2E Test ProdNLD2
// App Surface: AJO - IAM E2E Automated tests (com.adobe.marketing.mobile.messaging.e2etest)
// Datastream: Android Messaging E2E Test ProdNLD2 (6506a534-c67e-4e0a-9055-43deea6645ac)
// AppID for SDK configuration: bf7248f92b53/ed0ea2d62097/launch-a0faa600f503-development

// Environment: Stage
// Org: CJM Stage (745F37C35E4B776E0A49421B@AdobeOrg)
// Sandbox: AJO Web (VA7)
// Data Collection tag: Android Messaging E2E Test CJMStage
// App Surface: AJO - IAM E2E Automated tests (com.adobe.marketing.mobile.messaging.e2etest)
// Datastream: Android Messaging E2E Test CJMStage (bf13a388-bf50-461f-8567-eed128195a7a)
// AppID for SDK configuration: staging/1b50a869c4a2/0ae7a3b5fdbf/launch-55942f2836d4-development

enum class Environment {
    PROD_VA7,
    PROD_AUS5,
    PROD_NLD2,
    STAGE_VA7;

    companion object {
        @VisibleForTesting
        var buildConfigEnvironment: String = BuildConfig.ADOBE_ENVIRONMENT

        /**
         * Gets the App ID for the current environment set in the BuildConfig.
         * If the BuildConfig is unavailable or does not contain the correct setting,
         * this method returns the appId for the `prodVA7` environment.
         * @return the App ID [String] for the current environment
         */
        @JvmStatic
        fun getAppId(): String {
            return when (getEnvironmentFromBuildConfig()) {
                PROD_VA7 -> "3149c49c3910/d255d2ca2e85/launch-750429361c0c-development"
                PROD_AUS5 -> "3269cfd2f1f9/13bf39b5c459/launch-e6e27a440c61-development"
                PROD_NLD2 -> "bf7248f92b53/ed0ea2d62097/launch-a0faa600f503-development"
                STAGE_VA7 -> "staging/1b50a869c4a2/0ae7a3b5fdbf/launch-55942f2836d4-development"
            }
        }

        /**
         * Gets the configuration update [Map] for the current environment set in the BuildConfig.
         * @return the configuration update [Map] for the current environment
         */
        @JvmStatic
        fun configurationUpdates(): Map<String, Any> {
            return when (getEnvironmentFromBuildConfig()) {
                STAGE_VA7 -> mapOf("edge.environment" to "int")
                else -> mapOf("edge.environment" to "")
            }
        }

        /**
         * Creates an [Environment] from the environment [String] set in the BuildConfig.
         * If the BuildConfig is unavailable or does not contain the correct setting,
         * this method defaults to [PROD_VA7].
         * @return [Environment] created from the converted environment string
         */
        private fun getEnvironmentFromBuildConfig(): Environment {
            return when (buildConfigEnvironment) {
                "prodVA7" -> PROD_VA7
                "prodAUS5" -> PROD_AUS5
                "prodNLD2" -> PROD_NLD2
                "stageVA7" -> STAGE_VA7
                else -> PROD_VA7
            }
        }
    }

}