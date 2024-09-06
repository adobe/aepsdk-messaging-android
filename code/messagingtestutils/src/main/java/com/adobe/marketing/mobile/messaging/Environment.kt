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
// App Surface: android messaging functional tests (com.adobe.marketing.mobile.messaging.test)
// Datastream: android messaging functional test datastream (63b13590-156b-427e-8981-548d711644ca)
// AppID for SDK configuration: 3149c49c3910/d255d2ca2e85/launch-750429361c0c-development

// Environment: Production
// Org: CJM Prod AUS5 (404C2CDA605B7E960A495FCE@AdobeOrg)
// Sandbox: Prod (AUS5)
// Data Collection tag: Android Messaging E2E Test AUS5
// App Surface: AJO - IAM E2E Automated tests (com.adobe.marketing.mobile.messaging.test)
// Datastream: Android Messaging E2E Test ProdAUS5 (e027f69b-3bcf-4505-b100-01db317a16d1)
// AppID for SDK configuration: 3269cfd2f1f9/13bf39b5c459/launch-e6e27a440c61-development

// Environment: Production
// Org: CJM Prod NLD2 (4DA0571C5FDC4BF70A495FC2@AdobeOrg)
// Sandbox: Prod (NLD2)
// Data Collection tag: Android Messaging E2E Test ProdNLD2
// App Surface: AJO - IAM E2E Automated tests (com.adobe.marketing.mobile.messaging.test)
// Datastream: Android Messaging E2E Test ProdNLD2 (6506a534-c67e-4e0a-9055-43deea6645ac)
// AppID for SDK configuration: bf7248f92b53/ed0ea2d62097/launch-a0faa600f503-development

// Environment: Stage
// Org: CJM Stage (745F37C35E4B776E0A49421B@AdobeOrg)
// Sandbox: AJO Web (VA7)
// Data Collection tag: Android Messaging E2E Test CJMStage
// App Surface: AJO - IAM E2E Automated tests (com.adobe.marketing.mobile.messaging.test)
// Datastream: Android Messaging E2E Test CJMStage (1ddb7c29-5a00-4fbc-b8b3-1370ddc5bacc)
// AppID for SDK configuration: staging/1b50a869c4a2/dfc312636dbd/launch-325dd1746c45-development

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
                STAGE_VA7 -> "staging/1b50a869c4a2/dfc312636dbd/launch-325dd1746c45-development"
            }
        }

        /**
         * Gets the configuration update [Map] for the current environment set in the BuildConfig.
         * @return the configuration update [Map] for the current environment
         */
        @JvmStatic
        fun configurationUpdates(): Map<String, Any> {
            return when (getEnvironmentFromBuildConfig()) {
                STAGE_VA7 -> mapOf("edge.environment" to "int", "edge.configId" to "1ddb7c29-5a00-4fbc-b8b3-1370ddc5bacc")
                else -> mapOf("edge.environment" to "")
            }
        }

        /**
         * Gets the expected message ID for the show once in-app messages.
         * @return a [String] containing the expected message ID for the show once in-app messages
         */
        @JvmStatic
        fun getShowOnceMessageId(): String {
            return when (getEnvironmentFromBuildConfig()) {
                PROD_VA7 -> "2c0a68ea-eda2-4d79-8d27-28e2d5df6ce1#511a8b8e-a42e-4d1b-8621-b1b45370b3a8"
                PROD_AUS5 -> "5815a673-a48d-4486-aaad-bd3184d9fa9f#e314702b-afef-4e83-bb90-73e1b7dff6eb"
                PROD_NLD2 -> "3ef2b330-fdd3-4c0a-817e-157f3c2947bd#7d828885-c0ab-4f7d-a0ea-1f94fa41f8c5"
                STAGE_VA7 -> "2540d79f-9036-45d0-95ab-6108d893d1af#c1eeb0a5-2451-4c99-8e8b-0bcc14bde703"
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