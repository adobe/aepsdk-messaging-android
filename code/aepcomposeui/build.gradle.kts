plugins {
    id("aep-library")
}

val mavenCoreVersion: String by project
val aepComposeUiModuleName: String by project
val aepComposeUiVersion: String by project
val aepComposeUiMavenRepoName: String by project
val aepComposeUiMavenRepoDescription: String by project

aepLibrary {
    namespace = "com.adobe.marketing.mobile.aepcomposeui"

    moduleName = aepComposeUiModuleName
    moduleVersion = aepComposeUiVersion
    enableSpotless = true
    enableCheckStyle = true
    enableDokkaDoc = true

    publishing {
        mavenRepoName = aepComposeUiMavenRepoName
        mavenRepoDescription = aepComposeUiMavenRepoDescription
        gitRepoName = "aepsdk-messsaging-android"
    }
}

dependencies {
    testImplementation("org.robolectric:robolectric:4.7")
    testImplementation("io.mockk:mockk:1.13.11")
}


