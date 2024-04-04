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
import static com.adobe.marketing.mobile.util.MonitorExtension.EventSpec;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SDKHelper;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ServiceProviderHelper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Test helper for functional testing to read, write, reset and assert against eventhub events,
 * shared states and persistence data.
 */
public class TestHelper {
    private static final String LOG_SOURCE = "TestHelper";
    // List of threads to wait for after test execution
    private static final List<String> knownThreads = new ArrayList<String>();
    private static final FunctionalTestNetworkService testNetworkService =
            new FunctionalTestNetworkService();
    private static Application defaultApplication;

    // List of threads to wait for after test execution
    private static final List<String> sdkThreadPrefixes = new ArrayList<>();

    static {
        sdkThreadPrefixes.add("pool"); // used for threads that execute the listeners code
        sdkThreadPrefixes.add("ADB"); // module internal threads
    }

    /**
     * {@code TestRule} which sets up the MobileCore for testing before each test execution, and
     * tearsdown the MobileCore after test execution.
     *
     * <p>To use, add the following to your test class:
     *
     * <pre>
     * 	&#064;Rule
     * 	public FunctionalTestHelper.SetupCoreRule coreRule = new FunctionalTestHelper.SetupCoreRule();
     * </pre>
     */
    public static class SetupCoreRule implements TestRule {

        private static final String LOG_SOURCE = "SetupCoreRule";

