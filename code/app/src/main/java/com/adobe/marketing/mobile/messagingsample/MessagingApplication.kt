/*
 Copyright 2021 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.messagingsample

import android.app.Application
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.optimize.Optimize
import com.google.firebase.iid.FirebaseInstanceId
import org.json.JSONObject

class MessagingApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        Messaging.registerExtension()
        Optimize.registerExtension()
        Identity.registerExtension()
        UserProfile.registerExtension()
        Lifecycle.registerExtension()
        Signal.registerExtension()
        Edge.registerExtension();

        MobileCore.start {
            // Necessary property id which has the edge configuration id needed by aep sdk
            MobileCore.configureWithAppID("3149c49c3910/cf7779260cdd/launch-be72758aa82a-development")
            MobileCore.lifecycleStart(null)
            // update config to use cjmstage for int integration
            val cjmStageConfig = HashMap<String, Any>()
            cjmStageConfig["edge.environment"] = "int"
            cjmStageConfig["experienceCloud.org"] = "745F37C35E4B776E0A49421B@AdobeOrg"
            cjmStageConfig["edge.configId"] = "d9457e9f-cacc-4280-88f2-6c846e3f9531"
            //cjmStageConfig["edge.configId"] = "1f0eb783-2464-4bdd-951d-7f8afbf527f5:dev"
            cjmStageConfig["messaging.eventDataset"] = "610ae80b3cbbc718dab06208"
            MobileCore.updateConfiguration(cjmStageConfig)
        }


        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener{ task ->
            if(task.isSuccessful) {
                val token = task.result?.token ?: ""
                print("MessagingApplication Firebase token :: $token")
                // Syncing the push token with experience platform
                MobileCore.setPushIdentifier(token)
            }
        }
    }

    private fun dispatchEdgeResponseEvent() {
        // simulate edge response event containing offers
        val eventData = HashMap<String, Any>()
        eventData.put("type", "personalization:decisions")
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B")
        val payload = JSONObject("{\n" +
                "  \"activity\": {\n" +
                "    \"id\": \"906E3A095DC834230A495FD6@AdobeOrg\",\n" +
                "    \"etag\": \"9\"\n" +
                "  },\n" +
                "  \"id\": \"57d8f990-734a-4d7b-a7e7-739b03753226\",\n" +
                "  \"scope\": \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTQwOTAyMzVlNmI2NzU3YSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjE0MGExNzZmMjcyZWU2NTEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"id\": \"xcore:personalized-offer:140f858533497db9\",\n" +
                "      \"data\": {\n" +
                "        \"characteristics\": {\n" +
                "          \"inappmessageExecutionId\": \"dummy_message_execution_id_7a237b33-2648-4903-82d1-efa1aac7c60d-all-visitors-everytime\"\n" +
                "        },\n" +
                "        \"id\": \"xcore:personalized-offer:140f858533497db9\",\n" +
                "        \"content\": \"{\\\"version\\\":1,\\\"rules\\\":[{\\\"condition\\\":{\\\"type\\\":\\\"group\\\",\\\"definition\\\":{\\\"conditions\\\":[{\\\"definition\\\":{\\\"key\\\":\\\"foo\\\",\\\"matcher\\\":\\\"eq\\\",\\\"values\\\":[\\\"bar\\\"]},\\\"type\\\":\\\"matcher\\\"}],\\\"logic\\\":\\\"and\\\"}},\\\"consequences\\\":[{\\\"id\\\":\\\"e56a8dbb-c5a4-4219-9563-0496e00b4083\\\",\\\"type\\\":\\\"cjmiam\\\",\\\"detail\\\":{\\\"mobileParameters\\\":{\\\"verticalAlign\\\":\\\"center\\\",\\\"horizontalInset\\\":0,\\\"dismissAnimation\\\":\\\"bottom\\\",\\\"uiTakeover\\\":true,\\\"horizontalAlign\\\":\\\"center\\\",\\\"verticalInset\\\":0,\\\"displayAnimation\\\":\\\"bottom\\\",\\\"width\\\":100,\\\"height\\\":100,\\\"gestures\\\":{}},\\\"html\\\":\\\"<html>\\\\n<head>\\\\n\\\\t<style>\\\\n\\\\t\\\\thtml,\\\\n\\\\t\\\\tbody {\\\\n\\\\t\\\\t\\\\tmargin: 0;\\\\n\\\\t\\\\t\\\\tpadding: 0;\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\twidth: 100%;\\\\n\\\\t\\\\t\\\\theight: 100%;\\\\n\\\\t\\\\t\\\\tfont-family: adobe-clean, \\\\\\\"Source Sans Pro\\\\\\\", -apple-system, BlinkMacSystemFont, \\\\\\\"Segoe UI\\\\\\\", Roboto, sans-serif;\\\\n\\\\t\\\\t}\\\\n\\\\n    h3 {\\\\n\\\\t\\\\t\\\\tmargin: .1rem auto;\\\\n\\\\t\\\\t}\\\\n\\\\t\\\\tp {\\\\n\\\\t\\\\t\\\\tmargin: 0;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.body {\\\\n\\\\t\\\\t\\\\tdisplay: flex;\\\\n\\\\t\\\\t\\\\tflex-direction: column;\\\\n\\\\t\\\\t\\\\tbackground-color: #FFF;\\\\n\\\\t\\\\t\\\\tborder-radius: 5px;\\\\n\\\\t\\\\t\\\\tcolor: #333333;\\\\n\\\\t\\\\t\\\\twidth: 100vw;\\\\n\\\\t\\\\t\\\\theight: 100vh;\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\talign-items: center;\\\\n\\\\t\\\\t\\\\tbackground-size: 'cover';\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.content {\\\\n\\\\t\\\\t\\\\twidth: 100%;\\\\n\\\\t\\\\t\\\\theight: 100%;\\\\n\\\\t\\\\t\\\\tdisplay: flex;\\\\n\\\\t\\\\t\\\\tjustify-content: center;\\\\n\\\\t\\\\t\\\\tflex-direction: column;\\\\n\\\\t\\\\t\\\\tposition: relative;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\ta {\\\\n\\\\t\\\\t\\\\ttext-decoration: none;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.image {\\\\n\\\\t\\\\t  height: 1rem;\\\\n\\\\t\\\\t  flex-grow: 4;\\\\n\\\\t\\\\t  flex-shrink: 1;\\\\n\\\\t\\\\t  display: flex;\\\\n\\\\t\\\\t  justify-content: center;\\\\n\\\\t\\\\t  width: 90%;\\\\n      flex-direction: column;\\\\n      align-items: center;\\\\n\\\\t\\\\t}\\\\n    .image img {\\\\n      max-height: 100%;\\\\n      max-width: 100%;\\\\n    }\\\\n\\\\n\\\\t\\\\t.text {\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\tline-height: 20px;\\\\n\\\\t\\\\t\\\\tfont-size: 14px;\\\\n\\\\t\\\\t\\\\tcolor: #333333;\\\\n\\\\t\\\\t\\\\tpadding: 0 25px;\\\\n\\\\t\\\\t\\\\tline-height: 1.25rem;\\\\n\\\\t\\\\t\\\\tfont-size: 0.875rem;\\\\n\\\\t\\\\t}\\\\n\\\\t\\\\t.title {\\\\n\\\\t\\\\t\\\\tline-height: 1.3125rem;\\\\n\\\\t\\\\t\\\\tfont-size: 1.025rem;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.buttons {\\\\n\\\\t\\\\t\\\\twidth: 100%;\\\\n\\\\t\\\\t\\\\tdisplay: flex;\\\\n\\\\t\\\\t\\\\tflex-direction: column;\\\\n\\\\t\\\\t\\\\tfont-size: 1rem;\\\\n\\\\t\\\\t\\\\tline-height: 1.3rem;\\\\n\\\\t\\\\t\\\\ttext-decoration: none;\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\tbox-sizing: border-box;\\\\n\\\\t\\\\t\\\\tpadding: .8rem;\\\\n\\\\t\\\\t\\\\tpadding-top: .4rem;\\\\n\\\\t\\\\t\\\\tgap: 0.3125rem;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.button {\\\\n\\\\t\\\\t\\\\tflex-grow: 1;\\\\n\\\\t\\\\t\\\\tbackground-color: #1473E6;\\\\n\\\\t\\\\t\\\\tcolor: #FFFFFF;\\\\n\\\\t\\\\t\\\\tborder-radius: .25rem;\\\\n\\\\t\\\\t\\\\tcursor: pointer;\\\\n\\\\t\\\\t\\\\tpadding: .3rem;\\\\n\\\\t\\\\t\\\\tgap: .5rem;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.btnClose {\\\\n\\\\t\\\\t\\\\tcolor: #000000;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.closeBtn {\\\\n\\\\t\\\\t\\\\talign-self: flex-end;\\\\n\\\\t\\\\t\\\\twidth: 1.8rem;\\\\n\\\\t\\\\t\\\\theight: 1.8rem;\\\\n\\\\t\\\\t\\\\tmargin-top: 1rem;\\\\n\\\\t\\\\t\\\\tmargin-right: .3rem;\\\\n\\\\t\\\\t}\\\\n\\\\t</style>\\\\n\\\\t<style type=\\\\\\\"text/css\\\\\\\" id=\\\\\\\"editor-styles\\\\\\\">\\\\n[data-uuid=\\\\\\\"e69adbda-8cfc-4c51-bcab-f8b2fd314d8f\\\\\\\"]  {\\\\n  flex-direction: row !important;\\\\n}\\\\n</style>\\\\n</head>\\\\n\\\\n<body>\\\\n\\\\t<div class=\\\\\\\"body\\\\\\\">\\\\n    <div class=\\\\\\\"closeBtn\\\\\\\" data-btn-style=\\\\\\\"plain\\\\\\\" data-uuid=\\\\\\\"fbc580c2-94c1-43c8-a46b-f66bb8ca8554\\\\\\\">\\\\n  <a class=\\\\\\\"btnClose\\\\\\\" href=\\\\\\\"adbinapp://cancel\\\\\\\">\\\\n    <svg xmlns=\\\\\\\"http://www.w3.org/2000/svg\\\\\\\" height=\\\\\\\"18\\\\\\\" viewbox=\\\\\\\"0 0 18 18\\\\\\\" width=\\\\\\\"18\\\\\\\" class=\\\\\\\"close\\\\\\\">\\\\n  <rect id=\\\\\\\"Canvas\\\\\\\" fill=\\\\\\\"#ffffff\\\\\\\" opacity=\\\\\\\"0\\\\\\\" width=\\\\\\\"18\\\\\\\" height=\\\\\\\"18\\\\\\\" />\\\\n  <path fill=\\\\\\\"currentColor\\\\\\\" xmlns=\\\\\\\"http://www.w3.org/2000/svg\\\\\\\" d=\\\\\\\"M13.2425,3.343,9,7.586,4.7575,3.343a.5.5,0,0,0-.707,0L3.343,4.05a.5.5,0,0,0,0,.707L7.586,9,3.343,13.2425a.5.5,0,0,0,0,.707l.707.7075a.5.5,0,0,0,.707,0L9,10.414l4.2425,4.243a.5.5,0,0,0,.707,0l.7075-.707a.5.5,0,0,0,0-.707L10.414,9l4.243-4.2425a.5.5,0,0,0,0-.707L13.95,3.343a.5.5,0,0,0-.70711-.00039Z\\\\\\\" />\\\\n</svg>\\\\n  </a>\\\\n</div><div class=\\\\\\\"image\\\\\\\" data-uuid=\\\\\\\"88c0afd9-1e3d-4d52-900f-fa08a56ad5e2\\\\\\\">\\\\n<img src=\\\\\\\"https://d2sqqyy3wk3mtg.cloudfront.net/6d123ed0-79c8-40bb-923e-d079c810bb68/urn:aaid:aem:f42f6d80-433f-4811-bda2-7fb42d17aa8f/oak:1.0::ci:c33d550028ccaba70c8208b42ae86ae7/c44127de-e0de-3436-9866-7673da9a798a\\\\\\\" alt=\\\\\\\"\\\\\\\">\\\\n</div><div class=\\\\\\\"text\\\\\\\" data-uuid=\\\\\\\"e7e49562-a365-4d38-be0e-d69cba829cb4\\\\\\\">\\\\n<h3>In App for the dogs</h3>\\\\n<p>For E2E testing!</p>\\\\n</div><div data-uuid=\\\\\\\"e69adbda-8cfc-4c51-bcab-f8b2fd314d8f\\\\\\\" class=\\\\\\\"buttons\\\\\\\">\\\\n  <a class=\\\\\\\"button\\\\\\\" data-uuid=\\\\\\\"864bd990-fc7f-4b1a-8d45-069c58981eb2\\\\\\\" href=\\\\\\\"adbinapp://dismiss\\\\\\\">Dismiss</a>\\\\n</div>\\\\n\\\\t</div>\\\\n\\\\n\\\\n</body></html>\\\",\\\"_xdm\\\":{\\\"mixins\\\":{\\\"_experience\\\":{\\\"customerJourneyManagement\\\":{\\\"messageExecution\\\":{\\\"messageExecutionID\\\":\\\"dummy_message_execution_id_7a237b33-2648-4903-82d1-efa1aac7c60d-all-visitors-everytime\\\",\\\"messageID\\\":\\\"fb31245e-3382-4b3a-a15a-dcf11f463020\\\",\\\"messagePublicationID\\\":\\\"aafe8df1-9c30-496d-ba0c-4b300f8cbabb\\\",\\\"ajoCampaignID\\\":\\\"7a237b33-2648-4903-82d1-efa1aac7c60d\\\",\\\"ajoCampaignVersionID\\\":\\\"38bd427b-f48e-4b3e-a4e1-03033b830d66\\\"},\\\"messageProfile\\\":{\\\"channel\\\":{\\\"_id\\\":\\\"https://ns.adobe.com/xdm/channels/inapp\\\"}}}}}}}}]}]}\",\n" +
                "        \"format\": \"application/json\"\n" +
                "      },\n" +
                "      \"etag\": \"1\",\n" +
                "      \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-json\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"placement\": {\n" +
                "    \"id\": \"com.adobe.marketing.mobile.messagingsample\",\n" +
                "    \"etag\": \"1\"\n" +
                "  }\n" +
                "}")
        eventData.put("payload", payload)
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8")

        val edgeResponseEvent = Event.Builder(
            "AEP Response Event Handle",
            "com.adobe.eventType.edge", "personalization:decisions"
        )
            .setEventData(eventData)
            .build()
        MobileCore.dispatchEvent(edgeResponseEvent, null)
    }
}