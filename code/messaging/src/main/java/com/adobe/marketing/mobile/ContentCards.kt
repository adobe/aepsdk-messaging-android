package com.adobe.marketing.mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adobe.marketing.mobile.aepcomposeui.aepui.AepComposableViewModelFactory
import com.adobe.marketing.mobile.aepcomposeui.aepui.AepList
import com.adobe.marketing.mobile.aepcomposeui.aepui.AepListViewModel
import com.adobe.marketing.mobile.aepcomposeui.aepui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.aepui.style.SmallImageUIStyle

/**
 * Public composable exposing the ContentCards UI, utilizing the AEP List component
 */
@Composable
fun ContentCards(
    viewModel: AepListViewModel,
    contentCardStyle: ContentCardStyle = ContentCardStyle(),
    container: @Composable (@Composable () -> Unit) -> Unit = { content ->
        // Default to a Column if no container is provided
        LazyColumn {
            item {
                content()
            }
        }
    },
    contentCardCallback: ContentCardCallback? = null
) {
    AepList(
        viewModel = viewModel,
        aepUiEventObserver = MessagingUiEventObserver(contentCardCallback),
        aepUiStyle = AepUIStyle(
            smallImageUiStyle = contentCardStyle.smallImageAepUiStyle,
        ),
        container = container,
    )
}

// Wrapper class that only accepts style for UIs supported by ContentCards
class ContentCardStyle(
    val smallImageAepUiStyle: SmallImageUIStyle = SmallImageUIStyle(),
)

@Composable
fun ContentCardsViewModel(surface: String) : AepListViewModel {
    return viewModel(
        factory = AepComposableViewModelFactory(ContentCardContentProvider(surface)),
        key = surface
    )
}