        @Override
        public Statement apply(
                @NonNull final Statement base, @NonNull final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (defaultApplication == null) {
                        Context context =
                                InstrumentationRegistry.getInstrumentation().getTargetContext();
                        defaultApplication =
                                Instrumentation.newApplication(CustomApplication.class, context);
                    }

                    SDKHelper.resetSDK();
                    setTestableNetworkService();
                    MobileCore.setLogLevel(LoggingMode.VERBOSE);
                    MobileCore.setApplication(defaultApplication);
                    clearAllDatastores();
                    Log.debug(LOG_TAG, LOG_SOURCE, "Execute '%s'", description.getMethodName());

                    try {
                        base.evaluate();
                    } catch (Throwable e) {
                        Log.debug(LOG_TAG, LOG_SOURCE, "Wait after test failure.");
                        throw e; // rethrow test failure
                    } finally {
                        // After test execution
                        Log.debug(
                                LOG_TAG, LOG_SOURCE, "Finished '%s'", description.getMethodName());
                        waitForThreads(5000); // wait to allow thread to run after test execution
                        SDKHelper.resetSDK();
                        clearAllDatastores();
                        resetTestExpectations();
                        resetServiceProvider();
                    }
                }
            };
        }
    }

    /**
     * {@code TestRule} which sets up the MobileCore for testing before each test execution, and
     * tearsdown the MobileCore after test execution. This version of the class uses the actual
     * network service.
     *
     * <p>To use, add the following to your test class:
     *
     * <pre>
     * 	&#064;Rule
     * 	public FunctionalTestHelper.SetupCoreRuleWithRealNetworkService coreRule = new FunctionalTestHelper.SetupCoreRuleWithRealNetworkService();
     * </pre>
     */
    public static class SetupCoreRuleWithRealNetworkService implements TestRule {
        private static final String LOG_SOURCE = "SetupCoreRuleWithRealNetworkService";

        @Override
        public Statement apply(
                @NonNull final Statement base, @NonNull final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (defaultApplication == null) {
                        Context context =
                                InstrumentationRegistry.getInstrumentation().getTargetContext();
                        defaultApplication =
                                Instrumentation.newApplication(CustomApplication.class, context);
                    }

                    SDKHelper.resetSDK();
                    MobileCore.setLogLevel(LoggingMode.VERBOSE);
                    MobileCore.setApplication(defaultApplication);
                    clearAllDatastores();
                    Log.debug(LOG_TAG, LOG_SOURCE, "Execute '%s'", description.getMethodName());

                    try {
                        base.evaluate();
                    } catch (Throwable e) {
                        Log.debug(LOG_TAG, LOG_SOURCE, "Wait after test failure.");
                        throw e; // rethrow test failure
                    } finally {
                        // After test execution
                        Log.debug(
                                LOG_TAG, LOG_SOURCE, "Finished '%s'", description.getMethodName());
                        waitForThreads(5000); // wait to allow thread to run after test execution
                        SDKHelper.resetSDK();
                        clearAllDatastores();
                        resetTestExpectations();
                        resetServiceProvider();
                    }
                }
            };
        }
    }

    /**
     * Reset the {@link MobileCore} and {@link com.adobe.marketing.mobile.services.ServiceProvider}
     * without clearing persistence or database. Initializes {@code MobileCore} and {@code
     * ServiceProvider} for testing after resetting by, setting the {@link
     * FunctionalTestNetworkService} to the {@code ServiceProvider}, and setting the instrumented
     * test application to {@code MobileCore}. This method does not clear the shared preferences,
     * application cache directory, or database directory.
     */
    public static void resetCoreHelper() {
        SDKHelper.resetSDK();
        ServiceProviderHelper.resetServices();
        setTestableNetworkService();
        MobileCore.setLogLevel(LoggingMode.VERBOSE);
        MobileCore.setApplication(defaultApplication);
    }

    /**
     * {@code TestRule} which registers the {@code MonitorExtension}, allowing test cases to assert
     * events passing through the {@code EventHub}. This {@code TestRule} must be applied after the
     * {@link SetupCoreRule} to ensure the {@code MobileCore} is setup for testing first.
     *
     * <p>To use, add the following to your test class:
     *
     * <pre>
     *  &#064;Rule
     * 	public RuleChain rule = RuleChain.outerRule(new SetupCoreRule())
     * 							.around(new RegisterMonitorExtensionRule());
     * </pre>
     */
    public static class RegisterMonitorExtensionRule implements TestRule {

        @Override
        @SuppressWarnings("deprecation")
        public Statement apply(
                @NonNull final Statement base, @NonNull final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    // Use registerExtension here to avoid starting the core yet, the tests should
                    // start it after all extensions have been registered
                    MobileCore.registerExtensions(
                            Collections.singletonList(MonitorExtension.class), null);

                    try {
                        base.evaluate();
                    } finally {
                        MonitorExtension.reset();
                    }
                }
            };
        }
    }

    public static class LogOnErrorRule implements TestRule {

        @Override
        public Statement apply(
                @NonNull final Statement base, @NonNull final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        base.evaluate();
                    } catch (Throwable t) {
                        throw new Throwable(collectLogCat(description.getMethodName()), t);
                    }
                }
            };
        }
    }

    /** Get the LogCat logs */
    private static String collectLogCat(final String methodName) {
        Process process;
        StringBuilder log = new StringBuilder();

        try {
            // Setting to just last 50 lines as logs are passed as Throwable stack trace which
            // has a line limit. The SDK logs have many multi-line entries which blow up the logs
            // quickly
            // If the log string is too long, it can crash the Throwable call.
            process =
                    Runtime.getRuntime()
                            .exec(
                                    "logcat -t 50 -d AdobeExperienceSDK:V TestRunner:I Hermetic:V"
                                            + " *:S");
            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            boolean ignoreLines = false; // "started" line may not be in last 50 lines

            while ((line = bufferedReader.readLine()) != null) {
                if (ignoreLines && line.matches(".*started: " + methodName + ".*")) {
                    ignoreLines = false;
                }

                if (!ignoreLines) {
                    log.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            // ignore
        }

        return log.toString();
    }

    /**
     * Waits for all the known SDK threads to finish or fails the test after timeoutMillis if some
     * of them are still running when the timer expires. If timeoutMillis is 0, a default timeout
     * will be set = 1000ms
     *
     * @param timeoutMillis max waiting time
     */
    public static void waitForThreads(final int timeoutMillis) {
        int TEST_DEFAULT_TIMEOUT_MS = 1000;
        int TEST_DEFAULT_SLEEP_MS = 50;
        int TEST_INITIAL_SLEEP_MS = 100;

        long startTime = System.currentTimeMillis();
        int timeoutTestMillis = timeoutMillis > 0 ? timeoutMillis : TEST_DEFAULT_TIMEOUT_MS;
        int sleepTime = Math.min(timeoutTestMillis, TEST_DEFAULT_SLEEP_MS);

        sleep(TEST_INITIAL_SLEEP_MS);
        Set<Thread> threadSet = getEligibleThreads();

        while (threadSet.size() > 0
                && ((System.currentTimeMillis() - startTime) < timeoutTestMillis)) {
            Log.debug(
                    LOG_TAG,
                    LOG_SOURCE,
                    "waitForThreads - Still waiting for " + threadSet.size() + " thread(s)");

            for (Thread t : threadSet) {
                Log.debug(
                        LOG_TAG,
                        LOG_SOURCE,
                        "waitForThreads - Waiting for thread "
                                + t.getName()
                                + " ("
                                + t.getId()
                                + ")");
                boolean done = false;
                boolean timedOut = false;

                while (!done && !timedOut) {
                    if (t.getState().equals(Thread.State.TERMINATED)
                            || t.getState().equals(Thread.State.TIMED_WAITING)
                            || t.getState().equals(Thread.State.WAITING)) {
                        // Cannot use the join() API since we use a cached thread pool, which
                        // means that we keep idle threads around for 60secs (default timeout).
                        done = true;
                    } else {
                        // blocking
                        sleep(sleepTime);
                        timedOut = (System.currentTimeMillis() - startTime) > timeoutTestMillis;
                    }
                }

                if (timedOut) {
                    Log.debug(
                            LOG_TAG,
                            LOG_SOURCE,
                            "waitForThreads - Timeout out waiting for thread "
                                    + t.getName()
                                    + " ("
                                    + t.getId()
                                    + ")");
                } else {
                    Log.debug(
                            LOG_TAG,
                            LOG_SOURCE,
                            "waitForThreads - Done waiting for thread "
                                    + t.getName()
                                    + " ("
                                    + t.getId()
                                    + ")");
                }
            }

            threadSet = getEligibleThreads();
        }

        Log.debug(LOG_TAG, LOG_SOURCE, "waitForThreads - All known SDK threads are terminated.");
    }

    /**
     * Retrieves all the known SDK threads that are still running
     *
     * @return set of running tests
     */
    private static Set<Thread> getEligibleThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Set<Thread> eligibleThreads = new HashSet<>();

        for (Thread t : threadSet) {
            if (isAppThread(t)
                    && !t.getState().equals(Thread.State.WAITING)
                    && !t.getState().equals(Thread.State.TERMINATED)
                    && !t.getState().equals(Thread.State.TIMED_WAITING)) {
                eligibleThreads.add(t);
            }
        }

        return eligibleThreads;
    }

    /**
     * Checks if current thread is not a daemon and its name starts with one of the known SDK thread
     * names specified here {@link #sdkThreadPrefixes}
     *
     * @param t current thread to verify
     * @return true if it is a known thread, false otherwise
     */
    private static boolean isAppThread(final Thread t) {
        if (t.isDaemon()) {
            return false;
        }

        for (String prefix : sdkThreadPrefixes) {
            if (t.getName().startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /** Resets the network and event test expectations. */
    public static void resetTestExpectations() {
        Log.debug(
                LOG_TAG,
                LOG_SOURCE,
                "Resetting functional test expectations for events and network requests");

        if (testNetworkService != null) {
            testNetworkService.reset();
        }

        MonitorExtension.reset();
    }

    // ---------------------------------------------------------------------------------------------
    // Event Test Helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * Sets an expectation for a specific event type and source and how many times the event should
     * be dispatched.
     *
     * @param type the event type
     * @param source the event source
     * @param count the expected number of times the event is dispatched
     * @throws IllegalArgumentException if {@code count} is less than 1
     */
    public static void setExpectationEvent(
            final String type, final String source, final int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Cannot set expectation event count less than 1!");
        }

        MonitorExtension.setExpectedEvent(type, source, count);
    }

    /**
     * Asserts if all the expected events were received and fails if an unexpected event was seen.
     *
     * @param ignoreUnexpectedEvents if set on false, an assertion is made on unexpected events,
     *     otherwise the unexpected events are ignored
     * @throws InterruptedException
     * @see #setExpectationEvent(String, String, int)
     * @see #assertUnexpectedEvents()
     */
    public static void assertExpectedEvents(final boolean ignoreUnexpectedEvents)
            throws InterruptedException {
        Map<EventSpec, ADBCountDownLatch> expectedEvents = MonitorExtension.getExpectedEvents();

        if (expectedEvents.isEmpty()) {
            fail(
                    "There are no event expectations set, use this API after calling"
                            + " setExpectationEvent");
            return;
        }

        for (Map.Entry<EventSpec, ADBCountDownLatch> expected : expectedEvents.entrySet()) {
            boolean awaitResult =
                    expected.getValue()
                            .await(
                                    FunctionalTestConstants.Defaults.WAIT_EVENT_TIMEOUT_MS,
                                    TimeUnit.MILLISECONDS);
            assertTrue(
                    "Timed out waiting for event type "
                            + expected.getKey().type
                            + " and source "
                            + expected.getKey().source,
                    awaitResult);
            int expectedCount = expected.getValue().getInitialCount();
            int receivedCount = expected.getValue().getCurrentCount();
            String failMessage =
                    String.format(
                            "Expected %d events for '%s', but received %d",
                            expectedCount, expected.getKey(), receivedCount);
            assertEquals(failMessage, expectedCount, receivedCount);
        }

        if (!ignoreUnexpectedEvents) {
            assertUnexpectedEvents(false);
        }
    }

    /**
     * Asserts if any unexpected event was received. Use this method to verify the received events
     * are correct when setting event expectations. Waits a short time before evaluating received
     * events to allow all events to come in.
     *
     * @see #setExpectationEvent
     */
    public static void assertUnexpectedEvents() throws InterruptedException {
        assertUnexpectedEvents(true);
    }

    /**
     * Asserts if any unexpected event was received. Use this method to verify the received events
     * are correct when setting event expectations.
     *
     * @param shouldWait waits a short time to allow events to be received when true
     * @see #setExpectationEvent
     */
    public static void assertUnexpectedEvents(final boolean shouldWait)
            throws InterruptedException {
        // Short wait to allow events to come in
        if (shouldWait) {
            sleep(FunctionalTestConstants.Defaults.WAIT_TIMEOUT_MS);
        }

        int unexpectedEventsReceivedCount = 0;
        StringBuilder unexpectedEventsErrorString = new StringBuilder();

        Map<EventSpec, List<Event>> receivedEvents = MonitorExtension.getReceivedEvents();
        Map<EventSpec, ADBCountDownLatch> expectedEvents = MonitorExtension.getExpectedEvents();

        for (Map.Entry<EventSpec, List<Event>> receivedEvent : receivedEvents.entrySet()) {
            ADBCountDownLatch expectedEventLatch = expectedEvents.get(receivedEvent.getKey());

            if (expectedEventLatch != null) {
                expectedEventLatch.await(
                        FunctionalTestConstants.Defaults.WAIT_EVENT_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS);
                int expectedCount = expectedEventLatch.getInitialCount();
                int receivedCount = receivedEvent.getValue().size();
                String failMessage =
                        String.format(
                                "Expected %d events for '%s', but received %d",
                                expectedCount, receivedEvent.getKey(), receivedCount);
                assertEquals(failMessage, expectedCount, receivedCount);
            } else {
                unexpectedEventsReceivedCount += receivedEvent.getValue().size();
                unexpectedEventsErrorString.append(
                        String.format(
                                "(%s,%s,%d)",
                                receivedEvent.getKey().type,
                                receivedEvent.getKey().source,
                                receivedEvent.getValue().size()));
                Log.debug(
                        LOG_TAG,
                        LOG_SOURCE,
                        "Received unexpected event with type: %s  source: %s",
                        receivedEvent.getKey().type,
                        receivedEvent.getKey().source);
            }
        }

        assertEquals(
                String.format(
                        "Received %d unexpected event(s): %s",
                        unexpectedEventsReceivedCount, unexpectedEventsErrorString),
                0,
                unexpectedEventsReceivedCount);
    }

    /**
     * Returns the {@code Event}(s) dispatched through the Event Hub, or empty if none was found.
     * Use this API after calling {@link #setExpectationEvent(String, String, int)} to wait for the
     * expected events. The wait time for each event is {@link
     * FunctionalTestConstants.Defaults#WAIT_EVENT_TIMEOUT_MS}ms.
     *
     * @param type the event type as in the expectation
     * @param source the event source as in the expectation
     * @return list of events with the provided {@code type} and {@code source}, or empty if none
     *     was dispatched
     * @throws InterruptedException
     * @throws IllegalArgumentException if {@code type} or {@code source} are null or empty strings
     */
    public static List<Event> getDispatchedEventsWith(final String type, final String source)
            throws InterruptedException {
        return getDispatchedEventsWith(
                type, source, FunctionalTestConstants.Defaults.WAIT_EVENT_TIMEOUT_MS);
    }

    /**
     * Returns the {@code Event}(s) dispatched through the Event Hub, or empty if none was found.
     * Use this API after calling {@link #setExpectationEvent(String, String, int)} to wait for the
     * right amount of time
     *
     * @param type the event type as in the expectation
     * @param source the event source as in the expectation
     * @param timeout how long should this method wait for the expected event, in milliseconds.
     * @return list of events with the provided {@code type} and {@code source}, or empty if none
     *     was dispatched
     * @throws InterruptedException
     * @throws IllegalArgumentException if {@code type} or {@code source} are null or empty strings
     */
    public static List<Event> getDispatchedEventsWith(
            final String type, final String source, int timeout) throws InterruptedException {
        EventSpec eventSpec = new EventSpec(source, type);

        Map<EventSpec, List<Event>> receivedEvents = MonitorExtension.getReceivedEvents();
        Map<EventSpec, ADBCountDownLatch> expectedEvents = MonitorExtension.getExpectedEvents();

        ADBCountDownLatch expectedEventLatch = expectedEvents.get(eventSpec);

        if (expectedEventLatch != null) {
            boolean awaitResult = expectedEventLatch.await(timeout, TimeUnit.MILLISECONDS);
            assertTrue(
                    "Timed out waiting for event type "
                            + eventSpec.type
                            + " and source "
                            + eventSpec.source,
                    awaitResult);
        } else {
            sleep(FunctionalTestConstants.Defaults.WAIT_TIMEOUT_MS);
        }

        return receivedEvents.containsKey(eventSpec)
                ? receivedEvents.get(eventSpec)
                : Collections.emptyList();
    }

    /**
     * Synchronous call to get the shared state for the specified {@code stateOwner}. This API
     * throws an assertion failure in case of timeout.
     *
     * @param stateOwner the owner extension of the shared state (typically the name of the
     *     extension)
     * @param timeout how long should this method wait for the requested shared state, in
     *     milliseconds
     * @return latest shared state of the given {@code stateOwner} or null if no shared state was
     *     found
     * @throws InterruptedException
     */
    public static Map<String, Object> getSharedStateFor(final String stateOwner, int timeout)
            throws InterruptedException {
        Event event =
                new Event.Builder(
                                "Get Shared State Request",
                                FunctionalTestConstants.EventType.MONITOR,
                                FunctionalTestConstants.EventSource.SHARED_STATE_REQUEST)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put(
                                                FunctionalTestConstants.EventDataKey.STATE_OWNER,
                                                stateOwner);
                                    }
                                })
                        .build();

        final CountDownLatch latch = new CountDownLatch(1);
        final Map<String, Object> sharedState = new HashMap<>();
        MobileCore.dispatchEventWithResponseCallback(
                event,
                5000,
                new AdobeCallbackWithError<Event>() {
                    @Override
                    public void call(final Event event) {
                        if (event.getEventData() != null) {
                            sharedState.putAll(event.getEventData());
                        }

                        latch.countDown();
                    }

                    @Override
                    public void fail(final AdobeError adobeError) {
                        Log.debug(
                                LOG_TAG,
                                LOG_SOURCE,
                                "Failed to get shared state for %s: %s",
                                stateOwner,
                                adobeError.getErrorName());
                    }
                });

        assertTrue(
                "Timeout waiting for shared state " + stateOwner,
                latch.await(timeout, TimeUnit.MILLISECONDS));
        return sharedState.isEmpty() ? null : sharedState;
    }

    // ---------------------------------------------------------------------------------------------
    // Network Test Helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * Set a custom network response to an Edge network request.
     *
     * @param url the url string for which to return the response
     * @param method the HTTP method for which to return the response
     * @param responseConnection the network response to be returned when a request matching the
     *     {@code url} and {@code method} is received. If null is provided, a default '200' response
     *     is used.
     */
    public static void setNetworkResponseFor(
            final String url, final HttpMethod method, final HttpConnecting responseConnection) {
        testNetworkService.setResponseConnectionFor(
                new TestableNetworkRequest(url, method), responseConnection);
    }

    /**
     * Set a network request expectation.
     *
     * @param url the url string for which to set the expectation
     * @param method the HTTP method for which to set the expectation
     * @param expectedCount how many times a request with this {@code url} and {@code method} is
     *     expected to be sent
     */
    public static void setExpectationNetworkRequest(
            final String url, final HttpMethod method, final int expectedCount) {
        testNetworkService.setExpectedNetworkRequest(
                new TestableNetworkRequest(url, method), expectedCount);
    }

    /**
     * Asserts that the correct number of network requests were being sent, based on the previously
     * set expectations.
     *
     * @throws InterruptedException
     * @see #setExpectationNetworkRequest(String, HttpMethod, int)
     */
    public static void assertNetworkRequestCount() throws InterruptedException {
        waitForThreads(2000); // allow for some extra time for threads to finish before asserts
        Map<TestableNetworkRequest, ADBCountDownLatch> expectedNetworkRequests =
                testNetworkService.getExpectedNetworkRequests();

        if (expectedNetworkRequests.isEmpty()) {
            fail(
                    "There are no network request expectations set, use this API after calling"
                            + " setExpectationNetworkRequest");
            return;
        }

        for (Map.Entry<TestableNetworkRequest, ADBCountDownLatch> expectedRequest :
                expectedNetworkRequests.entrySet()) {
            boolean awaitResult = expectedRequest.getValue().await(5, TimeUnit.SECONDS);
            assertTrue(
                    "Time out waiting for network request with URL '"
                            + expectedRequest.getKey().getUrl()
                            + "' and method '"
                            + expectedRequest.getKey().getMethod().name()
                            + "'",
                    awaitResult);
            int expectedCount = expectedRequest.getValue().getInitialCount();
            int receivedCount = expectedRequest.getValue().getCurrentCount();
            String message =
                    String.format(
                            "Expected %d network requests for URL %s (%s), but received %d",
                            expectedCount,
                            expectedRequest.getKey().getUrl(),
                            expectedRequest.getKey().getMethod(),
                            receivedCount);
            assertEquals(message, expectedCount, receivedCount);
        }
    }

    /**
     * Returns the {@link TestableNetworkRequest}(s) sent through the Core NetworkService, or empty
     * if none was found. Use this API after calling {@link #setExpectationNetworkRequest(String,
     * HttpMethod, int)} to wait 2 seconds for each request.
     *
     * @param url The url string for which to retrieved the network requests sent
     * @param method the HTTP method for which to retrieve the network requests
     * @return list of network requests with the provided {@code url} and {@code method}, or empty
     *     if none was dispatched
     * @throws InterruptedException
     */
    public static List<TestableNetworkRequest> getNetworkRequestsWith(
            final String url, final HttpMethod method) throws InterruptedException {
        return getNetworkRequestsWith(
                url, method, FunctionalTestConstants.Defaults.WAIT_NETWORK_REQUEST_TIMEOUT_MS);
    }

    /**
     * Returns the {@link TestableNetworkRequest}(s) sent through the Core NetworkService, or empty
     * if none was found. Use this API after calling {@link #setExpectationNetworkRequest(String,
     * HttpMethod, int)} to wait for each request.
     *
     * @param url The url string for which to retrieved the network requests sent
     * @param method the HTTP method for which to retrieve the network requests
     * @param timeoutMillis how long should this method wait for the expected network requests, in
     *     milliseconds
     * @return list of network requests with the provided {@code url} and {@code command}, or empty
     *     if none was dispatched
     * @throws InterruptedException
     */
    public static List<TestableNetworkRequest> getNetworkRequestsWith(
            final String url, final HttpMethod method, final int timeoutMillis)
            throws InterruptedException {
        TestableNetworkRequest networkRequest = new TestableNetworkRequest(url, method);

        if (testNetworkService.isNetworkRequestExpected(networkRequest)) {
            assertTrue(
                    "Time out waiting for network request(s) with URL '"
                            + networkRequest.getUrl()
                            + "' and method '"
                            + networkRequest.getMethod().name()
                            + "'",
                    testNetworkService.awaitFor(networkRequest, timeoutMillis));
        } else {
            sleep(timeoutMillis);
        }

        return testNetworkService.getReceivedNetworkRequestsMatching(networkRequest);
    }

    /**
     * Create a network response to be used when calling {@link #setNetworkResponseFor(String,
     * HttpMethod, HttpConnecting)}.
     *
     * @param responseString the network response string, returned by {@link
     *     HttpConnecting#getInputStream()}
     * @param code the HTTP status code, returned by {@link HttpConnecting#getResponseCode()}
     * @return an {@link HttpConnecting} object
     * @see #setNetworkResponseFor(String, HttpMethod, HttpConnecting)
     */
    public static HttpConnecting createNetworkResponse(
            final String responseString, final int code) {
        return createNetworkResponse(responseString, null, code, null, null);
    }

    /**
     * Create a network response to be used when calling {@link #setNetworkResponseFor(String,
     * HttpMethod, HttpConnecting)}.
     *
     * @param responseString the network response string, returned by {@link
     *     HttpConnecting#getInputStream()}
     * @param errorString the network error string, returned by {@link
     *     HttpConnecting#getErrorStream()}
     * @param code the HTTP status code, returned by {@link HttpConnecting#getResponseCode()}
     * @param responseMessage the network response message, returned by {@link
     *     HttpConnecting#getResponseMessage()}
     * @param propertyMap the network response header map, returned by {@link
     *     HttpConnecting#getResponsePropertyValue(String)}
     * @return an {@link HttpConnecting} object
     * @see #setNetworkResponseFor(String, HttpMethod, HttpConnecting)
     */
    public static HttpConnecting createNetworkResponse(
            final String responseString,
            final String errorString,
            final int code,
            final String responseMessage,
            final Map<String, String> propertyMap) {
        return new HttpConnecting() {
            @Override
            public InputStream getInputStream() {
                if (responseString != null) {
                    return new ByteArrayInputStream(
                            responseString.getBytes(StandardCharsets.UTF_8));
                }

                return null;
            }

            @Override
            public InputStream getErrorStream() {
                if (errorString != null) {
                    return new ByteArrayInputStream(errorString.getBytes(StandardCharsets.UTF_8));
                }

                return null;
            }

            @Override
            public int getResponseCode() {
                return code;
            }

            @Override
            public String getResponseMessage() {
                return responseMessage;
            }

            @Override
            public String getResponsePropertyValue(String responsePropertyKey) {
                if (propertyMap != null) {
                    return propertyMap.get(responsePropertyKey);
                }

                return null;
            }

            @Override
            public void close() {}
        };
    }

    /**
     * Sets the provided delay for all network responses, until reset
     *
     * @param delaySec delay in seconds
     */
    public static void enableNetworkResponseDelay(final Integer delaySec) {
        if (delaySec < 0) {
            return;
        }

        testNetworkService.enableDelayedResponse(delaySec);
    }

    /**
     * Pause test execution for the given {@code milliseconds}
     *
     * @param milliseconds the time to sleep the current thread.
     */
    public static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Use this API for JSON formatted {@code NetworkRequest} body in order to retrieve a flattened
     * map containing its data.
     *
     * @param networkRequest the {@link NetworkRequest} to parse
     * @return The JSON request body represented as a flatten map
     */
    public static Map<String, String> getFlattenedNetworkRequestBody(
            final NetworkRequest networkRequest) {
        return FunctionalTestUtils.flattenBytes(networkRequest.getBody());
    }

    /** Get the default application */
    public static Application getDefaultApplication() {
        if (defaultApplication == null) {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            try {
                defaultApplication =
                        Instrumentation.newApplication(CustomApplication.class, context);
            } catch (Exception ignored) {
                return null;
            }
        }
        return defaultApplication;
    }

    /** Dummy Application for the test instrumentation */
    public static class CustomApplication extends Application {
        public CustomApplication() {}
    }

    private static void clearAllDatastores() {
        final List<String> knownDatastores =
                new ArrayList<String>() {
                    {
                        add(FunctionalTestConstants.SharedState.IDENTITY);
                        add(FunctionalTestConstants.SharedState.MESSAGING);
                        add(FunctionalTestConstants.EDGE_DATA_STORAGE);
                        add("AdobeMobile_ConfigState");
                    }
                };
        final Application application = TestHelper.defaultApplication;

        if (application == null) {
            fail(
                    "FunctionalTestHelper - Unable to clear datastores. Application is null, fast"
                            + " failing the test case.");
        }

        final Context context = application.getApplicationContext();

        if (context == null) {
            fail(
                    "FunctionalTestHelper - Unable to clear datastores. Context is null, fast"
                            + " failing the test case.");
        }

        for (String datastore : knownDatastores) {
            SharedPreferences sharedPreferences =
                    context.getSharedPreferences(datastore, Context.MODE_PRIVATE);

            if (sharedPreferences == null) {
                fail(
                        "FunctionalTestHelper - Unable to clear datastores. sharedPreferences is"
                                + " null, fast failing the test case.");
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
        }
    }

    /**
     * Reset the {@link ServiceProvider} by clearing all files under the application cache folder,
     * clearing all files under the database folder, and instantiate new instances of each service
     * provider
     */
    private static void resetServiceProvider() {
        ServiceProviderHelper.cleanCacheDir();
        ServiceProviderHelper.cleanDatabaseDir();
        ServiceProviderHelper.resetServices();
    }

    /**
     * Replaces the {@link com.adobe.marketing.mobile.services.Networking} service with a mock
     * network service.
     */
    private static void setTestableNetworkService() {
        ServiceProvider.getInstance().setNetworkService(testNetworkService);
    }
}
