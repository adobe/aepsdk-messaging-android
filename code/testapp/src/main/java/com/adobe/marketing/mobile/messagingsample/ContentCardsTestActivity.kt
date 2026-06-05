/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messagingsample

import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.ImageOnlyUI
import com.adobe.marketing.mobile.aepcomposeui.LargeImageUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.components.ImageOnlyCard
import com.adobe.marketing.mobile.aepcomposeui.components.LargeImageCard
import com.adobe.marketing.mobile.aepcomposeui.components.SmallImageCard
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepColumnStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepRowStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.ImageOnlyUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.LargeImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.SmallImageUIStyle
import com.adobe.marketing.mobile.messaging.Surface

class ContentCardsTestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                ContentCardsTestScreen()
            }
        }
    }
}

@Composable
private fun ContentCardsTestScreen(viewModel: ContentCardsTestViewModel = viewModel()) {
    val context = LocalContext.current
    var surfacesRaw by remember { mutableStateOf("card/ms, card/ms2") }
    var triggerAction by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val itemsStyle = rememberContentCardItemsStyle()

    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.content_cards_surfaces_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = surfacesRaw,
            onValueChange = { surfacesRaw = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            label = { Text(stringResource(R.string.content_cards_surfaces_label)) },
            placeholder = { Text(stringResource(R.string.content_cards_surfaces_hint)) },
            minLines = 2
        )
        Button(
            onClick = {
                val entries = parseSurfaceEntries(surfacesRaw)
                if (entries == null) {
                    Toast.makeText(context, "Enter at least one surface path", Toast.LENGTH_SHORT)
                        .show()
                    return@Button
                }
                Messaging.updatePropositionsForSurfaces(entries.map { it.second })
                status =
                    "updatePropositionsForSurfaces dispatched (${entries.size} surface(s)). Tap Get content to refresh the card UI."
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.content_cards_refresh))
        }
        Text(
            text = stringResource(R.string.content_cards_trigger_action_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
        OutlinedTextField(
            value = triggerAction,
            onValueChange = { triggerAction = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            label = { Text(stringResource(R.string.content_cards_trigger_action_label)) },
            placeholder = { Text(stringResource(R.string.content_cards_trigger_action_hint)) },
            singleLine = true
        )
        Button(
            onClick = {
                val action = triggerAction.trim()
                if (action.isEmpty()) {
                    Toast.makeText(
                            context,
                            "Enter a trigger action name",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                    return@Button
                }
                MobileCore.trackAction(action, null)
                status = "MobileCore.trackAction(\"$action\", null)"
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.content_cards_send_action))
        }
        Button(
            onClick = {
                val entries = parseSurfaceEntries(surfacesRaw)
                if (entries == null) {
                    Toast.makeText(context, "Enter at least one surface path", Toast.LENGTH_SHORT)
                        .show()
                    return@Button
                }
                viewModel.setSurfaceEntries(entries)
                val time =
                    DateFormat.getTimeFormat(context).format(System.currentTimeMillis())
                status = "Card UI loaded from cache ($time)"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.content_cards_get_content))
        }
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = stringResource(R.string.content_cards_results_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (sections.isEmpty()) {
            Text(
                text =
                    "Tap Get content to load ContentCardUIProvider output. Refresh only fetches propositions from Edge (no UI update).",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            sections.forEach { section ->
                SurfaceCardsSection(
                    section = section,
                    itemsStyle = itemsStyle,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun rememberContentCardItemsStyle(): AepUIStyle {
    return remember {
        val cardStyle = AepCardStyle(modifier = Modifier.padding(8.dp))
        val smallImageCardStyle =
            SmallImageUIStyle.Builder()
                .cardStyle(cardStyle)
                .rootRowStyle(
                    AepRowStyle(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    )
                )
                .build()
        val largeImageCardStyle =
            LargeImageUIStyle.Builder()
                .cardStyle(cardStyle)
                .rootColumnStyle(AepColumnStyle(modifier = Modifier.fillMaxWidth().padding(8.dp)))
                .build()
        val imageOnlyCardStyle =
            ImageOnlyUIStyle.Builder()
                .cardStyle(cardStyle)
                .imageStyle(AepImageStyle(modifier = Modifier.fillMaxWidth()))
                .build()
        AepUIStyle(
            smallImageUIStyle = smallImageCardStyle,
            largeImageUIStyle = largeImageCardStyle,
            imageOnlyUIStyle = imageOnlyCardStyle,
        )
    }
}

@Composable
private fun SurfaceCardsSection(
    section: SurfaceCardSection,
    itemsStyle: AepUIStyle,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "--------------${section.pathToken}--------------",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        val flow = remember(section.provider) { section.provider.getContentCardUIFlow() }
        val result by flow.collectAsStateWithLifecycle(initialValue = Result.success(emptyList()))
        result.fold(
            onSuccess = { list ->
                if (list.isEmpty()) {
                    Text(
                        text = "(no content cards)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        list.forEach { aepUI ->
                            ContentCardRow(
                                aepUI = aepUI,
                                itemsStyle = itemsStyle,
                                observer = section.observer
                            )
                        }
                    }
                }
            },
            onFailure = { e ->
                Text(
                    text = e.message ?: "Error loading content cards",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        )
    }
}

@Composable
private fun ContentCardRow(
    aepUI: AepUI<*, *>,
    itemsStyle: AepUIStyle,
    observer: AepUIEventObserver?,
) {
    val state = aepUI.getState()
    if (state.dismissed) {
        return
    }
    when (aepUI) {
        is SmallImageUI ->
            SmallImageCard(
                ui = aepUI,
                style = itemsStyle.smallImageUIStyle,
                observer = observer
            )
        is LargeImageUI ->
            LargeImageCard(
                ui = aepUI,
                style = itemsStyle.largeImageUIStyle,
                observer = observer
            )
        is ImageOnlyUI ->
            ImageOnlyCard(
                ui = aepUI,
                style = itemsStyle.imageOnlyUIStyle,
                observer = observer
            )
    }
}

private fun parseSurfaceEntries(raw: String): List<Pair<String, Surface>>? {
    val entries =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { token ->
            token to Surface(token)
        }
    return if (entries.isEmpty()) null else entries
}
