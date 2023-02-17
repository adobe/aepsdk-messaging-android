# Install the AEPMessaging extension

> [!INFO]
> Using the `AEPMessaging` extension requires that `AEPCore`, `AEPEdge`, and `AEPEdgeIdentity` extensions also be installed in your mobile application.

The following installation options are currently supported when integrating the Adobe Experience Platform Mobile SDK extensions:

## Maven

Add the maven central repository to your app's top level gradle file

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
}
```

Add the AEP Extension dependencies to your app's gradle file

```groovy
implementation 'com.adobe.marketing.mobile:messaging:2.+'
implementation 'com.adobe.marketing.mobile:edge:2.+'
implementation 'com.adobe.marketing.mobile:edgeidentity:2.+'
implementation 'com.adobe.marketing.mobile:core:2.+'
```

## Manual Installation

Build the Messaging extension `aar` by running

```
make ci-build
```

This will generate the `aar` file and will place it in the `aepsdk-messaging-android/ci/assemble/build/outputs/aar` directory. Move the `messaging-release-2.0.1.aar` to your app's `libs` folder. In your app's gradle file add:

```groovy
dependencies {
  	...
		implementation fileTree(include: ['*.aar'], dir: 'libs')
}
```

The other AEPExtensions must also be built and placed in the `libs` directory if those extensions will be manually installed.
