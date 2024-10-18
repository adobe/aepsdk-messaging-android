package com.adobe.marketing.mobile

import android.util.Log
import com.adobe.marketing.mobile.aepcomposeui.aepui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.interactions.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepuitemplates.AepUITemplate

class MessagingUiEventObserver(private val callback: ContentCardCallback?): AepUIEventObserver {
    override fun onEvent(event: UIEvent<*, *>) {
        when(event) {
            is UIEvent.Display -> {
            }
            is UIEvent.Interact -> {
                if (callback?.onCardClick(event.aepUi.getTemplate()) != true) {
                    handleClickEvent(event)
                }
            }
            is UIEvent.Dismiss -> {
            }
        }
    }

    private fun handleClickEvent(event: UIEvent.Interact<*, *>) {
        val aepUi = event.aepUi
        when(aepUi) {
            is SmallImageUI -> {
                Log.d("AepUiEventObserverImpl", "SmallImageAepUi Click")
            }
        }
    }

}

interface ContentCardCallback {
    fun onCardClick(template: AepUITemplate): Boolean
    fun onCardDismiss(template: AepUITemplate)
}

