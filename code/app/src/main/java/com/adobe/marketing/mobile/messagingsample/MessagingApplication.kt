package com.adobe.marketing.mobile.messagingsample

import android.app.Application
import com.adobe.marketing.mobile.*
import com.google.firebase.iid.FirebaseInstanceId

class MessagingApplication : Application(){

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
            MobileCore.configureWithAppID("")
            MobileCore.lifecycleStart(null);
        }

        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener{ task ->
            if(task.isSuccessful){
                val token = task.result?.token ?: ""
                print("MessagingApplication Firebase token :: $token")
                MobileCore.setPushIdentifier(token)
            }
        }
    }

}