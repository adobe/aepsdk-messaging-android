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
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.adobe.marketing.mobile.services.ServiceProvider
import org.json.JSONObject

class SingleFeedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_singlefeed)

        val feedActivityItemTitle: TextView
        val feedActivityItemImage: ImageView
        val feedActivityBody: TextView
        val feedActivityButton: Button

        feedActivityItemImage = findViewById(R.id.feedActivityImage)
        feedActivityItemTitle = findViewById(R.id.feedActivityTitle)
        feedActivityBody = findViewById(R.id.feedActivityBody)
        feedActivityButton = findViewById(R.id.feedActivityActionButton)

        // retrieve feed content from the intent
        var content = intent.extras?.getString("content")

        // show single feed item
        val jsonContent = JSONObject(content)
        feedActivityItemTitle.text = jsonContent.getString("title")
        feedActivityItemImage.setImageBitmap(ImageDownloader.getImage(jsonContent.getString("imageUrl")))
        feedActivityItemImage.refreshDrawableState()
        feedActivityBody.text = jsonContent.getString("body")
        feedActivityButton.text = jsonContent.getString("actionTitle")
        feedActivityButton.setOnClickListener {
            ServiceProvider.getInstance().uiService.showUrl(jsonContent.getString("actionUrl"))
        }
    }
}