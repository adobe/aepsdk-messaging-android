/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Test retry rule for functional testing. This rule allows a failed test to be automatically
 * retried a specified number of times.
 */
public class TestRetryRule implements TestRule {
    private int numberOfTestAttempts;
    private int currentTestRun;

    public TestRetryRule(final int numberOfTestAttempts) {
        this.numberOfTestAttempts = numberOfTestAttempts;
    }

    public Statement apply(final Statement base, final Description description) {
        return statement(base, description);
    }

    private Statement statement(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable caughtThrowable = null;

                for (int i = 0; i < numberOfTestAttempts; i++) {
                    currentTestRun = i + 1;

                    try {
                        base.evaluate();
                        return;
                    } catch (Throwable t) {
                        caughtThrowable = t;
                        System.err.println(
                                description.getDisplayName()
                                        + ": run "
                                        + currentTestRun
                                        + " failed, "
                                        + (numberOfTestAttempts - currentTestRun)
                                        + " retries remain.");
                        System.err.println(
                                "test failure caused by: " + caughtThrowable.getLocalizedMessage());
                    }
                }

                System.err.println(
                        description.getDisplayName()
                                + ": giving up after "
                                + numberOfTestAttempts
                                + " failures");
                throw caughtThrowable;
            }
        };
    }
}
