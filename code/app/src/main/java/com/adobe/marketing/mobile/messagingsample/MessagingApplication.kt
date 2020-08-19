package com.adobe.marketing.mobile.messagingsample

import android.app.Application
import com.adobe.marketing.mobile.*
import com.google.firebase.iid.FirebaseInstanceId

class MessagingApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        Messaging.registerExtension()
        Identity.registerExtension()
        UserProfile.registerExtension()
        Lifecycle.registerExtension()
        Signal.registerExtension()
        ExperiencePlatform.registerExtension();

        MobileCore.start {
            // Necessary property id for MessagingSDKTest which has the edge configuration id needed by aep sdk
            MobileCore.configureWithAppID("3805cb8645dd/b8dec0fe156d/launch-7dfbe727ca00-development")
            MobileCore.lifecycleStart(null);
        }

        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener{ task ->
            if(task.isSuccessful) {
                val token = task.result?.token ?: ""
                print("MessagingApplication Firebase token :: $token")
                MobileCore.setPushIdentifier(token)
            }
        }
    }
}