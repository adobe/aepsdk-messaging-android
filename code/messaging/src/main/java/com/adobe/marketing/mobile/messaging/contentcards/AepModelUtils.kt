package com.adobe.marketing.mobile.messaging.contentcards

import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepColor
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepDismissButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.messaging.ContentCardSchemaData
import org.json.JSONObject

object AepModelUtils {
    fun createAepText(jsonObject: JSONObject): AepText {
        return AepText(
            content = jsonObject.optString(AepUIConstants.Keys.CONTENT)
        )
    }

    fun createAepColor(jsonObject: JSONObject): AepColor {
        return AepColor(
            lightColour = jsonObject.getString(AepUIConstants.Keys.LIGHT_COLOUR),
            darkColour = jsonObject.optString(AepUIConstants.Keys.DARK_COLOUR, null)
        )
    }

    fun createAepImage(jsonObject: JSONObject): AepImage {
        return AepImage(
            url = jsonObject.optString(AepUIConstants.Keys.URL),
            darkUrl = jsonObject.optString(AepUIConstants.Keys.DARK_URL)
        )
    }

    fun createAepButton(jsonObject: JSONObject): AepButton {
        return AepButton(
            interactId = jsonObject.optString(AepUIConstants.Keys.INTERACT_ID),
            actionUrl = jsonObject.optString(AepUIConstants.Keys.ACTION_URL),
            text = createAepText(jsonObject.getJSONObject(AepUIConstants.Keys.TEXT))
        )
    }

    fun createAepDismissButton(jsonObject: JSONObject): AepDismissButton {
        return AepDismissButton(
            style = jsonObject.optString(AepUIConstants.Keys.STYLE, "NONE_ICON")
        )
    }
}

// Manual JSON parsing function
fun getTemplateModelFromContentCardSchemaData(contentCardSchemaData: ContentCardSchemaData): AepUITemplate? {
    val jsonString = contentCardSchemaData.contentJsonString
    return try {
        val jsonObject = JSONObject(jsonString)
        val id = jsonObject.getString(AepUIConstants.Keys.ID)
        val title = AepModelUtils.createAepText(jsonObject.getJSONObject(AepUIConstants.Keys.TITLE))
        val body = jsonObject.optJSONObject(AepUIConstants.Keys.BODY)?.let { AepModelUtils.createAepText(it) }
        val image = jsonObject.optJSONObject(AepUIConstants.Keys.IMAGE)?.let { AepModelUtils.createAepImage(it) }
        val actionUrl = jsonObject.optString(AepUIConstants.Keys.ACTION_URL, null)
        val buttons = jsonObject.optJSONArray(AepUIConstants.Keys.BUTTONS)?.let { buttonsArray ->
            List(buttonsArray.length()) { index ->
                AepModelUtils.createAepButton(buttonsArray.getJSONObject(index))
            }
        }
        val dismissBtn = jsonObject.optJSONObject(AepUIConstants.Keys.DISMISS_BTN)?.let { AepModelUtils.createAepDismissButton(it) }
        SmallImageTemplate(
            id = id,
            title = title,
            body = body,
            image = image,
            actionUrl = actionUrl,
            buttons = buttons,
            dismissBtn = dismissBtn
        )
    } catch (e: Exception) {
        // Handle parsing error
        println("Error parsing JSON: ${e.message}")
        null
    }
}