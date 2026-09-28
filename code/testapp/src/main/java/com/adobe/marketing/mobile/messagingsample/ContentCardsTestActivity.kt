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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
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
import com.adobe.marketing.mobile.messaging.Proposition
import com.adobe.marketing.mobile.messaging.Surface

private const val DEFAULT_SURFACE_1 = "card/ms"
private const val DEFAULT_SURFACE_2 = "largeImageCards"
private const val DEFAULT_TRACK_ACTION = "smoke_test"

private const val PREFS_NAME = "messaging_test_prefs"
private const val PREF_OFFLINE_AVAILABLE = "contentCardOfflineAvailable"
private const val CONFIG_KEY_OFFLINE_AVAILABLE = "messaging.contentCardOfflineAvailable"

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
    var surface1Path by remember { mutableStateOf(DEFAULT_SURFACE_1) }
    var surface2Path by remember { mutableStateOf(DEFAULT_SURFACE_2) }
    var offlineAvailable by remember { mutableStateOf(readPersistedOfflineFlag(context)) }
    var trackActionName by remember { mutableStateOf(DEFAULT_TRACK_ACTION) }

    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val loadStatus by viewModel.loadStatus.collectAsStateWithLifecycle()
    val itemsStyle = rememberContentCardItemsStyle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Settings shortcut ─────────────────────────────────────────────────────────
        Button(
            onClick = { context.startActivity(Intent(context, MessagingSettingsActivity::class.java)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Text("Settings & Testing")
        }

        // ── Surface path inputs ───────────────────────────────────────────────────────
        Text(
            text = "Surface Paths",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = surface1Path,
            onValueChange = { surface1Path = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            label = { Text("S1 surface path") },
            singleLine = true
        )
        OutlinedTextField(
            value = surface2Path,
            onValueChange = { surface2Path = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            label = { Text("S2 surface path") },
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        // ── Offline flag toggle ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Offline Content Cards",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = CONFIG_KEY_OFFLINE_AVAILABLE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = offlineAvailable,
                onCheckedChange = { enabled ->
                    offlineAvailable = enabled
                    persistOfflineFlag(context, enabled)
                    MobileCore.updateConfiguration(mapOf(CONFIG_KEY_OFFLINE_AVAILABLE to enabled))
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // ── Update Propositions (network fetch only; UI is not refreshed) ─────────────
        Text(
            text = "Update Propositions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { updateSurface(context, surface1Path, "S1", viewModel) },
                modifier = Modifier.weight(1f)
            ) { Text("Update S1") }

            Button(
                onClick = { updateSurface(context, surface2Path, "S2", viewModel) },
                modifier = Modifier.weight(1f)
            ) { Text("Update S2") }
        }

        Spacer(Modifier.height(8.dp))

        // ── Get Propositions (reads cache and reflects cards / errors on the UI) ──────
        Text(
            text = "Get Propositions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { getSurface(context, surface1Path, "S1", viewModel) },
                modifier = Modifier.weight(1f)
            ) { Text("Get S1") }

            Button(
                onClick = { getSurface(context, surface2Path, "S2", viewModel) },
                modifier = Modifier.weight(1f)
            ) { Text("Get S2") }
        }

        Spacer(Modifier.height(8.dp))

        // ── Clear Propositions (clear API only) ───────────────────────────────────────
        Button(
            onClick = {
                Messaging.clearCachedPropositions()
                viewModel.setLoadStatus("clearCachedPropositions() called.")
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Propositions")
        }

        Spacer(Modifier.height(8.dp))

        // ── Track Action (dispatches an event that can qualify trigger-gated cards) ────
        Text(
            text = "Track Action",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = trackActionName,
            onValueChange = { trackActionName = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            label = { Text("Action name") },
            singleLine = true
        )
        Button(
            onClick = {
                val action = trackActionName.trim()
                if (action.isEmpty()) {
                    Toast.makeText(context, "Action name is empty", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                MobileCore.trackAction(action, null)
                viewModel.setLoadStatus("trackAction(\"$action\") sent. Tap Get to refresh cards.")
            },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        ) {
            Text("Send Action")
        }

        // ── Status label ──────────────────────────────────────────────────────────────
        if (loadStatus.isNotEmpty()) {
            Text(
                text = loadStatus,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // ── Card results ──────────────────────────────────────────────────────────────
        Text(
            text = "Content Cards",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        sections.forEach { section ->
            SurfaceCardsSection(
                section = section,
                itemsStyle = itemsStyle,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/** Fires an update (network fetch) for a single surface. Does not touch the card UI. */
private fun updateSurface(
    context: Context,
    rawPath: String,
    label: String,
    viewModel: ContentCardsTestViewModel,
) {
    val path = rawPath.trim()
    if (path.isEmpty()) {
        Toast.makeText(context, "$label path is empty", Toast.LENGTH_SHORT).show()
        return
    }
    viewModel.setLoadStatus("Updating $label…")
    Messaging.updatePropositionsForSurfaces(listOf(Surface(path))) { success ->
        viewModel.setLoadStatus(
            if (success == true) "$label update completed." else "$label update failed or timed out."
        )
    }
}

/** Reads a single surface from cache and reflects the cards (or error) on the UI. */
private fun getSurface(
    context: Context,
    rawPath: String,
    label: String,
    viewModel: ContentCardsTestViewModel,
) {
    val path = rawPath.trim()
    if (path.isEmpty()) {
        Toast.makeText(context, "$label path is empty", Toast.LENGTH_SHORT).show()
        return
    }
    val surface = Surface(path)
    viewModel.setLoadStatus("Loading $label…")
    Messaging.getPropositionsForSurfaces(
        listOf(surface),
        object : AdobeCallbackWithError<Map<Surface, List<Proposition>>> {
            override fun call(propositionsMap: Map<Surface, List<Proposition>>?) {
                val count = propositionsMap?.values?.sumOf { it.size } ?: 0
                viewModel.setLoadStatus("$label: $count card(s) loaded.")
                if (count > 0) {
                    viewModel.upsertSurfaceEntry(path, surface)
                } else {
                    viewModel.removeSurfaceEntry(path)
                }
            }

            override fun fail(error: AdobeError?) {
                viewModel.setLoadStatus("$label get failed: ${error?.errorName ?: "unknown"}")
                viewModel.removeSurfaceEntry(path)
            }
        }
    )
}

private fun readPersistedOfflineFlag(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(PREF_OFFLINE_AVAILABLE, false)

private fun persistOfflineFlag(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(PREF_OFFLINE_AVAILABLE, enabled)
        .apply()
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
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
            SmallImageCard(ui = aepUI, style = itemsStyle.smallImageUIStyle, observer = observer)
        is LargeImageUI ->
            LargeImageCard(ui = aepUI, style = itemsStyle.largeImageUIStyle, observer = observer)
        is ImageOnlyUI ->
            ImageOnlyCard(ui = aepUI, style = itemsStyle.imageOnlyUIStyle, observer = observer)
    }
}
