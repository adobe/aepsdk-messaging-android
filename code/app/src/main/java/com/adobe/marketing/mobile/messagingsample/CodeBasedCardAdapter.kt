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
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adobe.marketing.mobile.messaging.MessagingProposition
import com.adobe.marketing.mobile.services.ServiceProvider
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class CodeBasedCardAdapter(messagingPropositions: MutableList<MessagingProposition>) :
    RecyclerView.Adapter<CodeBasedCardAdapter.ViewHolder>() {
    private var messagingPropositions = mutableListOf<MessagingProposition>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.card_codebaseditem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val proposition = messagingPropositions[position]
        for (item in proposition.items) {
            val mimeType =
                if (item.schema.equals("https://ns.adobe.com/personalization/json-content-item")) "application/json" else "text/html"
            val contentString = item.content
            // show code based experiences with html content in a webview
            if (mimeType == "text/html") {
                ServiceProvider.getInstance().uiService.run {
                    holder.webView.loadData(
                        contentString,
                        mimeType,
                        StandardCharsets.UTF_8.toString()
                    )
                }
            } else { // show code based experiences with text or json content in a text view
                if (mimeType == "application/json") {
                    if (contentString.startsWith("[")) { // we have a json array
                        val jsonArray = JSONArray(contentString)
                        holder.textView.text = jsonArray.toString(5)
                    } else {
                        val json = JSONObject(contentString)
                        holder.textView.text = json.toString(5)
                    }
                } else {
                    holder.textView.text = contentString
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return messagingPropositions.size
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
        this.messagingPropositions = messagingPropositions
    }
}