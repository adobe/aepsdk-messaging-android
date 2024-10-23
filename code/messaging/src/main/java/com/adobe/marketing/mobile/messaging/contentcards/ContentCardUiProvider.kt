package com.adobe.marketing.mobile.messaging.contentcards

import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepUIContentProvider
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.messaging.MessagingConstants
import com.adobe.marketing.mobile.messaging.MessagingExtension
import com.adobe.marketing.mobile.messaging.Proposition
import com.adobe.marketing.mobile.messaging.SchemaType
import com.adobe.marketing.mobile.messaging.Surface
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContentCardUiProvider(val surfaceString: String) : AepUIContentProvider {
    companion object {
        private const val SELF_TAG: String = "MessageAssetDownloader"
    }

    private val _contentFlow = MutableStateFlow<List<AepUITemplate>>(emptyList())
    private val contentFlow: StateFlow<List<AepUITemplate>> = _contentFlow

    override suspend fun getContent(): Flow<List<AepUITemplate>> {
        CoroutineScope(Dispatchers.IO).launch {
            val surface = Surface(surfaceString)
            val surfaceList = mutableListOf<Surface>()
            surfaceList.add(surface)
            Messaging.updatePropositionsForSurfaces(surfaceList)
            Messaging.getPropositionsForSurfaces(surfaceList) {
                getCardsForSurface(surfaceList) {
                    it.onSuccess { templateList ->
                        _contentFlow.value = templateList
                    }
                    it.onFailure { error ->
                        Log.error(MessagingConstants.LOG_TAG, SELF_TAG, "Failed to get content: ${error.message}")
                        _contentFlow.value = emptyList()
                    }
                }
            }
        }
        return contentFlow
    }

    fun getCardsForSurface(
        surfaceList: MutableList<Surface>,
        completion: (Result<List<AepUITemplate>>) -> Unit
    ) {
        // Update propositions for the provided surface
        Messaging.updatePropositionsForSurfaces(surfaceList)

        // Retrieve propositions for the provided surface
        Messaging.getPropositionsForSurfaces(surfaceList, object :
            AdobeCallbackWithError<Map<Surface, List<Proposition>>> {
            override fun call(resultMap: Map<Surface, List<Proposition>>?) {
                if (resultMap == null) {
                    completion(
                        Result.failure(
                            Throwable(
                                "resultMap null for surfaces ${
                                    surfaceList.joinToString(
                                        ","
                                    )
                                }"
                            )
                        )
                    )
                    return
                }

                val templateModelList = mutableListOf<AepUITemplate>()
                Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "getPropositionsForSurfaces callback contained Null Map"
                )

                for ((_, propositions) in resultMap.entries) {
                    for (proposition in propositions) {
                        var aepUiTemplate = buildTemplate(proposition)
                        aepUiTemplate?.let { templateModelList.add(it) }
                    }
                }
                completion(Result.success(templateModelList))
            }

            override fun fail(error: AdobeError?) {
                completion(
                    Result.failure(
                        Throwable(
                            "Failed to retrieve propositions for surface ${
                                surfaceList.joinToString(
                                    ","
                                )
                            }"
                        )
                    )
                )
            }
        })
    }

    override suspend fun refreshContent() {
        _contentFlow.value = emptyList()
        getContent()
    }

    fun buildTemplate(proposition: Proposition): AepUITemplate? {
        var baseTemplateModel: AepUITemplate? = null
        if (isContentCard(proposition)) {
            val propositionItem = proposition.items[0]
            baseTemplateModel = propositionItem.contentCardSchemaData?.let {
                getTemplateModelFromContentCardSchemaData(it)
            }
        }
        return baseTemplateModel
    }

    private fun isContentCard(proposition: Proposition): Boolean {
        return proposition.items[0].schema == SchemaType.CONTENT_CARD
    }
}