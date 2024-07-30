/*
 * Copyright 2024 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
plugins {
    id("aep-library")
}

val mavenCoreVersion: String by project
val mavenEdgeVersion: String by project
val mavenEdgeIdentityVersion: String by project

aepLibrary {
    namespace = "com.adobe.marketing.mobile.messaging"
    enableSpotless = true
    enableCheckStyle = true

    publishing {
        gitRepoName = "aepsdk-messaging-android"
        addCoreDependency(mavenCoreVersion)
        addEdgeDependency(mavenEdgeVersion)
    }

    android {
        defaultConfig {
            buildConfigField("java.util.concurrent.atomic.AtomicBoolean", "IS_E2E_TEST", "new java.util.concurrent.atomic.AtomicBoolean(false)")
            buildConfigField("java.util.concurrent.atomic.AtomicBoolean", "IS_FUNCTIONAL_TEST", "new java.util.concurrent.atomic.AtomicBoolean(false)")
        }

        sourceSets {
            named("test").configure { resources.srcDir("src/test/resources") }
            named("androidTest").configure { resources.srcDir("src/test/resources") }
        }
    }
}

dependencies {
    implementation("com.adobe.marketing.mobile:core:$mavenCoreVersion")
    compileOnly("com.google.firebase:firebase-messaging:23.4.1")

    // testImplementation dependencies provided by aep-library:
    // MOCKITO_CORE, MOCKITO_INLINE, JSON
    testImplementation(project(":messagingtestutils"))
    testImplementation("com.google.firebase:firebase-messaging:23.4.1")

    // androidTestImplementation dependencies provided by aep-library:
    // ANDROIDX_TEST_EXT_JUNIT, ESPRESSO_CORE
    androidTestImplementation("com.fasterxml.jackson.core:jackson-databind:2.12.7.1")
    androidTestImplementation("com.adobe.marketing.mobile:edge:$mavenEdgeVersion") {
        exclude(group = "com.adobe.marketing.mobile", module = "core")
        exclude(group = "com.adobe.marketing.mobile", module = "edgeidentity")
    }
    androidTestImplementation("com.adobe.marketing.mobile:edgeidentity:$mavenEdgeIdentityVersion") {
        exclude(group = "com.adobe.marketing.mobile", module = "core")
    }
    androidTestImplementation(project(":messagingtestutils"))
}
