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
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "aepsdk-messaging-android"
include(
    ":testapp",
    ":messaging",
    ":messagingtestutils",
)

// Builds the Edge extension from a local aepsdk-edge-android checkout instead of Maven.
// Every "com.adobe.marketing.mobile:edge" dependency in this build resolves to its ":edge" project.
// Enabled only when the checkout exists. Override the path with -PlocalEdgePath=<path to edge "code" dir>,
// or turn it off with -PuseLocalEdge=false.
val useLocalEdge = (providers.gradleProperty("useLocalEdge").orNull ?: "true").toBoolean()
val localEdgeDir = file(providers.gradleProperty("localEdgePath").orNull ?: "../../aepsdk-edge-android/code")
if (useLocalEdge && localEdgeDir.resolve("edge").isDirectory) {
    includeBuild(localEdgeDir) {
        name = "aepsdk-edge-android"
        dependencySubstitution {
            substitute(module("com.adobe.marketing.mobile:edge")).using(project(":edge"))
        }
    }
}