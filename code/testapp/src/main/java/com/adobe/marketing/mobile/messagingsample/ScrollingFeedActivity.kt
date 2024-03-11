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
import com.adobe.marketing.mobile.messagingsample.databinding.ActivityScrollingBinding

class ScrollingFeedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScrollingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScrollingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // retrieve any cached feed propositions
        var propositions = mutableListOf<Proposition>()
        val surfaces = mutableListOf<Surface>()
        val surface = Surface("feeds/apifeed")
        surfaces.add(surface)
        Messaging.updatePropositionsForSurfaces(surfaces)
        Messaging.getPropositionsForSurfaces(surfaces) {
            println("getPropositionsForSurfaces callback contained ${it.entries.size} entry/entries for surface ${surface.uri}")
            for (entry in it.entries) {
                propositions = entry.value
            }

            // show feed items
            val feedInboxRecyclerView = findViewById<RecyclerView>(R.id.feedInboxView)
            val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            val feedCardAdapter = FeedCardAdapter(propositions)
            runOnUiThread {
                feedInboxRecyclerView.layoutManager = linearLayoutManager
                feedInboxRecyclerView.adapter = feedCardAdapter
            }
        }
    }
}