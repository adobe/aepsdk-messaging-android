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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.messaging.Proposition
import com.adobe.marketing.mobile.messaging.Surface
import com.adobe.marketing.mobile.messagingsample.databinding.ActivityCodebasedBinding

class CodeBasedExperienceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCodebasedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCodebasedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // create list of code based surface paths
        var propositions = mutableListOf<Proposition>()
        val surfaces = mutableListOf<Surface>()
        surfaces.add(Surface("cbe-path1"))
        surfaces.add(Surface("cbe-path2"))

        // fetch code based experiences
        Messaging.updatePropositionsForSurfaces(surfaces)
        // retrieve any cached code based experiences
        Messaging.getPropositionsForSurfaces(surfaces) {
            println("getPropositionsForSurfaces callback contained ${it.entries.size} entry/entries")
            for (entry in it.entries) {
                println("Adding ${entry.value.size} proposition(s) from surface ${entry.key.uri}")
                propositions.addAll(entry.value)
            }

            // show code based experiences
            val codeBasedRecyclerView = findViewById<RecyclerView>(R.id.codeBasedContentView)
            val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            val codeBasedCardAdapter = CodeBasedCardAdapter(propositions)
            runOnUiThread {
                codeBasedRecyclerView.layoutManager = linearLayoutManager
                codeBasedRecyclerView.adapter = codeBasedCardAdapter
            }
        }
    }
}