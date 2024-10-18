package com.adobe.marketing.mobile

import com.adobe.marketing.mobile.aepcomposeui.contentprovider.AepUIContentProvider
import com.adobe.marketing.mobile.aepuitemplates.AepUITemplate
import com.adobe.marketing.mobile.aepuitemplates.SmallImageTemplate
import com.adobe.marketing.mobile.messaging.Proposition
import com.adobe.marketing.mobile.messaging.Surface
import com.adobe.marketing.mobile.util.DataReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ContentCardContentProvider(val surface: String) :  AepUIContentProvider {
    private val _contentFlow = MutableStateFlow<List<AepUITemplate>>(emptyList())
    private val contentFlow: StateFlow<List<AepUITemplate>> = _contentFlow

    override suspend fun getContent(): Flow<List<AepUITemplate>> {
        // use "surface" to make getPropositionsForSurface call
        val surfaceList = listOf(Surface(surface))
        Messaging.getPropositionsForSurfaces(surfaceList) {
            _contentFlow.value = getTemplates(it.values.flatten())
        }
        return contentFlow
    }

    private fun getTemplates(propositionList: List<Proposition>): List<AepUITemplate> {
        val templateList = mutableListOf<AepUITemplate>()
        for(proposition in propositionList) {
            val item = proposition.items[0]
            val contentCardSchemaData = item.contentCardSchemaData
            val contentMap = contentCardSchemaData?.content as HashMap<String, Object>

            templateList.add(SmallImageTemplate(contentMap))
        }
        return templateList
    }

    override suspend fun refreshContent() {
        TODO("Not yet implemented")
    }
}