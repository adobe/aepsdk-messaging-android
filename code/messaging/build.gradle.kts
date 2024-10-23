import com.adobe.marketing.mobile.gradle.BuildConstants

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
val toolingPreviewVersion = "1.7.4"
val viewModelComposeVersion = "2.8.6"
val runtimeComposeVersion = "2.8.6"
val material3Version = "1.3.0"

aepLibrary {
    namespace = "com.adobe.marketing.mobile.messaging"
    enableSpotless = true
    enableCheckStyle = true
    enableDokkaDoc = true
    compose = true

    publishing {
        gitRepoName = "aepsdk-messaging-android"
        addCoreDependency(mavenCoreVersion)
        addEdgeDependency(mavenEdgeVersion)

        addMavenDependency("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", BuildConstants.Versions.KOTLIN)
        addMavenDependency("androidx.appcompat", "appcompat", BuildConstants.Versions.ANDROIDX_APPCOMPAT)
        addMavenDependency("androidx.compose.runtime", "runtime", BuildConstants.Versions.COMPOSE)
        addMavenDependency("androidx.activity", "activity-compose", BuildConstants.Versions.ANDROIDX_ACTIVITY_COMPOSE)
        addMavenDependency("androidx.compose.material3", "material3", material3Version)
    }

    android {
        defaultConfig {
            buildConfigField("java.util.concurrent.atomic.AtomicBoolean", "IS_E2E_TEST", "new java.util.concurrent.atomic.AtomicBoolean(false)")
            buildConfigField("java.util.concurrent.atomic.AtomicBoolean", "IS_FUNCTIONAL_TEST", "new java.util.concurrent.atomic.AtomicBoolean(false)")
            buildConfigField("String", "ADOBE_ENVIRONMENT", "\"prodVA7\"")
        }

        sourceSets {
            named("test").configure { resources.srcDir("src/test/resources") }
            named("androidTest").configure { resources.srcDir("src/test/resources") }
        }

    }
}

dependencies {
    implementation("com.adobe.marketing.mobile:core:$mavenCoreVersion")
    implementation(BuildConstants.Dependencies.COMPOSE_UI_TOOLING)
    // Compose UI Tooling Preview
    implementation("androidx.compose.ui:ui-tooling-preview:$toolingPreviewVersion")
    // Material 3
    implementation("androidx.compose.material3:material3:$material3Version")
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
