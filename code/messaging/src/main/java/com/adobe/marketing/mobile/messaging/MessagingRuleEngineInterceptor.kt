package com.adobe.marketing.mobile.messaging

import com.adobe.marketing.mobile.AdobeCallback
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine

class MessagingRuleEngineInterceptor : LaunchRulesEngine.RuleReevaluationInterceptor {
    override fun onReevaluationTriggered(
        event: Event?,
        revaluableRules: List<LaunchRule?>?,
        callback: LaunchRulesEngine.CompletionCallback?
    ) {
        refreshMessagesThenProcessEvent(callback)
    }

    private fun refreshMessagesThenProcessEvent(
        callback: LaunchRulesEngine.CompletionCallback?
    ) {
        val eventData: MutableMap<String?, Any?> = HashMap()
        eventData["refreshmessages"] = true
        val refreshMessageEvent =
            Event.Builder(
                "Refresh in-app messages",
                EventType.MESSAGING,
                EventSource.REQUEST_CONTENT
            )
                .setEventData(eventData)
                .build()

        val updateCallback = AdobeCallback<Boolean> {
            callback?.onComplete()
        }

        MessagingExtension.addCompletionHandler(
            CompletionHandler(
                refreshMessageEvent.uniqueIdentifier,
                updateCallback
            )
        )

        MobileCore.dispatchEvent(refreshMessageEvent)
    }
}