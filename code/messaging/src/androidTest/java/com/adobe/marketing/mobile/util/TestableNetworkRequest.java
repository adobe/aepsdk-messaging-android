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

package com.adobe.marketing.mobile.util;

import static com.adobe.marketing.mobile.util.FunctionalTestConstants.LOG_TAG;

import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NetworkRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Class represents a network request received by the {@code NetworkService}. */
public class TestableNetworkRequest extends NetworkRequest {
    private static final String LOG_SOURCE = "TestableNetworkRequest";
    private final Map<String, String> queryParamMap;

    public TestableNetworkRequest(String url, HttpMethod command) {
        this(url, command, null, null, 5, 5);
    }

    public TestableNetworkRequest(
            String url,
            HttpMethod command,
            byte[] connectPayload,
            Map<String, String> requestProperty,
            int connectTimeout,
            int readTimeout) {
        super(url, command, connectPayload, requestProperty, connectTimeout, readTimeout);
        queryParamMap = splitQueryParameters(url);
    }

    public String queryParam(final String key) {
        return queryParamMap.get(key);
    }

    /**
     * Two {@link TestableNetworkRequest}s are equal if their URLs have equal protocol, host and
     * paths and use the same {@link HttpMethod}.
     *
     * @param o the other {@link TestableNetworkRequest} to compare to
     * @return true if the provided request is equal to this
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TestableNetworkRequest that = (TestableNetworkRequest) o;

        if (this.getMethod() != that.getMethod()) {
            return false;
        }

        if (this.getUrl() == null && that.getUrl() == null) {
            return true;
        }

        try {
            // URL equality is based just on Protocol, Host, and Path
            URL rhUrl = new URL(this.getUrl());
            URL lhUrl = new URL(that.getUrl());
            return (Objects.equals(rhUrl.getHost(), lhUrl.getHost())
                    && Objects.equals(rhUrl.getProtocol(), lhUrl.getProtocol())
                    && Objects.equals(rhUrl.getPath(), lhUrl.getPath()));
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        try {
            URL u = new URL(this.getUrl());
            return Objects.hash(u.getProtocol(), u.getHost(), u.getPath(), this.getMethod());
        } catch (MalformedURLException e) {
            return Objects.hash(this.getUrl(), this.getMethod());
        }
    }

    private static Map<String, String> splitQueryParameters(final String url) {
        Map<String, String> queryParamMap = new HashMap<>();

        try {
            URL urlObj = new URL(url);

            if (urlObj.getQuery() == null) {
                return queryParamMap;
            }

            final String[] pairs = urlObj.getQuery().split("&");

            for (String pair : pairs) {
                int index = pair.indexOf("=");
                queryParamMap.put(pair.substring(0, index), pair.substring(index + 1));
            }
        } catch (MalformedURLException e) {
            Log.warning(LOG_TAG, LOG_SOURCE, "Failed to decode Network Request URL '%s'", url);
        }

        return queryParamMap;
    }
}
