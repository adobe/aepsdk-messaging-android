package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingUtils.toVariantMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestUtils {
    private static final String LOG_TAG = "TestUtils";
    private static final int STREAM_WRITE_BUFFER_SIZE = 4096;

    static void waitForExecutor(ExecutorService executor, int executorTime) {
        Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                // Fake task to check the execution termination
            }
        });

        try {
            future.get(executorTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail(String.format("Executor took longer than %s (sec)", executorTime));
        }
    }

    static List<Map> generateMessagePayload(final MessagePayloadConfig config) {
        if (config.count <= 0) {
            return null;
        }
        ArrayList<HashMap<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < config.count; i++) {
            HashMap<String, Object> item = new HashMap<>();
            HashMap<String, Object> data = new HashMap<>();
            HashMap<String, Variant> characteristics = new HashMap<>();
            item.put("schema", Variant.fromString("https://ns.adobe.com/experience/offer-management/content-component-json"));
            item.put("etag", Variant.fromString("2"));
            item.put("id", Variant.fromString("xcore:personalized-offer:142554af6579650" + i));
            characteristics.put("inappmessageExecutionId", Variant.fromString("UIA-65098551"));
            data.put("format", "application/json");
            data.put("characteristics", characteristics);
            data.put("id", "xcore:personalized-offer:142554af6579650" + i);
            data.put("content", "{\"version\":1,\"" + (config.isMissingRulesKey ? "invalid" : "rules") + "\":[{\"condition\":{\"type\":\"matcher\",\"definition\":{\"key\":\"isLoggedIn" + i + "\",\"matcher\":\"eq\",\"values\":[\"true\"]}},\"consequences\":[{" + (config.isMissingMessageId ? "" : "\"id\":\"fa99415e-dc8b-478a-84d2-21f67d13e866\",") + (config.isMissingMessageType ? "" : "\"type\":\"cjmiam\",") + (config.isMissingMessageDetail ? "" : "\"detail\":{\"mobileParameters\":{\"schemaVersion\":\"0.0.1\",\"width\":100,\"height\":100,\"verticalAlign\":\"center\",\"verticalInset\":0,\"horizontalAlign\":\"center\",\"horizontalInset\":0,\"uiTakeover\":true,\"displayAnimation\":\"bottom\",\"dismissAnimation\":\"bottom\",\"gestures\":{\"swipeDown\":\"adbinapp://dismiss?interaction=swipeDown\",\"swipeUp\":\"adbinapp://dismiss?interaction=swipeUp\"}},") + (config.hasHtmlPayloadMissing ? "" : "\"html\":\"<html>\\n<head>\\n\\t<meta name=\\\"viewport\\\" content=\\\"width=device-width, initial-scale=1.0\\\">\\n\\t<style>\\n\\t\\thtml,\\n\\t\\tbody {\\n\\t\\t\\tmargin: 0;\\n\\t\\t\\tpadding: 0;\\n\\t\\t\\ttext-align: center;\\n\\t\\t\\twidth: 100%;\\n\\t\\t\\theight: 100%;\\n\\t\\t\\tfont-family: adobe-clean, \\\"Source Sans Pro\\\", -apple-system, BlinkMacSystemFont, \\\"Segoe UI\\\", Roboto, sans-serif;\\n\\t\\t}\\n\\n\\t\\t.body {\\n\\t\\t\\tdisplay: flex;\\n\\t\\t\\tflex-direction: column;\\n\\t\\t\\tbackground-color: #121c3e;\\n\\t\\t\\tborder-radius: 5px;\\n\\t\\t\\tcolor: #333333;\\n\\t\\t\\twidth: 100vw;\\n\\t\\t\\theight: 100vh;\\n\\t\\t\\ttext-align: center;\\n\\t\\t\\talign-items: center;\\n\\t\\t\\tbackground-size: 'cover';\\n\\t\\t}\\n\\n\\t\\t.content {\\n\\t\\t\\twidth: 100%;\\n\\t\\t\\theight: 100%;\\n\\t\\t\\tdisplay: flex;\\n\\t\\t\\tjustify-content: center;\\n\\t\\t\\tflex-direction: column;\\n\\t\\t\\tposition: relative;\\n\\t\\t}\\n\\n\\t\\ta {\\n\\t\\t\\ttext-decoration: none;\\n\\t\\t}\\n\\n\\t\\t.image {\\n\\t\\t  height: 1rem;\\n\\t\\t  flex-grow: 4;\\n\\t\\t  flex-shrink: 1;\\n\\t\\t  display: flex;\\n\\t\\t  justify-content: center;\\n\\t\\t  width: 90%;\\n      flex-direction: column;\\n      align-items: center;\\n\\t\\t}\\n    .image img {\\n      max-height: 100%;\\n      max-width: 100%;\\n    }\\n\\n\\t\\t.btnClose {\\n\\t\\t\\tcolor: #000000;\\n\\t\\t}\\n\\n\\t\\t.closeBtn {\\n\\t\\t\\talign-self: flex-end;\\n\\t\\t\\twidth: 1.8rem;\\n\\t\\t\\theight: 1.8rem;\\n\\t\\t\\tmargin-top: 1rem;\\n\\t\\t\\tmargin-right: .3rem;\\n\\t\\t}\\n\\t</style>\\n</head>\\n\\n<body>\\n\\t<div class=\\\"body\\\">\\n    <div class=\\\"closeBtn\\\" data-btn-style=\\\"plain\\\" data-uuid=\\\"3de6f6ef-f98b-4981-9530-b3c47ae6984d\\\">\\n  <a class=\\\"btnClose\\\" href=\\\"adbinapp://dismiss?interaction=cancel\\\">\\n    <svg xmlns=\\\"http://www.w3.org/2000/svg\\\" height=\\\"18\\\" viewbox=\\\"0 0 18 18\\\" width=\\\"18\\\" class=\\\"close\\\">\\n  <rect id=\\\"Canvas\\\" fill=\\\"#ffffff\\\" opacity=\\\"0\\\" width=\\\"18\\\" height=\\\"18\\\" />\\n  <path fill=\\\"currentColor\\\" xmlns=\\\"http://www.w3.org/2000/svg\\\" d=\\\"M13.2425,3.343,9,7.586,4.7575,3.343a.5.5,0,0,0-.707,0L3.343,4.05a.5.5,0,0,0,0,.707L7.586,9,3.343,13.2425a.5.5,0,0,0,0,.707l.707.7075a.5.5,0,0,0,.707,0L9,10.414l4.2425,4.243a.5.5,0,0,0,.707,0l.7075-.707a.5.5,0,0,0,0-.707L10.414,9l4.243-4.2425a.5.5,0,0,0,0-.707L13.95,3.343a.5.5,0,0,0-.70711-.00039Z\\\" />\\n</svg>\\n  </a>\\n</div><div class=\\\"image\\\" data-uuid=\\\"46514c31-b883-4d1f-8f97-26f054309646\\\">\\n  <img src=\\\"https://www.adobe.com/adobe.png\\\" data-mediarepo-id=\\\"author-p16854-e23341-cmstg.adobeaemcloud.com\\\" alt=\\\"\\\">\\n</div>\\n\\n\\n</div></body></html>\",") + "\"_xdm\":{\"mixins\":{\"_experience\":{\"customerJourneyManagement\":{\"messageExecution\":{\"messageExecutionID\":\"UIA-65098551\",\"messageID\":\"6195c1e5-f92c-4fe4-b20d-0f3b175ff01b\",\"messagePublicationID\":\"b3c204db-fce6-4ba6-92b0-0c9da490be05\",\"ajoCampaignID\":\"d9dd1e85-173b-4aa2-aa7e-9c242e15f9da\",\"ajoCampaignVersionID\":\"84b9430a-3ac1-49d5-a687-98e2f6d03437\"},\"messageProfile\":{\"channel\":{\"_id\":\"https://ns.adobe.com/xdm/channels/inapp\"}}}}}}}}]}]}");
            item.put("data", data);
            items.add(item);
        }
        Map<String, Object> messagePayload = new HashMap<>();
        Map<String, Object> activity = new HashMap<>();
        Map<String, Object> placement = new HashMap<>();
        activity.put("etag", "27");
        if (!config.isUsingApplicationId) {
            if (config.invalidActivityId) {
                activity.put("id", "xcore:offer-activity:invalid");
            } else {
                activity.put("id", "mock_activity");
            }
            placement.put("etag", "1");
            if (config.invalidPlacementId) {
                placement.put("id", "xcore:offer-placement:invalid");
            } else {
                placement.put("id", "mock_placement");
            }
            messagePayload.put("activity", activity);
            messagePayload.put("placement", placement);
        } else {
            if (!config.hasNoScopeInPayload) {
                if (config.invalidApplicationId) {
                    // base 64 encoded "{"xdm:name":"non_matching_applicationId"}"
                    messagePayload.put("scope", "eyJ4ZG06bmFtZSI6Im5vbl9tYXRjaGluZ19hcHBsaWNhdGlvbklkIn0=");
                } else {
                    // base 64 encoded "{"xdm:name":"mock_applicationId"}"
                    messagePayload.put("scope", "eyJ4ZG06bmFtZSI6Im1vY2tfYXBwbGljYXRpb25JZCJ9");
                }
            }
        }
        messagePayload.put("items", items);
        List<Map> payload = new ArrayList<>();
        if (config.hasEmptyPayload) {
            payload.add(new HashMap<>());
        } else {
            payload.add(messagePayload);
        }
        return payload;
    }

    static Map<String, Variant> getMapFromFile(final String name) {
        try {
            final InputStream inputStream = TestUtils.convertResourceFileToInputStream(name, ".json");
            final JSONObject cachedMessagePayload = loadJsonFromFile(inputStream);
            return toVariantMap(cachedMessagePayload);
        } catch (final FileNotFoundException fileNotFoundException) {
            Log.warning(LOG_TAG, "Exception occurred when retrieving the cached message file: %s", fileNotFoundException.getMessage());
            return null;
        } catch (final IOException ioException) {
            Log.warning(LOG_TAG, "Exception occurred when converting the cached message file to a string: %s", ioException.getMessage());
            return null;
        } catch (final JSONException jsonException) {
            Log.warning(LOG_TAG, "Exception occurred when creating the JSONObject: %s", jsonException.getMessage());
            return null;
        }
    }

    static JSONObject loadJsonFromFile(final InputStream inputStream) throws IOException, JSONException {
        final String streamContents = StringUtils.streamToString(inputStream);
        inputStream.close();
        return new JSONObject(streamContents);
    }

    static InputStream convertResourceFileToInputStream(final String name, final String fileExtension) {
        return TestUtils.class.getClassLoader().getResourceAsStream(name + fileExtension);
    }

    static boolean readInputStreamIntoFile(final File cachedFile, final InputStream input, final boolean append) {
        boolean result;

        if (cachedFile == null || input == null) {
            return false;
        }

        FileOutputStream output = null;

        try {
            output = new FileOutputStream(cachedFile, append);
            final byte[] data = new byte[STREAM_WRITE_BUFFER_SIZE];
            int count;

            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            result = true;
        } catch (final IOException e) {
            Log.error(LOG_TAG, "IOException while attempting to write remote file (%s)", e);
            return false;
        } catch (final Exception e) {
            Log.error(LOG_TAG, "Unexpected exception while attempting to write remote file (%s)", e);
            return false;
        } finally {
            try {
                if (output != null) {
                    output.close();
                }

            } catch (final Exception e) {
                Log.error(LOG_TAG, "Unable to close the OutputStream (%s) ", e);
            }
        }

        return result;
    }
}

class MessagePayloadConfig {
    public int count = 0;
    public boolean isMissingRulesKey = false;
    public boolean isMissingMessageId = false;
    public boolean isMissingMessageType = false;
    public boolean isMissingMessageDetail = false;
    public boolean hasHtmlPayloadMissing = false;
    public boolean invalidActivityId = false;
    public boolean invalidPlacementId = false;
    public boolean isUsingApplicationId = false;
    public boolean invalidApplicationId = false;
    public boolean hasEmptyPayload = false;
    public boolean hasNoScopeInPayload = false;
}
