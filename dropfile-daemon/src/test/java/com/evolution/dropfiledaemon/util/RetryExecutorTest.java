package com.evolution.dropfiledaemon.util;

import com.evolution.dropfile.common.CommonUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RetryExecutorTest {

    @Test
    public void retryIfNotNullValue() {
        AtomicInteger called = new AtomicInteger(0);

        Boolean result = RetryExecutor
                .call(() -> {
                    called.incrementAndGet();
                    if (called.get() == 2) {
                        return true;
                    }
                    return null;
                })
                .attempts(2)
                .delay(Duration.ofSeconds(0))
                .retryIf(it -> it.result() == null)
                .run();

        assertThat("Task should be called exactly 2 times", called.get(), is(2));
        assertThat("Result should be true", result, is(true));
    }

    @Test
    public void retryIfException() {
        AtomicInteger called = new AtomicInteger(0);

        Object result = RetryExecutor
                .call(() -> {
                    called.incrementAndGet();
                    if (called.get() == 2) {
                        return null;
                    }
                    throw new RuntimeException();
                })
                .attempts(2)
                .delay(Duration.ofSeconds(0))
                .retryIf(it -> it.exception() != null)
                .run();

        assertThat("Task should be called exactly 2 times", called.get(), is(2));
        assertThat("Result should be null", result, is(nullValue()));
    }

    @Test
    public void retryIfHasCalled() {
        AtomicInteger called = new AtomicInteger(0);
        AtomicInteger retryIf = new AtomicInteger(0);
        AtomicReference<List<Integer>> attemptsRetryIf = new AtomicReference<>(new ArrayList<>());
        AtomicReference<List<Exception>> exceptionRetryIf = new AtomicReference<>(new ArrayList<>());

        RetryExecutor.RetryExecutorException e = assertThrows(
                RetryExecutor.RetryExecutorException.class,
                () -> RetryExecutor
                        .call(() -> {
                            int i = called.incrementAndGet();
                            throw new RuntimeException("test message " + i);
                        })
                        .retryIf(it -> {
                            attemptsRetryIf.updateAndGet(integers -> {
                                integers.add(it.attempt());
                                return integers;
                            });
                            exceptionRetryIf.updateAndGet(exceptions -> {
                                exceptions.add(it.exception());
                                return exceptions;
                            });
                            retryIf.incrementAndGet();
                            return true;
                        })
                        .attempts(3)
                        .delay(Duration.ofMillis(0))
                        .run()
        );

        assertThat("Should contain 3 recorded exceptions", e.getExceptions().size(), is(3));
        assertThat("Task should be called exactly 3 times", called.get(), is(3));
        assertThat("retryIf predicate should be invoked 3 times", retryIf.get(), is(3));
        assertThat("Recorded attempt numbers should be 1, 2, 3", attemptsRetryIf.get(), hasItems(1, 2, 3));
        assertThat("Should record 3 exceptions in retryIf", exceptionRetryIf.get().size(), is(3));

        assertThat(
                "All recorded exceptions should be of the same type",
                exceptionRetryIf.get().stream().map(Object::getClass).distinct().toList().size(),
                is(1)
        );

        assertThat(
                "Exception messages should match attempt numbers",
                exceptionRetryIf.get().stream().map(Throwable::getMessage).toList(),
                hasItems("test message 1", "test message 2", "test message 3")
        );
    }

    @Test
    public void failsIfResultIsNull() {
        AtomicBoolean called = new AtomicBoolean(false);

        RetryExecutor.RetryExecutorException e = assertThrows(
                RetryExecutor.RetryExecutorException.class,
                () -> RetryExecutor
                        .call(() -> {
                            called.set(true);
                            return null;
                        })
                        .attempts(1)
                        .delay(Duration.ofMillis(0))
                        .run()
        );

        assertThat("Exceptions list should be empty when result is null without explicit retryIf",
                e.getExceptions().size(),
                is(0)
        );
        assertThat("Task should have been called once", called.get(), is(true));
    }

    @Test
    public void failsIfExceptionIsThrown() {
        AtomicBoolean called = new AtomicBoolean(false);

        RetryExecutor.RetryExecutorException e = assertThrows(
                RetryExecutor.RetryExecutorException.class,
                () -> RetryExecutor
                        .call(() -> {
                            called.set(true);
                            throw new RuntimeException();
                        })
                        .attempts(1)
                        .delay(Duration.ofMillis(0))
                        .run()
        );

        assertThat("Exceptions list should contain 1 exception", e.getExceptions().size(), is(1));
        assertThat("Task should have been called once", called.get(), is(true));
    }

    @Test
    public void throwsDuringCall() {
        AtomicBoolean called = new AtomicBoolean(false);

        RetryExecutor.RetryExecutorException e = assertThrows(
                RetryExecutor.RetryExecutorException.class,
                () -> RetryExecutor
                        .call(() -> {
                            called.set(true);
                            throw new RuntimeException("test message");
                        })
                        .attempts(1)
                        .delay(Duration.ofMillis(0))
                        .run()
        );

        assertThat("Exceptions list should contain 1 exception", e.getExceptions().size(), is(1));
        Exception exception = e.getExceptions().getFirst();
        assertThat("Exception class should be RuntimeException", exception.getClass(), is(RuntimeException.class));
        assertThat("Exception message should match the thrown message", exception.getMessage(), is("test message"));
        assertThat("Task should have been called once", called.get(), is(true));
    }

    @Test
    public void retry4Times() {
        AtomicInteger counter = new AtomicInteger(0);

        Boolean result = RetryExecutor
                .call(() -> {
                    if (counter.get() == 3) {
                        return true;
                    }
                    counter.incrementAndGet();
                    throw new RuntimeException();
                })
                .attempts(4)
                .delay(Duration.ofMillis(0))
                .run();

        assertThat("Counter should reach 3 after retries", counter.get(), is(3));
        assertThat("Execution result should be true", result, is(true));
    }

    @Test
    public void retry4TimesReturnNull() {
        AtomicInteger counter = new AtomicInteger(0);

        Boolean result = RetryExecutor
                .call(() -> {
                    if (counter.get() == 3) {
                        return true;
                    }
                    counter.incrementAndGet();
                    return null;
                })
                .attempts(4)
                .delay(Duration.ofMillis(0))
                .run();

        assertThat("Counter should reach 3 after retries", counter.get(), is(3));
        assertThat("Execution result should be true", result, is(true));
    }

    @Test
    public void doOnSuccessful() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger attemptReference = new AtomicInteger(0);
        AtomicBoolean resultReference = new AtomicBoolean(false);

        Boolean run = RetryExecutor
                .call(() -> {
                    if (counter.get() == 3) {
                        return true;
                    }
                    counter.incrementAndGet();
                    return null;
                })
                .attempts(4)
                .doOnSuccessful((attempt, result) -> {
                    attemptReference.set(attempt);
                    resultReference.set(result);
                })
                .delay(Duration.ofMillis(0))
                .run();

        assertThat("Counter should reach 3 after retries", counter.get(), is(3));
        assertThat("Successful attempt passed to callback should be 4", attemptReference.get(), is(4));
        assertThat("Successful result passed to callback should be true", resultReference.get(), is(true));
        assertThat("Execution result should be true", run, is(true));
    }

    @Test
    public void delay() {
        List<Long> timestamps = new ArrayList<>();
        int attempts = 3;
        Duration delay = Duration.ofMillis(25);

        Boolean result = RetryExecutor
                .call(() -> {
                    timestamps.add(System.nanoTime());
                    if (timestamps.size() == attempts) {
                        return true;
                    }
                    return null;
                })
                .attempts(attempts)
                .delay(delay)
                .run();

        assertThat("Execution result should be true", result, is(true));
        assertThat("Timestamps recorded count should match attempts", timestamps.size(), is(attempts));

        for (int i = 0; i < timestamps.size() - 1; i++) {
            long diffNanos = timestamps.get(i + 1) - timestamps.get(i);
            long diffMillis = TimeUnit.NANOSECONDS.toMillis(diffNanos);

            assertThat(
                    "Time difference between attempts should be at least configured delay",
                    diffMillis >= delay.toMillis(),
                    is(true)
            );
        }
    }

    @Test
    public void doOnError() {
        AtomicInteger counter = new AtomicInteger(0);

        AtomicReference<List<Integer>> attemptReference = new AtomicReference<>(new ArrayList<>());
        AtomicReference<List<Exception>> exceptionReference = new AtomicReference<>(new ArrayList<>());

        Boolean run = RetryExecutor
                .call(() -> {
                    if (counter.get() == 3) {
                        counter.incrementAndGet();
                        return true;
                    }
                    counter.incrementAndGet();
                    if (counter.get() - 1 == 1) {
                        throw new RuntimeException("test message " + counter.get());
                    }
                    if (counter.get() - 1 == 2) {
                        throw new IOException("test message " + counter.get());
                    }
                    throw new IllegalArgumentException("test message " + counter.get());
                })
                .doOnError((integer, exception) -> {
                    attemptReference.updateAndGet(integers -> {
                        integers.add(integer);
                        return integers;
                    });
                    exceptionReference.updateAndGet(exceptions -> {
                        exceptions.add(exception);
                        return exceptions;
                    });
                })
                .attempts(4)
                .delay(Duration.ofMillis(0))
                .run();

        assertThat("Counter should reach 4 attempts", counter.get(), is(4));
        assertThat("Execution result should be true", run, is(true));

        assertThat("doOnError should be called 3 times for failed attempts", attemptReference.get().size(), is(3));
        assertThat("Recorded attempt numbers in doOnError should be 1, 2, 3", attemptReference.get(), hasItems(1, 2, 3));

        List<Class<?>> list = (List) exceptionReference.get().stream().map(Object::getClass).toList();
        assertThat(
                "Caught exception types should match thrown types",
                list,
                hasItems(RuntimeException.class, IOException.class, IllegalArgumentException.class)
        );

        assertThat(
                "Caught exception messages should match thrown messages",
                exceptionReference.get().stream().map(Throwable::getMessage).toList(),
                hasItems("test message 1", "test message 2", "test message 3")
        );
    }

    @Test
    public void exceptionIfRetryReturnFalse() {
        AtomicInteger counter = new AtomicInteger(0);

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> RetryExecutor
                        .call(() -> {
                            counter.incrementAndGet();
                            if (counter.get() == 2) {
                                throw new IllegalArgumentException();
                            }
                            return null;
                        })
                        .retryIf(it -> {
                            if (it.exception() instanceof IllegalArgumentException) {
                                return false;
                            }
                            return it.exception() != null || it.result() == null;
                        })
                        .delay(Duration.ofMillis(0))
                        .run()
        );

        assertThat("Thrown exception should be IllegalArgumentException", e.getClass(), is(IllegalArgumentException.class));
        assertThat("Task execution count should be 2", counter.get(), is(2));
    }

    @Test
    public void callTimeoutNegative() {
        AtomicInteger callCounter = new AtomicInteger(0);
        AtomicBoolean exit = new AtomicBoolean(false);
        List<Exception> exceptions = new ArrayList<>();

        long start = System.currentTimeMillis();
        assertThrows(RetryExecutor.RetryExecutorException.class, () ->
                RetryExecutor
                        .call(() -> {
                            callCounter.incrementAndGet();
                            Thread.sleep(1000);
                            exit.set(true);
                            return true;
                        })
                        .delay(Duration.ofMillis(0))
                        .attempts(10)
                        .callTimeout(Duration.ofMillis(50))
                        .doOnError((integer, e) -> exceptions.add(e))
                        .run()
        );
        long executionTime = System.currentTimeMillis() - start;

        assertThat("Total execution time should take at least 500ms due to timeouts", executionTime >= 500, is(true));
        assertThat("Call count should reach max attempts of 10", callCounter.get(), is(10));
        assertThat("Execution inside call should not complete past sleep", exit.get(), is(false));
        assertThat("Number of caught errors should equal max attempts", exceptions.size(), is(10));
        assertThat("All collected exceptions should be TimeoutExceptions",
                exceptions.stream().allMatch(it -> it instanceof TimeoutException),
                is(true)
        );
    }

    @Test
    public void callTimeout() {
        AtomicInteger callCounter = new AtomicInteger(0);
        AtomicInteger exitCounter = new AtomicInteger();
        List<Exception> exceptions = new ArrayList<>();

        long start = System.currentTimeMillis();
        boolean result = RetryExecutor
                .call(() -> {
                    callCounter.incrementAndGet();
                    if (callCounter.get() != 5) {
                        Thread.sleep(1000);
                    }
                    exitCounter.incrementAndGet();
                    return true;
                })
                .attempts(10)
                .delay(Duration.ofMillis(0))
                .callTimeout(Duration.ofMillis(50))
                .doOnError((integer, e) -> exceptions.add(e))
                .run();
        long executionTime = System.currentTimeMillis() - start;

        assertThat("Total execution time should reflect timeouts", executionTime >= 200, is(true));
        assertThat("Execution result should be true", result, is(true));
        assertThat("Call count should reach 5 attempts before succeeding", callCounter.get(), is(5));
        assertThat("Exit counter should be 1 after successful run", exitCounter.get(), is(1));
        assertThat("Number of exceptions should match failed attempt count", exceptions.size(), is(4));
        assertThat("All recorded exceptions before success should be TimeoutExceptions",
                exceptions.stream().allMatch(it -> it instanceof TimeoutException),
                is(true)
        );
    }

    @Test
    public void doNotRetryInterruptedException() {
        AtomicInteger callCounter = new AtomicInteger(0);

        assertThrows(InterruptedException.class, () ->
                RetryExecutor
                        .call(() -> {
                            if (callCounter.get() <= 5) {
                                callCounter.incrementAndGet();
                                return 1;
                            }
                            throw new InterruptedException();
                        })
                        .delay(Duration.ofMillis(0))
                        .attempts(10)
                        .retryIf(it -> true)
                        .run()
        );

        assertThat("Call counter should be 6 when InterruptedException stops retries", callCounter.get(), is(6));
    }

    @Test
    public void doNotRetryInterruptedFlag() {
        AtomicInteger callCounter = new AtomicInteger(0);

        assertThrows(InterruptedException.class, () ->
                RetryExecutor
                        .call(() -> {
                            if (callCounter.get() <= 5) {
                                callCounter.incrementAndGet();
                                return 1;
                            }
                            Thread.currentThread().interrupt();
                            return 1;
                        })
                        .delay(Duration.ofMillis(0))
                        .attempts(10)
                        .retryIf(it -> true)
                        .run()
        );

        assertThat("Call counter should be 6 when interrupted status stops retries", callCounter.get(), is(6));
    }

    @Test
    public void doNotRetryCauseInterruptedException() {
        AtomicInteger callCounter = new AtomicInteger(0);

        UnsupportedOperationException e = assertThrows(
                UnsupportedOperationException.class,
                () -> RetryExecutor
                        .call(() -> {
                            if (callCounter.get() <= 5) {
                                callCounter.incrementAndGet();
                                return 1;
                            }
                            throw new UnsupportedOperationException(new InterruptedException());
                        })
                        .delay(Duration.ofMillis(0))
                        .attempts(10)
                        .retryIf(it -> true)
                        .run()
        );

        assertThat("Root cause should be InterruptedException", e.getCause(), isA(InterruptedException.class));
        assertThat("Call counter should be 6 when cause InterruptedException stops retries", callCounter.get(), is(6));
    }

    @Test
    public void doNotRetryClosedChannelException() {
        AtomicInteger callCounter = new AtomicInteger(0);

        assertThrows(ClosedChannelException.class, () ->
                RetryExecutor
                        .call(() -> {
                            if (callCounter.get() <= 5) {
                                callCounter.incrementAndGet();
                                return 1;
                            }
                            throw new ClosedChannelException();
                        })
                        .delay(Duration.ofMillis(0))
                        .attempts(10)
                        .retryIf(it -> true)
                        .run()
        );

        assertThat("Call counter should be 6 when ClosedChannelException stops retries", callCounter.get(), is(6));
    }

    @Test
    public void doNotRetryCauseClosedChannelException() {
        AtomicInteger callCounter = new AtomicInteger(0);

        UnsupportedOperationException e = assertThrows(
                UnsupportedOperationException.class,
                () -> RetryExecutor
                        .call(() -> {
                            if (callCounter.get() <= 5) {
                                callCounter.incrementAndGet();
                                return 1;
                            }
                            throw new UnsupportedOperationException(new ClosedChannelException());
                        })
                        .delay(Duration.ofMillis(0))
                        .attempts(10)
                        .retryIf(it -> true)
                        .run()
        );

        assertThat("Root cause should be ClosedChannelException", e.getCause(), isA(ClosedChannelException.class));
        assertThat("Call counter should be 6 when cause ClosedChannelException stops retries", callCounter.get(), is(6));
    }

    @Test
    void shouldPreserveInterruptStatusWhenThreadIsInterrupted() throws Exception {
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskFinished = new CountDownLatch(1);

        AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        AtomicReference<Throwable> caughtException = new AtomicReference<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                try {
                    RetryExecutor
                            .call(() -> {
                                taskStarted.countDown();
                                Thread.sleep(5000);
                                throw new RuntimeException("Simulated error");
                            })
                            .run();
                } catch (Throwable e) {
                    wasInterrupted.set(Thread.currentThread().isInterrupted());
                    caughtException.set(e);
                } finally {
                    taskFinished.countDown();
                }
            });

            assertThat("Task did not start in time", taskStarted.await(2, TimeUnit.SECONDS), is(true));

            executor.shutdownNow();

            assertThat("Task did not complete after interruption", taskFinished.await(2, TimeUnit.SECONDS), is(true));

            assertThat("Expected exception to be thrown", caughtException.get(), is(notNullValue()));

            assertThat("Exception chain should contain InterruptedException",
                    CommonUtils.checkThrowable(
                            caughtException.get(),
                            InterruptedException.class
                    ),
                    is(true)
            );

            assertThat("isInterrupted() flag should be true", wasInterrupted.get(), is(true));
        }
    }

    @Test
    void shouldThrowInterruptedExceptionAndPreserveFlagWhenInterrupted() throws Exception {
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskFinished = new CountDownLatch(1);

        AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        AtomicReference<Throwable> caughtException = new AtomicReference<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                try {
                    RetryExecutor
                            .call(() -> {
                                taskStarted.countDown();
                                throw new RuntimeException("Simulated error to trigger retry delay");
                            })
                            .delay(Duration.ofSeconds(5000))
                            .run();
                } catch (Throwable e) {
                    wasInterrupted.set(Thread.currentThread().isInterrupted());
                    caughtException.set(e);
                } finally {
                    taskFinished.countDown();
                }
            });

            assertThat("Task did not start in time", taskStarted.await(2, TimeUnit.SECONDS), is(true));

            executor.shutdownNow();

            assertThat("Task did not complete after interruption", taskFinished.await(2, TimeUnit.SECONDS), is(true));

            assertThat("Expected exception to be thrown", caughtException.get(), is(notNullValue()));

            assertThat(
                    "Exception chain should contain InterruptedException",
                    CommonUtils.checkThrowable(
                            caughtException.get(),
                            InterruptedException.class
                    ),
                    is(true)
            );

            assertThat("isInterrupted() flag should be true", wasInterrupted.get(), is(true));
        }
    }

    @Test
    void shouldNotRetryWhenThreadIsInterruptedBeforeExecution() {
        AtomicInteger callCounter = new AtomicInteger(0);

        Thread.currentThread().interrupt();

        Throwable thrown = assertThrows(Throwable.class, () ->
                RetryExecutor
                        .call(() -> {
                            callCounter.incrementAndGet();
                            return true;
                        })
                        .attempts(5)
                        .delay(Duration.ofMillis(100))
                        .run()
        );

        assertThat("Should be or wrap InterruptedException",
                CommonUtils.checkThrowable(thrown, InterruptedException.class), is(true));
        assertThat("Call counter should remain 0", callCounter.get(), is(0));
        assertThat("Thread interrupt flag should remain true", Thread.currentThread().isInterrupted(), is(true));
    }

    @Test
    void shouldCallDoOnErrorAndPreserveFlagOnInterruption() throws Exception {
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskFinished = new CountDownLatch(1);

        AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        List<Exception> errorsCaughtInDoOnError = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                try {
                    RetryExecutor
                            .call(() -> {
                                taskStarted.countDown();
                                throw new RuntimeException("Error before delay");
                            })
                            .delay(Duration.ofSeconds(5000))
                            .doOnError((attempt, ex) -> errorsCaughtInDoOnError.add(ex))
                            .run();
                } catch (Throwable e) {
                    wasInterrupted.set(Thread.currentThread().isInterrupted());
                } finally {
                    taskFinished.countDown();
                }
            });

            assertThat("Task did not start in time", taskStarted.await(2, TimeUnit.SECONDS), is(true));

            executor.shutdownNow();

            assertThat("Task did not complete after interruption", taskFinished.await(2, TimeUnit.SECONDS), is(true));

            assertThat("First error before delay should be caught in doOnError", errorsCaughtInDoOnError.size(), is(1));
            assertThat("isInterrupted() flag should be true", wasInterrupted.get(), is(true));
        }
    }
}