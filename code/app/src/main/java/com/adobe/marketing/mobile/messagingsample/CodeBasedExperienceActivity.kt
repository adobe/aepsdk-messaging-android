/*
  Copyright 2023 Adobe. All rights reserved.
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
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.Proposition
import com.adobe.marketing.mobile.Surface
import java.nio.charset.StandardCharsets

class CodeBasedExperienceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_codebased)

        // retrieve any cached code based experiences
        var propositions = mutableListOf<Proposition>()
        val surfaces = mutableListOf<Surface>()
        val surface = Surface("codeBasedPath")
        surfaces.add(surface)
        Messaging.getPropositionsForSurfaces(surfaces) {
            println("getPropositionsForSurfaces callback contained ${it.entries.size} entry/entries for surface ${surface.uri}")
            for (entry in it.entries) {
                propositions = entry.value
            }

            // show code based experiences
            val codeBasedExperienceWebView: WebView = findViewById(R.id.codeBasedExperienceWebView)
            val htmlContentString = propositions[0].items[0].content
            runOnUiThread {
                codeBasedExperienceWebView.loadData(
                    htmlContentString,
                    "text/html",
                    StandardCharsets.UTF_8.toString()
                )
            }
        }
    }
}