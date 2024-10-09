plugins {
    id("aep-library")
}

val mavenCoreVersion: String by project
val aepUiTemplatesModuleName: String by project
val aepUiTemplatesVersion: String by project
val aepUiTemplatesMavenRepoName: String by project
val aepUiTemplatesMavenRepoDescription: String by project

aepLibrary {
    namespace = "com.adobe.marketing.mobile.aepuitemplates"

    moduleName = aepUiTemplatesModuleName
    moduleVersion = aepUiTemplatesVersion
    enableSpotless = true
    enableCheckStyle = true
    enableDokkaDoc = true

    publishing {
        mavenRepoName = aepUiTemplatesMavenRepoName
        mavenRepoDescription = aepUiTemplatesMavenRepoDescription
        gitRepoName = "aepsdk-messaging-android"
        addCoreDependency(mavenCoreVersion)
    }
}

dependencies {
    testImplementation("org.robolectric:robolectric:4.7")
    testImplementation("io.mockk:mockk:1.13.11")
}