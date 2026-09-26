package com.evolution.dropfiledaemon.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConcurrentTaskServiceExecuteAtLeastOneTest {

    private ConcurrentTaskService underTest;

    private static final class TestError extends Error {
        private TestError(String message) {
            super(message);
        }
    }

    @BeforeEach
    void before() {
        underTest = new ConcurrentTaskService();
    }

    @AfterEach
    void after() {
        underTest.close();
    }

    @Test
    void shouldThrowNpeWhenTimeoutIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> underTest.executeAtLeastOne(List.of(() -> {
                }), null)
        );
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTimeoutIsZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> underTest.executeAtLeastOne(List.of(() -> {
                }), Duration.ZERO)
        );
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTimeoutIsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> underTest.executeAtLeastOne(List.of(() -> {
                }), Duration.ofMillis(-100))
        );
    }

    @Test
    void shouldReturnImmediatelyWhenRunnablesEmptyOrNull() throws Exception {
        underTest.executeAtLeastOne(null, Duration.ofSeconds(1));
        underTest.executeAtLeastOne(Collections.emptyList(), Duration.ofSeconds(1));
    }

    @Test
    void shouldExecuteSingleRunnableSuccessfully() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        underTest.executeAtLeastOne(
                List.of(() -> executed.set(true)),
                Duration.ofSeconds(10)
        );
        assertThat(executed.get(), is(true));
    }

    @Test
    void shouldReturnOnFirstSuccessAndCancelOthers() throws Exception {
        CountDownLatch slowTaskStartedLatch = new CountDownLatch(1);
        CountDownLatch slowTaskInterruptedLatch = new CountDownLatch(1);

        Runnable slowTask = () -> {
            slowTaskStartedLatch.countDown();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                slowTaskInterruptedLatch.countDown();
                Thread.currentThread().interrupt();
            }
        };

        Runnable fastSuccess = () -> {
            try {
                slowTaskStartedLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        underTest.executeAtLeastOne(
                List.of(slowTask, fastSuccess),
                Duration.ofSeconds(2)
        );

        boolean interrupted = slowTaskInterruptedLatch.await(5, TimeUnit.SECONDS);
        assertThat(interrupted, is(true));
    }

    @Test
    void shouldSucceedWhenFastTasksFailButSlowTaskSucceeds() throws Exception {
        AtomicBoolean slowTaskSucceeded = new AtomicBoolean(false);

        Runnable fastFail1 = () -> {
            throw new RuntimeException("Fail 1");
        };
        Runnable fastFail2 = () -> {
            throw new RuntimeException("Fail 2");
        };
        Runnable slowSuccess = () -> {
            try {
                Thread.sleep(100);
                slowTaskSucceeded.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        underTest.executeAtLeastOne(
                List.of(fastFail1, fastFail2, slowSuccess),
                Duration.ofSeconds(5)
        );

        assertThat(slowTaskSucceeded.get(), is(true));
    }

    @Test
    void shouldThrowIllegalStateExceptionWithSuppressedWhenAllFail() {
        Runnable fail1 = () -> {
            throw new IllegalArgumentException("Cause 1");
        };
        Runnable fail2 = () -> {
            throw new IllegalStateException("Cause 2");
        };

        ExecutionException ex = assertThrows(
                ExecutionException.class,
                () -> underTest.executeAtLeastOne(List.of(fail1, fail2), Duration.ofSeconds(1))
        );

        assertThat(ex.getCause(), instanceOf(IllegalStateException.class));
        assertThat(ex.getCause().getMessage(), is("All endpoints unreachable"));

        Throwable[] suppressed = ex.getCause().getSuppressed();
        assertThat(suppressed, arrayWithSize(2));

        var suppressedClasses = Arrays.stream(suppressed)
                .map(Object::getClass)
                .toList();

        assertThat(suppressedClasses, containsInAnyOrder(
                IllegalArgumentException.class,
                IllegalStateException.class
        ));
    }

    @Test
    void shouldThrowTimeoutExceptionWhenAllHang() {
        Runnable hangingTask = () -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        ExecutionException ex = assertThrows(
                ExecutionException.class,
                () -> underTest.executeAtLeastOne(
                        List.of(hangingTask, hangingTask),
                        Duration.ofMillis(200)
                )
        );

        assertThat(ex.getCause(), instanceOf(TimeoutException.class));
    }

    @Test
    void shouldIncludePriorFailuresInSuppressedOnTimeout() throws Exception {
        CountDownLatch failStartedLatch = new CountDownLatch(1);

        Runnable fastFail = () -> {
            failStartedLatch.countDown();
            throw new RuntimeException("Fast error");
        };
        Runnable hangingTask = () -> {
            try {
                failStartedLatch.await(5, TimeUnit.SECONDS);
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        ExecutionException ex = assertThrows(
                ExecutionException.class,
                () -> underTest.executeAtLeastOne(
                        List.of(fastFail, hangingTask),
                        Duration.ofMillis(300)
                )
        );

        assertThat(ex.getCause(), instanceOf(TimeoutException.class));

        Throwable[] suppressed = ex.getCause().getSuppressed();
        assertThat(suppressed, arrayWithSize(1));
        assertThat(suppressed[0], instanceOf(RuntimeException.class));
        assertThat(suppressed[0].getMessage(), is("Fast error"));
    }

    @Test
    void shouldPropagateErrorDirectlyAndCancelOthers() throws Exception {
        CountDownLatch secondaryStartedLatch = new CountDownLatch(1);
        CountDownLatch secondaryInterruptedLatch = new CountDownLatch(1);

        Runnable secondaryTask = () -> {
            secondaryStartedLatch.countDown();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                secondaryInterruptedLatch.countDown();
                Thread.currentThread().interrupt();
            }
        };

        Runnable errorTask = () -> {
            try {
                secondaryStartedLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new TestError("Simulated critical error");
        };

        TestError error = assertThrows(
                TestError.class,
                () -> underTest.executeAtLeastOne(
                        List.of(errorTask, secondaryTask),
                        Duration.ofSeconds(10)
                )
        );

        assertThat(error.getMessage(), is("Simulated critical error"));

        boolean interrupted = secondaryInterruptedLatch.await(5, TimeUnit.SECONDS);
        assertThat(interrupted, is(true));
    }

    @Test
    void shouldRestoreInterruptFlagWhenCallerIsInterrupted() throws Exception {
        CountDownLatch taskStartedLatch = new CountDownLatch(1);
        AtomicBoolean callerWasInterrupted = new AtomicBoolean(false);

        Runnable hangingTask = () -> {
            taskStartedLatch.countDown();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Thread callerThread = new Thread(() -> {
            try {
                underTest.executeAtLeastOne(List.of(hangingTask), Duration.ofSeconds(5));
            } catch (ExecutionException ignored) {
            } finally {
                callerWasInterrupted.set(Thread.currentThread().isInterrupted());
            }
        });

        callerThread.start();
        assertThat(taskStartedLatch.await(1, TimeUnit.SECONDS), is(true));

        callerThread.interrupt();
        callerThread.join(2000);

        assertThat(callerThread.isAlive(), is(false));
        assertThat(callerWasInterrupted.get(), is(true));
    }
}