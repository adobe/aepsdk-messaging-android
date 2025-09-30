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
    id("io.github.takahirom.roborazzi")
}

val mavenCoreVersion: String by project
val mavenEdgeVersion: String by project
val mavenEdgeIdentityVersion: String by project
// Lowest material3 library version we can use is v1.2.0
// since clickable Cards are marked @ExperimentalMaterial3Api in lower versions
val material3Version = "1.2.0"
val viewModelComposeVersion = "2.6.0"
val runtimeComposeVersion = "2.6.0"

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
        addMavenDependency("androidx.lifecycle", "lifecycle-viewmodel-compose", viewModelComposeVersion)
        addMavenDependency("androidx.lifecycle", "lifecycle-runtime-compose", runtimeComposeVersion)
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

        testOptions.unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation("com.adobe.marketing.mobile:core:$mavenCoreVersion")
    // dependencies provided by aep-library:
    // COMPOSE_RUNTIME, COMPOSE_MATERIAL, ANDROIDX_ACTIVITY_COMPOSE, COMPOSE_UI_TOOLING
    implementation("androidx.compose.ui:ui-tooling-preview:${BuildConstants.Versions.COMPOSE}")
    implementation("androidx.compose.material3:material3:$material3Version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$viewModelComposeVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$runtimeComposeVersion")

    compileOnly("com.google.firebase:firebase-messaging:23.4.1")

    // testImplementation dependencies provided by aep-library:
    // MOCKITO_CORE, MOCKITO_INLINE, JSON
    testImplementation(project(":messagingtestutils"))
    testImplementation("com.google.firebase:firebase-messaging:23.4.1")
    testImplementation(BuildConstants.Dependencies.MOCKK)
    testImplementation(BuildConstants.Dependencies.ESPRESSO_CORE)
    testImplementation(BuildConstants.Dependencies.COMPOSE_UI_TEST_JUNIT4)
    testImplementation(BuildConstants.Dependencies.COMPOSE_UI_TEST_MANIFEST)
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.32.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.32.2")
    // need to use robolectric 4.14 to get android 35 support in unit tests
    testImplementation("org.robolectric:robolectric:4.14")

    // androidTestImplementation dependencies provided by aep-library:
    // ANDROIDX_TEST_EXT_JUNIT, ESPRESSO_CORE, COMPOSE_UI_TEST_JUNIT4, COMPOSE_UI_TEST_MANIFEST
    androidTestImplementation("com.fasterxml.jackson.core:jackson-databind:2.12.7.1")
    androidTestImplementation("com.adobe.marketing.mobile:edge:$mavenEdgeVersion")
    androidTestImplementation("com.adobe.marketing.mobile:edgeidentity:$mavenEdgeIdentityVersion")
    androidTestImplementation(project(":messagingtestutils"))
    // specify byte buddy version to fix compatibility issue with jdk 21
    testImplementation ("org.mockito:mockito-inline:5.2.0"){
        exclude(group = "net.bytebuddy", module = "byte-buddy")
    }
    testImplementation("net.bytebuddy:byte-buddy:1.14.17")
}
