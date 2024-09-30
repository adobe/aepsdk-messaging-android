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
import com.adobe.marketing.mobile.util.DataReader

class SingleFeedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_singlefeed)

        val feedActivityItemImage: ImageView = findViewById(R.id.feedActivityImage)
        val feedActivityItemTitle: TextView = findViewById(R.id.feedActivityTitle)
        val feedActivityBody: TextView = findViewById(R.id.feedActivityBody)
        val feedActivityButton: Button = findViewById(R.id.feedActivityActionButton)

        // retrieve feed content from the intent
        val contentMap = intent.getSerializableExtra("content") as HashMap<String, Object>

        // show single feed item
        feedActivityItemTitle.text = DataReader.getStringMap(contentMap, "title")["content"] ?: "defaultTitle"
        feedActivityItemImage.setImageBitmap(
            DataReader.getStringMap(contentMap, "image").get("url")?.let {
                ImageDownloader.getImage(it)
            })
        feedActivityItemImage.refreshDrawableState()
        feedActivityBody.text = DataReader.getStringMap(contentMap, "body")["content"] ?: "defaultBody"
        val buttonList = DataReader.optTypedListOfMap(Object::class.java, contentMap, "buttons", emptyList())
        if (buttonList.isNotEmpty()) {
            feedActivityButton.text =
                DataReader.optStringMap(buttonList[0], "text", emptyMap())["content"]
                    ?: "defaultButton"
            feedActivityButton.setOnClickListener {
                DataReader.getString(buttonList[0], "actionUrl")?.let { it1 ->
                    ServiceProvider.getInstance().uriService.openUri(it1)
                }
            }
        }
    }
}