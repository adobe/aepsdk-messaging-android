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
import com.adobe.marketing.mobile.gradle.BuildConstants

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val mavenCoreVersion: String by project

android {
      namespace = "com.adobe.marketing.mobile.messagingtestutils"

    defaultConfig {
        compileSdk = BuildConstants.Versions.COMPILE_SDK_VERSION
        minSdk = BuildConstants.Versions.MIN_SDK_VERSION
        targetSdk = BuildConstants.Versions.TARGET_SDK_VERSION
        versionCode = BuildConstants.Versions.VERSION_CODE
        versionName = BuildConstants.Versions.VERSION_NAME
    }

    buildTypes {
        getByName(BuildConstants.BuildTypes.RELEASE)  {
            isMinifyEnabled = false
        }
    }
    kotlinOptions {
        jvmTarget = BuildConstants.Versions.KOTLIN_JVM_TARGET
        languageVersion = BuildConstants.Versions.KOTLIN_LANGUAGE_VERSION
        apiVersion = BuildConstants.Versions.KOTLIN_API_VERSION
    }
}

dependencies {
    implementation(project(":messaging"))
    implementation(project(":core"))

    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.7")
    implementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.mockito:mockito-core:4.5.1")
    implementation("org.mockito:mockito-inline:4.5.1")
    implementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
}