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

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adobe.marketing.mobile.MessagingEdgeEventType
import com.adobe.marketing.mobile.messaging.Proposition
import com.adobe.marketing.mobile.messaging.SchemaType
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject


class CodeBasedCardAdapter(propositions: MutableList<Proposition>) :
    RecyclerView.Adapter<CodeBasedCardAdapter.ViewHolder>() {
    private var propositions = mutableListOf<Proposition>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.card_codebaseditem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val proposition = propositions[position]
        for (item in proposition.items) {
            var mimeType = ""
            if (item.schema == SchemaType.JSON_CONTENT) {
                mimeType = "application/json"
            } else if (item.schema == SchemaType.HTML_CONTENT) {
                mimeType = "text/html"
            }
            // show code based experiences with html content in a webview
            if (mimeType == "text/html") {
                val contentString = item.htmlContent
                holder.webView.loadData(
                        contentString,
                        mimeType,
                        StandardCharsets.UTF_8.toString())
                item.track(MessagingEdgeEventType.DISPLAY)
                holder.webView.setOnTouchListener(object: View.OnTouchListener {
                    override fun onTouch(p0: View?, event: MotionEvent?): Boolean {
                        if (event?.action == MotionEvent.ACTION_UP) {
                            item.track(MessagingEdgeEventType.INTERACT)
                            return true
                        }
                        return false
                    }
                })
            } else if (mimeType == "application/json") {
                var contentString = item.jsonArrayList
                if (contentString != null) { // we have a json array
                    val jsonArray = JSONArray(contentString)
                    holder.textView.text = jsonArray.toString(5)
                } else {
                    val json = JSONObject(item.jsonContentMap)
                    holder.textView.text = json.toString(5)
                }
                item.track(MessagingEdgeEventType.DISPLAY)
                holder.textView.setOnClickListener {
                    item.track(MessagingEdgeEventType.INTERACT)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return propositions.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var webView: WebView
        var textView: TextView

        init {
            webView = itemView.findViewById(R.id.codeBasedHtmlContent)
            textView = itemView.findViewById(R.id.codeBasedTextContent)
        }
    }

    init {
        this.propositions = propositions
    }
}