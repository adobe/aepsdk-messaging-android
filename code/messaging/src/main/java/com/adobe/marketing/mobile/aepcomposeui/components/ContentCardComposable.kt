package com.adobe.marketing.mobile.aepcomposeui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepRowStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepTextStyle
import com.adobe.marketing.mobile.aepcomposeui.style.SmallImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.viewmodel.AepContentCardViewModel
import com.adobe.marketing.mobile.aepcomposeui.viewmodel.factory.AepContentCardViewModelFactory
import com.adobe.marketing.mobile.messaging.ContentCardEventObserver
import com.adobe.marketing.mobile.messaging.ContentCardMapper
import com.adobe.marketing.mobile.messaging.ContentCardUIEventListener
import com.adobe.marketing.mobile.messaging.ContentCardUIProvider
import com.adobe.marketing.mobile.messaging.Surface

@Composable
fun ContentCardComposable(
    surface: Surface,
    viewModel: AepContentCardViewModel,
    comparator: Comparator<AepUI<*, *>>?,
    callback: ContentCardUIEventListener?,
) {

    // Initialize the ContentCardUIProvider
    val contentCardUIProvider = ContentCardUIProvider(surface)


    // Collect the state from ViewModel
    val aepUiList by viewModel.aepUIList.collectAsStateWithLifecycle()

    // Get the ContentCardSchemaData for the AepUI list if needed
    val contentCardSchemaDataList = aepUiList.map {
        when (it) {
            is SmallImageUI ->
                ContentCardMapper.instance.getContentCardSchemaData(it.getTemplate().id)

            else -> null
        }
    }

    // Reorder the AepUI list based on the ContentCardSchemaData fields if needed
    val reorderedAepUIList = aepUiList.sortedWith(compareByDescending {
        val rank =
            contentCardSchemaDataList[aepUiList.indexOf(it)]?.meta?.get("priority") as String?
                ?: "0"
        rank.toInt()
    })

    val smallImageCardStyleRow = SmallImageUIStyle.Builder()
        .cardStyle(AepCardStyle(modifier = Modifier
            .width(400.dp)
            .height(200.dp)))
        .rootRowStyle(
            AepRowStyle(
                modifier = Modifier.fillMaxSize()
            )
        )
        .titleAepTextStyle(AepTextStyle(textStyle = TextStyle(Color.Green)))
        .build()

    // Create row with composables from AepUI instances
    LazyRow {
        items(reorderedAepUIList) { aepUI ->
            when (aepUI) {
                is SmallImageUI -> {
                    //TODO check if the card is dismissed to return or not
                    val state = aepUI.getState()
                    if (!state.dismissed) {
                        SmallImageCard(
                            ui = aepUI,
                            style = smallImageCardStyleRow,
                            observer = ContentCardEventObserver(callback)
                        )
                    }
                }
            }
        }
    }
}