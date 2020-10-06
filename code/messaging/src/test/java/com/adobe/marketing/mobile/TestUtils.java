package com.adobe.marketing.mobile;

import org.junit.Assert;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestUtils {
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
}
