/*
  Copyright 2026 Adobe. All rights reserved.
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
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.consent.Consent

/**
 * Manual settings screen for testing the consent-driven push token re-sync flow.
 *
 * Three controls:
 *   1. Collect Consent radio (y / n / p) + Update button — dispatches
 *      `Consent.update({"consents": {"collect": {"val": <value>}}})`. The `p` option
 *      is here so testers can verify the load-bearing `y → p → y` invariant: the
 *      pending event must not trigger a re-sync.
 *   2. Optimize Push Sync switch — flips `messaging.optimizePushSync` via
 *      `MobileCore.updateConfiguration`. Lets testers exercise both the
 *      same-token-suppression and forced-resync paths.
 *   3. Push Identifier text field + button — `MobileCore.setPushIdentifier(...)` so
 *      testers can simulate the host-app push registration step without going through
 *      Firebase. Empty input is rejected.
 *
 * The "Last action" line at the bottom echoes whatever was just dispatched so the
 * tester can confirm the click landed.
 */
class MessagingSettingsActivity : ComponentActivity() {

    private lateinit var collectConsentGroup: RadioGroup
    private lateinit var optimizeSwitch: Switch
    private lateinit var pushTokenField: EditText
    private lateinit var lastActionLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messaging_settings)

        collectConsentGroup = findViewById(R.id.collect_consent_group)
        optimizeSwitch = findViewById(R.id.switch_optimize_push_sync)
        pushTokenField = findViewById(R.id.edit_push_token)
        lastActionLabel = findViewById(R.id.last_action)

        // Default radio selection so Update Consent always has an effective value
        collectConsentGroup.check(R.id.collect_yes)

        findViewById<Button>(R.id.btn_update_consent).setOnClickListener {
            val value = when (collectConsentGroup.checkedRadioButtonId) {
                R.id.collect_yes -> "y"
                R.id.collect_no -> "n"
                R.id.collect_pending -> "p"
                else -> {
                    showStatus("Select a collect consent value first.")
                    return@setOnClickListener
                }
            }
            updateCollectConsent(value)
        }

        optimizeSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateOptimizePushSync(isChecked)
        }

        findViewById<Button>(R.id.btn_set_push_identifier).setOnClickListener {
            val token = pushTokenField.text.toString().trim()
            if (token.isEmpty()) {
                showStatus("Enter a push token first.")
                return@setOnClickListener
            }
            setPushIdentifier(token)
        }
    }

    private fun updateCollectConsent(value: String) {
        val consents = mapOf(
            "consents" to mapOf(
                "collect" to mapOf("val" to value)
            )
        )
        Consent.update(consents)
        showStatus("Consent.update(collect = \"$value\")")
    }

    private fun updateOptimizePushSync(enabled: Boolean) {
        MobileCore.updateConfiguration(mapOf("messaging.optimizePushSync" to enabled))
        showStatus("messaging.optimizePushSync = $enabled")
    }

    private fun setPushIdentifier(token: String) {
        MobileCore.setPushIdentifier(token)
        showStatus("MobileCore.setPushIdentifier(\"$token\")")
    }

    private fun showStatus(message: String) {
        lastActionLabel.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
