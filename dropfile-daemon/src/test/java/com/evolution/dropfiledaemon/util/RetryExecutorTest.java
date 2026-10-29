package com.evolution.dropfiledaemon.util;

import com.evolution.dropfile.common.ThrowableUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RetryExecutorTest {

    @Test
    public void retryIfNotNullValue() throws Exception {
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
    public void retryIfException() throws Exception {
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

        ExecutionException e = assertThrows(
                ExecutionException.class,
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

        List<Throwable> allThrownExceptions = new ArrayList<>();
        allThrownExceptions.add(e);
        Collections.addAll(allThrownExceptions, e.getSuppressed());

        assertThat("Should contain 3 recorded exceptions in chain (1 primary + 2 suppressed)", allThrownExceptions.size(), is(3));
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
                exceptionRetryIf.get().stream().map(it -> it.getCause()).map(Throwable::getMessage).toList(),
                hasItems("test message 1", "test message 2", "test message 3")
        );
    }

    @Test
    public void failsIfResultIsNull() {
        AtomicBoolean called = new AtomicBoolean(false);

        ExecutionException e = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            called.set(true);
                            return null;
                        })
                        .attempts(1)
                        .delay(Duration.ZERO)
                        .run()
        );

        assertThat("Suppressed exceptions list should be empty when result is null", e.getSuppressed().length, is(0));
        assertThat("Task should have been called once", called.get(), is(true));
    }

    @Test
    public void failsIfExceptionIsThrown() {
        AtomicBoolean called = new AtomicBoolean(false);

        ExecutionException e = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            called.set(true);
                            throw new RuntimeException();
                        })
                        .attempts(1)
                        .delay(Duration.ZERO)
                        .run()
        );

        assertThat(e.getCause(), isA(RuntimeException.class));
        assertThat("Suppressed exceptions list should be empty for single attempt", e.getSuppressed().length, is(0));
        assertThat("Task should have been called once", called.get(), is(true));
    }

    @Test
    public void throwsDuringCall() {
        AtomicBoolean called = new AtomicBoolean(false);

        ExecutionException exception = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            called.set(true);
                            throw new RuntimeException("test message");
                        })
                        .attempts(1)
                        .delay(Duration.ofMillis(0))
                        .run()
        );

        assertThat("Exception message should match the thrown message", exception.getCause().getMessage(), is("test message"));
        assertThat("Task should have been called once", called.get(), is(true));
    }

    @Test
    public void retry4Times() throws Exception {
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
    public void retry4TimesReturnNull() throws Exception {
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
    public void doOnSuccessful() throws Exception {
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
    public void delay() throws Exception {
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
    public void doOnError() throws Exception {
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

        @SuppressWarnings("unchecked")
        List<Class<?>> list = (List) exceptionReference.get().stream().map(it -> it.getCause()).map(Object::getClass).toList();
        assertThat(
                "Caught exception types should match thrown types",
                list,
                hasItems(RuntimeException.class, IOException.class, IllegalArgumentException.class)
        );

        assertThat(
                "Caught exception messages should match thrown messages",
                exceptionReference.get().stream().map(it -> it.getCause()).map(Throwable::getMessage).toList(),
                hasItems("test message 1", "test message 2", "test message 3")
        );
    }

    @Test
    public void exceptionIfRetryReturnFalse() {
        AtomicInteger counter = new AtomicInteger(0);

        ExecutionException e = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            counter.incrementAndGet();
                            if (counter.get() == 2) {
                                throw new IllegalArgumentException();
                            }
                            return null;
                        })
                        .retryIf(it -> {
                            if (it.exception() != null
                                    && it.exception().getCause() instanceof IllegalArgumentException) {
                                return false;
                            }
                            return it.exception() != null || it.result() == null;
                        })
                        .delay(Duration.ZERO)
                        .run()
        );

        assertThat("Thrown exception should be IllegalArgumentException", e.getCause().getClass(), is(IllegalArgumentException.class));
        assertThat("Task execution count should be 2", counter.get(), is(2));
    }

    @Test
    public void callTimeoutFailsAllAttempts() {
        AtomicInteger callCounter = new AtomicInteger(0);
        AtomicBoolean exit = new AtomicBoolean(false);
        List<Exception> exceptions = new ArrayList<>();

        long start = System.currentTimeMillis();
        assertThrows(ExecutionException.class, () ->
                RetryExecutor
                        .call(() -> {
                            callCounter.incrementAndGet();
                            Thread.sleep(1000);
                            exit.set(true);
                            return true;
                        })
                        .delay(Duration.ZERO)
                        .attempts(10)
                        .callTimeout(Duration.ofMillis(50))
                        .doOnError((_, e) -> exceptions.add(e))
                        .run()
        );
        long executionTime = System.currentTimeMillis() - start;

        assertThat("Total execution time should reflect ten 50ms timeouts", executionTime >= 400, is(true));
        assertThat("Call count should reach max attempts of 10", callCounter.get(), is(10));
        assertThat("Execution inside call should not complete past sleep", exit.get(), is(false));
        assertThat("Number of caught errors should equal max attempts", exceptions.size(), is(10));
        assertThat("All collected exceptions should be TimeoutExceptions",
                exceptions.stream().map(it -> it.getCause()).allMatch(it -> it instanceof TimeoutException),
                is(true)
        );
    }

    @Test
    public void callTimeout() throws Exception {
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
                .delay(Duration.ZERO)
                .callTimeout(Duration.ofMillis(50))
                .doOnError((_, e) -> exceptions.add(e))
                .run();
        long executionTime = System.currentTimeMillis() - start;

        assertThat("Total execution time should reflect four 50ms timeouts", executionTime >= 150, is(true));
        assertThat("Execution result should be true", result, is(true));
        assertThat("Call count should reach 5 attempts before succeeding", callCounter.get(), is(5));
        assertThat("Exit counter should be 1 after successful run", exitCounter.get(), is(1));
        assertThat("Number of exceptions should match failed attempt count", exceptions.size(), is(4));
        assertThat("All recorded exceptions before success should be TimeoutExceptions",
                exceptions.stream().map(it -> it.getCause()).allMatch(it -> it instanceof TimeoutException),
                is(true)
        );
    }

    @Test
    public void doNotRetryInterruptedException() {
        AtomicInteger callCounter = new AtomicInteger(0);

        AtomicReference<InterruptedException> interruptedExceptionAtomicReference = new AtomicReference<>();

        ExecutionException executionException = assertThrows(ExecutionException.class, () ->
                RetryExecutor
                        .call(() -> {
                            if (callCounter.get() <= 5) {
                                callCounter.incrementAndGet();
                                return 1;
                            }
                            InterruptedException interruptedException = new InterruptedException();
                            interruptedExceptionAtomicReference.set(interruptedException);
                            throw interruptedException;
                        })
                        .delay(Duration.ofMillis(0))
                        .attempts(10)
                        .retryIf(_ -> true)
                        .run()
        );

        assertThat(Thread.currentThread().isInterrupted(), is(true));
        assertThat("Root cause should be InterruptedException", executionException.getCause(), isA(InterruptedException.class));
        assertThat("Root cause should be same instance of InterruptedException",
                executionException.getCause(),
                sameInstance(interruptedExceptionAtomicReference.get())
        );
        assertThat("Call counter should be 6 when InterruptedException stops retries", callCounter.get(), is(6));
    }

    @Test
    public void doNotRetryInterruptedFlag() {
        AtomicInteger callCounter = new AtomicInteger(0);

        ExecutionException executionException = assertThrows(ExecutionException.class, () ->
                RetryExecutor
                        .call(() -> {
                            if (callCounter.get() <= 5) {
                                callCounter.incrementAndGet();
                                return 1;
                            }
                            Thread.currentThread().interrupt();
                            return 1;
                        })
                        .delay(Duration.ZERO)
                        .attempts(10)
                        .retryIf(_ -> true)
                        .run()
        );

        assertThat(Thread.currentThread().isInterrupted(), is(true));
        assertThat("Root cause should be InterruptedException", executionException.getCause(), isA(InterruptedException.class));
        assertThat("Call counter should be 6 when interrupted status stops retries", callCounter.get(), is(6));
    }

    @Test
    public void doNotRetryInterruptedFlagWithCallTimeout() {
        AtomicInteger callCounter = new AtomicInteger(0);

        ExecutionException executionException = assertThrows(ExecutionException.class, () ->
                RetryExecutor
                        .call(() -> {
                            if (callCounter.get() <= 5) {
                                callCounter.incrementAndGet();
                                return 1;
                            }
                            Thread.currentThread().interrupt();
                            return 1;
                        })
                        .delay(Duration.ofMillis(1))
                        .callTimeout(Duration.ofSeconds(100))
                        .attempts(10)
                        .retryIf(_ -> true)
                        .run()
        );

        assertThat(Thread.currentThread().isInterrupted(), is(true));
        assertThat("Root cause should be InterruptedException", executionException.getCause(), isA(InterruptedException.class));
        assertThat("Call counter should be 6 when interrupted status stops retries", callCounter.get(), is(6));
    }

    @Test
    public void doNotRetryCauseInterruptedException() {
        AtomicInteger callCounter = new AtomicInteger(0);

        ExecutionException e = assertThrows(
                ExecutionException.class,
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

        assertThat("Root cause should be InterruptedException", e.getCause(), isA(UnsupportedOperationException.class));
        assertThat("Root cause should be InterruptedException", e.getCause().getCause(), isA(InterruptedException.class));
        assertThat("Call counter should be 6 when cause InterruptedException stops retries", callCounter.get(), is(6));
    }

    @Test
    public void doNotRetryClosedChannelException() {
        AtomicInteger callCounter = new AtomicInteger(0);

        ExecutionException executionException = assertThrows(
                ExecutionException.class,
                () ->
                        RetryExecutor
                                .call(() -> {
                                    callCounter.incrementAndGet();
                                    if (callCounter.get() <= 5) {
                                        return 1;
                                    }
                                    throw new ClosedChannelException();
                                })
                                .delay(Duration.ofMillis(1))
                                .attempts(10)
                                .retryIf(_ -> true)
                                .run()
        );

        assertThat(executionException.getCause(), isA(ClosedChannelException.class));
        assertThat("Call counter should be 6 when ClosedChannelException stops retries", callCounter.get(), is(6));
    }

    @Test
    public void doNotRetryCauseClosedChannelException() {
        AtomicInteger callCounter = new AtomicInteger(0);

        ExecutionException e = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            if (callCounter.get() <= 5) {
                                callCounter.incrementAndGet();
                                return 1;
                            }
                            throw new UnsupportedOperationException(new ClosedChannelException());
                        })
                        .delay(Duration.ZERO)
                        .attempts(10)
                        .retryIf(_ -> true)
                        .run()
        );

        assertThat(e.getCause(), isA(UnsupportedOperationException.class));
        assertThat("Root cause should be ClosedChannelException", e.getCause().getCause(), isA(ClosedChannelException.class));
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
                    ThrowableUtils.checkThrowable(
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
                    ThrowableUtils.checkThrowable(
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

        ExecutionException thrown = assertThrows(ExecutionException.class, () ->
                RetryExecutor
                        .call(() -> {
                            callCounter.incrementAndGet();
                            return true;
                        })
                        .attempts(5)
                        .delay(Duration.ofMillis(100))
                        .run()
        );

        assertThat("Should be or wrap InterruptedException", ThrowableUtils.checkThrowable(thrown, InterruptedException.class), is(true));
        assertThat("Call counter should remain 0", callCounter.get(), is(0));
        assertThat("Thread interrupt flag should remain true", Thread.currentThread().isInterrupted(), is(true));
    }

    @Test
    void shouldCallDoOnErrorAndPreserveFlagOnInterruption() throws Exception {
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskFinished = new CountDownLatch(1);

        AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        List<Exception> errorsCaughtInDoOnError = new ArrayList<>();
        CountDownLatch errorCallbackCalled = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                try {
                    RetryExecutor
                            .call(() -> {
                                taskStarted.countDown();
                                throw new RuntimeException("Error before delay");
                            })
                            .delay(Duration.ofSeconds(5000))
                            .doOnError((_, ex) -> {
                                errorsCaughtInDoOnError.add(ex);
                                errorCallbackCalled.countDown();
                            })
                            .run();
                } catch (Throwable e) {
                    wasInterrupted.set(Thread.currentThread().isInterrupted());
                } finally {
                    taskFinished.countDown();
                }
            });

            assertThat("Task did not start in time", taskStarted.await(2, TimeUnit.SECONDS), is(true));
            assertThat(
                    "Error callback did not run before interruption",
                    errorCallbackCalled.await(2, TimeUnit.SECONDS),
                    is(true)
            );

            executor.shutdownNow();

            assertThat("Task did not complete after interruption", taskFinished.await(2, TimeUnit.SECONDS), is(true));

            assertThat("First error before delay should be caught in doOnError", errorsCaughtInDoOnError.size(), is(1));
            assertThat("isInterrupted() flag should be true", wasInterrupted.get(), is(true));
        }
    }

    @Test
    void shouldInterruptActivePollingLoopOnTimeout() throws Exception {
        AtomicBoolean loopExited = new AtomicBoolean(false);
        CountDownLatch taskStarted = new CountDownLatch(1);

        ExecutionException executionException = assertThrows(ExecutionException.class, () ->
                RetryExecutor
                        .call(() -> {
                            taskStarted.countDown();
                            while (!Thread.currentThread().isInterrupted()) {

                            }
                            loopExited.set(true);
                            throw new RuntimeException("Exited loop via interrupt");
                        })
                        .callTimeout(Duration.ofMillis(100))
                        .attempts(1)
                        .run()
        );

        assertThat(executionException.getCause(), isA(TimeoutException.class));

        assertThat("Task should have started", taskStarted.await(1, TimeUnit.SECONDS), is(true));

        assertThat(
                "Task should exit the while-loop when interrupted by timeout",
                loopExited.get(),
                is(true)
        );
    }

    @Test
    void shouldNotSleepAfterLastFailedAttempt() {
        long start = System.currentTimeMillis();

        assertThrows(ExecutionException.class, () ->
                RetryExecutor
                        .call(() -> {
                            throw new RuntimeException("Instant failure");
                        })
                        .attempts(1)
                        .delay(Duration.ofSeconds(100))
                        .run()
        );

        long executionTime = System.currentTimeMillis() - start;

        assertThat(
                "Execution should fail immediately without delay after the last attempt",
                executionTime < 500,
                is(true)
        );
    }

    @Test
    void shouldCancelBackgroundTaskOnTimeout() throws Exception {
        CountDownLatch taskStarted = new CountDownLatch(1);
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        AtomicBoolean completedPastTimeout = new AtomicBoolean(false);

        assertThrows(ExecutionException.class, () ->
                RetryExecutor
                        .call(() -> {
                            taskStarted.countDown();
                            try {
                                Thread.sleep(500);
                                completedPastTimeout.set(true);
                            } catch (InterruptedException e) {
                                wasInterrupted.set(true);
                                throw e;
                            }
                            return true;
                        })
                        .attempts(1)
                        .callTimeout(Duration.ofMillis(100))
                        .run()
        );

        assertThat("Background task should be interrupted on timeout", wasInterrupted.get(), is(true));
        assertThat("Background task should not finish work after timeout", completedPastTimeout.get(), is(false));
    }

    @Test
    void shouldInterruptTaskWhenCallTimeoutExpires() throws Exception {
        CountDownLatch taskStarted = new CountDownLatch(1);
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        assertThrows(ExecutionException.class, () ->
                RetryExecutor
                        .call(() -> {
                            taskStarted.countDown();
                            while (!Thread.currentThread().isInterrupted()) {

                            }
                            wasInterrupted.set(true);
                            return true;
                        })
                        .attempts(1)
                        .callTimeout(Duration.ofMillis(100))
                        .run()
        );

        assertThat("Task should have started", taskStarted.await(1, TimeUnit.SECONDS), is(true));
        assertThat("Task should be interrupted", wasInterrupted.get(), is(true));
    }

    @Test
    void shouldNotFailWithSelfSuppressionWhenThrowingSingletonException() {
        RuntimeException singletonException = new RuntimeException("I am a singleton");
        AtomicInteger callCounter = new AtomicInteger(0);

        ExecutionException e = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            callCounter.incrementAndGet();
                            throw singletonException;
                        })
                        .attempts(3)
                        .delay(Duration.ZERO)
                        .run()
        );

        assertThat("Task should be called 3 times", callCounter.get(), is(3));
        assertThat("Should be the exact singleton instance", e.getCause(), sameInstance(singletonException));
        assertThat("Should not contain self-suppressed exceptions", e.getSuppressed().length, is(0));
    }

    @Test
    void shouldNotBreakRetryLoopWhenExceptionIsThrownInDoOnError() {
        AtomicInteger callCounter = new AtomicInteger(0);

        ExecutionException e = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            callCounter.incrementAndGet();
                            throw new IOException("Original error");
                        })
                        .attempts(3)
                        .delay(Duration.ofMillis(0))
                        .doOnError((_, _) -> {
                            throw new RuntimeException("Error in callback");
                        })
                        .run()
        );

        assertThat(e.getCause(), isA(IOException.class));
        assertThat("Task should be called 3 times despite callback errors", callCounter.get(), is(3));
        assertThat("The exception thrown should be the original one", e.getCause().getMessage(), is("Original error"));
        assertThat("Should contain suppressed exceptions for attempts 2 and 3", e.getSuppressed().length, is(2));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnInvalidAttempts() {
        assertThrows(IllegalArgumentException.class, () ->
                RetryExecutor.call(() -> true).attempts(0).run()
        );

        assertThrows(IllegalArgumentException.class, () ->
                RetryExecutor.call(() -> true).attempts(-5).run()
        );
    }

    @Test
    void shouldThrowNullPointerExceptionOnNullDelay() {
        assertThrows(NullPointerException.class, () ->
                RetryExecutor.call(() -> true).delay(null)
        );
    }

    @Test
    void mixedExceptionsShouldBeAggregatedCorrectly() {
        AtomicInteger counter = new AtomicInteger(0);

        ExecutionException e = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            int current = counter.incrementAndGet();
                            if (current == 1) {
                                throw new TimeoutException("First timeout");
                            }
                            if (current == 2) {
                                throw new IOException("Network glitch");
                            }
                            throw new IllegalStateException("Final failure");
                        })
                        .attempts(3)
                        .delay(Duration.ofMillis(0))
                        .run()
        );

        assertThat(e.getCause(), instanceOf(TimeoutException.class));
        assertThat(e.getSuppressed().length, is(2));
        assertThat(e.getSuppressed()[0], instanceOf(IOException.class));
        assertThat(e.getSuppressed()[1], instanceOf(IllegalStateException.class));
    }

    @Test
    void shouldNotRetryAndPropagateOriginalErrorWithoutCallTimeout() {
        AtomicInteger callCounter = new AtomicInteger(0);
        AtomicInteger doOnErrorCounter = new AtomicInteger(0);
        Error original = new Error("fatal");

        ExecutionException executionException = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            callCounter.incrementAndGet();
                            throw original;
                        })
                        .attempts(5)
                        .delay(Duration.ZERO)
                        .retryIf(it -> true)
                        .doOnError((_, _) -> doOnErrorCounter.incrementAndGet())
                        .run()
        );

        assertThat(executionException.getCause(), sameInstance(original));
        assertThat(executionException.getSuppressed().length, is(0));
        assertThat(callCounter.get(), is(1));
        assertThat(doOnErrorCounter.get(), is(0));
    }

    @Test
    void shouldNotRetryAndPropagateOriginalErrorWithCallTimeout() {
        AtomicInteger callCounter = new AtomicInteger(0);
        AtomicInteger doOnErrorCounter = new AtomicInteger(0);
        Error original = new Error("fatal with timeout path");

        ExecutionException executionException = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            callCounter.incrementAndGet();
                            throw original;
                        })
                        .attempts(5)
                        .delay(Duration.ZERO)
                        .callTimeout(Duration.ofSeconds(100))
                        .retryIf(it -> true)
                        .doOnError((_, _) -> doOnErrorCounter.incrementAndGet())
                        .run()
        );

        assertThat(executionException.getCause(), sameInstance(original));
        assertThat(executionException.getSuppressed().length, is(0));
        assertThat(callCounter.get(), is(1));
        assertThat(doOnErrorCounter.get(), is(0));
    }

    @Test
    void callTimeoutZeroShouldDisableTimeout() throws Exception {
        Boolean result = RetryExecutor
                .call(() -> {
                    Thread.sleep(100);
                    return true;
                })
                .attempts(1)
                .delay(Duration.ZERO)
                .callTimeout(Duration.ZERO)
                .run();

        assertThat("Call should complete despite zero timeout", result, is(true));
    }

    @Test
    void callTimeoutNegativeShouldDisableTimeout() throws Exception {
        Boolean result = RetryExecutor
                .call(() -> {
                    Thread.sleep(100);
                    return true;
                })
                .attempts(1)
                .delay(Duration.ZERO)
                .callTimeout(Duration.ofMillis(-1))
                .run();

        assertThat("Call should complete despite negative timeout", result, is(true));
    }

    @Test
    void callTimeoutZeroShouldPreserveExceptionContract() {
        IOException original = new IOException("Original exception");

        ExecutionException thrown = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            throw original;
                        })
                        .attempts(1)
                        .delay(Duration.ZERO)
                        .callTimeout(Duration.ZERO)
                        .run()
        );

        assertThat("Original exception should be preserved as cause",
                thrown.getCause(), sameInstance(original));
    }

    @Test
    void callTimeoutNegativeShouldPreserveExceptionContract() {
        IOException original = new IOException("Original exception");

        ExecutionException thrown = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            throw original;
                        })
                        .attempts(1)
                        .delay(Duration.ZERO)
                        .callTimeout(Duration.ofMillis(-1))
                        .run()
        );

        assertThat("Original exception should be preserved as cause",
                thrown.getCause(), sameInstance(original));
    }

    @Test
    void shouldReturnResultWhenRetryIfThrowsException() throws Exception {
        AtomicInteger callCounter = new AtomicInteger(0);

        Boolean result = RetryExecutor
                .call(() -> {
                    callCounter.incrementAndGet();
                    return true;
                })
                .attempts(3)
                .delay(Duration.ZERO)
                .retryIf(it -> {
                    throw new IllegalStateException("retryIf failed");
                })
                .run();

        assertThat("Result should be returned", result, is(true));
        assertThat("Callable should be invoked only once", callCounter.get(), is(1));
    }

    @Test
    void shouldPropagateCallableExceptionWhenRetryIfThrowsException() {
        AtomicInteger callCounter = new AtomicInteger(0);
        RuntimeException original = new IllegalArgumentException("Callable failed");

        ExecutionException thrown = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            callCounter.incrementAndGet();
                            throw original;
                        })
                        .attempts(3)
                        .delay(Duration.ZERO)
                        .retryIf(_ -> {
                            throw new IllegalStateException("retryIf failed");
                        })
                        .run()
        );

        assertThat("Original callable exception should be propagated", thrown.getCause(), sameInstance(original));
        assertThat("Callable should not be retried", callCounter.get(), is(1));
    }

    @Test
    void shouldPropagateTimeoutExceptionFromCallable() {
        TimeoutException original = new TimeoutException("Callable timed out");

        ExecutionException thrown = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            throw original;
                        })
                        .attempts(1)
                        .callTimeout(Duration.ofSeconds(100))
                        .run()
        );

        assertThat("Original timeout exception should be propagated", thrown.getCause(), sameInstance(original));
        assertThat(thrown.getSuppressed().length, is(0));
    }

    @Test
    void shouldThrowTimeoutExceptionWhenCallTimeoutExpires() {
        ExecutionException executionException = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            Thread.sleep(120_000);
                            return true;
                        })
                        .attempts(1)
                        .callTimeout(Duration.ofMillis(50))
                        .run()
        );

        assertThat("TimeoutException should be propagated",
                executionException.getCause(), instanceOf(TimeoutException.class));
    }

    @Test
    void shouldWaitForCallableToFinishAfterTimeoutWhenItIgnoresInterruption() throws Exception {
        CountDownLatch callableStarted = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        AtomicBoolean release = new AtomicBoolean();

        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

        try {
            Future<Integer> result = executorService.submit(() ->
                    RetryExecutor
                            .call(() -> {
                                callableStarted.countDown();
                                while (!release.get()) {
                                    if (Thread.currentThread().isInterrupted()) {
                                        interrupted.countDown();
                                    }
                                    Thread.onSpinWait();
                                }
                                return 1;
                            })
                            .attempts(1)
                            .delay(Duration.ZERO)
                            .callTimeout(Duration.ofMillis(50))
                            .run()
            );

            try {
                assertThat(
                        "Callable should start",
                        callableStarted.await(5, TimeUnit.SECONDS),
                        is(true)
                );

                assertThat(
                        "Callable should receive interruption after timeout",
                        interrupted.await(5, TimeUnit.SECONDS),
                        is(true)
                );

                assertThrows(
                        TimeoutException.class,
                        () -> result.get(200, TimeUnit.MILLISECONDS)
                );
            } finally {
                release.set(true);
            }

            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> result.get(2, TimeUnit.SECONDS)
            );

            assertThat(
                    "Future covers ExecutionException into ExecutionException",
                    exception.getCause(),
                    instanceOf(ExecutionException.class)
            );

            assertThat(
                    "Original TimeoutException should be propagated",
                    exception.getCause().getCause(),
                    instanceOf(TimeoutException.class)
            );
        } finally {
            release.set(true);
            executorService.shutdownNow();
        }
    }


    @Test
    void shouldContinueWaitingWhenWaitingThreadIsInterrupted() throws Exception {
        CountDownLatch callableStarted = new CountDownLatch(1);
        CountDownLatch callableInterrupted = new CountDownLatch(1);
        CountDownLatch releaseCallable = new CountDownLatch(1);

        AtomicReference<Thread> retryThread = new AtomicReference<>();
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

        try {
            Future<Boolean> result = executorService.submit(() -> {
                Thread currentThread = Thread.currentThread();
                retryThread.set(currentThread);
                try {
                    RetryExecutor
                            .call(() -> {
                                callableStarted.countDown();

                                while (releaseCallable.getCount() > 0) {
                                    if (Thread.currentThread().isInterrupted()) {
                                        callableInterrupted.countDown();
                                    }
                                    Thread.onSpinWait();
                                }

                                return 1;
                            })
                            .attempts(1)
                            .delay(Duration.ZERO)
                            .callTimeout(Duration.ofMillis(100))
                            .run();
                    throw new AssertionError("Expected TimeoutException");
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof TimeoutException) {
                        return currentThread.isInterrupted();
                    }
                    throw new AssertionError("Expected TimeoutException");
                }
            });

            assertThat(
                    "Callable should start",
                    callableStarted.await(1, TimeUnit.SECONDS),
                    is(true)
            );

            assertThat(
                    "Callable should receive interruption after timeout",
                    callableInterrupted.await(1, TimeUnit.SECONDS),
                    is(true)
            );

            retryThread.get().interrupt();

            assertThrows(
                    TimeoutException.class,
                    () -> result.get(100, TimeUnit.MILLISECONDS)
            );

            releaseCallable.countDown();

            assertThat(
                    "Interrupt status should be restored after waiting",
                    result.get(1, TimeUnit.SECONDS),
                    is(true)
            );
        } finally {
            releaseCallable.countDown();
            executorService.shutdownNow();
        }
    }

    @Test
    void shouldAddInterruptedExceptionAsSuppressedWhenWaitingForCallableToFinish() throws Exception {
        CountDownLatch callableStarted = new CountDownLatch(1);
        CountDownLatch callableInterrupted = new CountDownLatch(1);
        CountDownLatch releaseCallable = new CountDownLatch(1);

        AtomicReference<Thread> retryThread = new AtomicReference<>();
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

        try {
            Future<?> result = executorService.submit(() -> {
                retryThread.set(Thread.currentThread());
                RetryExecutor
                        .call(() -> {
                            callableStarted.countDown();

                            while (releaseCallable.getCount() > 0) {
                                if (Thread.currentThread().isInterrupted()) {
                                    callableInterrupted.countDown();
                                }
                                Thread.onSpinWait();
                            }

                            return 1;
                        })
                        .attempts(1)
                        .delay(Duration.ZERO)
                        .callTimeout(Duration.ofMillis(100))
                        .run();
                return null;
            });

            assertThat(
                    "Callable should start",
                    callableStarted.await(1, TimeUnit.SECONDS),
                    is(true)
            );

            assertThat(
                    "Callable should receive interruption after timeout",
                    callableInterrupted.await(1, TimeUnit.SECONDS),
                    is(true)
            );

            retryThread.get().interrupt();

            assertThrows(
                    TimeoutException.class,
                    () -> result.get(100, TimeUnit.MILLISECONDS)
            );

            releaseCallable.countDown();

            ExecutionException thrown = assertThrows(
                    ExecutionException.class,
                    () -> result.get(1, TimeUnit.SECONDS)
            );

            assertThat(
                    "Future covers ExecutionException into ExecutionException",
                    thrown.getCause(),
                    instanceOf(ExecutionException.class)
            );

            assertThat(
                    "Original TimeoutException should be propagated",
                    thrown.getCause().getCause(),
                    instanceOf(TimeoutException.class)
            );

            assertThat(
                    "InterruptedException should be added as suppressed",
                    thrown.getCause().getSuppressed().length,
                    is(1)
            );

            assertThat(
                    "Suppressed exception should be InterruptedException",
                    thrown.getCause().getSuppressed()[0],
                    instanceOf(InterruptedException.class)
            );
        } finally {
            releaseCallable.countDown();
            executorService.shutdownNow();
        }
    }

    @Test
    void secondCallMustNotFinishFirstCall() throws Exception {
        CountDownLatch delegateStarted = new CountDownLatch(1);
        CountDownLatch releaseDelegate = new CountDownLatch(1);
        CountDownLatch delegateFinished = new CountDownLatch(1);

        CallableCancelable<Integer> callableCancelable = new CallableCancelable<>(
                () -> {
                    delegateStarted.countDown();
                    releaseDelegate.await();
                    return 42;
                },
                new CallableCancelable.SingleActionCountDownLatchCallableCancelableMiddleware(delegateFinished)
        );

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> firstCall = executor.submit(callableCancelable);
            try {
                assertThat("First delegate call should start",
                        delegateStarted.await(5, TimeUnit.SECONDS), is(true));

                assertThrows(IllegalStateException.class, callableCancelable::call);
                assertThat("Second call must not signal completion of the first call",
                        delegateFinished.getCount(), is(1L));
            } finally {
                releaseDelegate.countDown();
            }

            assertThat("First call result should be returned", firstCall.get(5, TimeUnit.SECONDS), is(42));
            assertThat("First call should signal completion",
                    delegateFinished.await(5, TimeUnit.SECONDS), is(true));
        }
    }

    @Test
    void negativeDelayShouldDisableDelay() throws Exception {
        AtomicInteger callCounter = new AtomicInteger();

        Boolean result = RetryExecutor
                .call(() -> callCounter.incrementAndGet() == 2 ? true : null)
                .attempts(2)
                .delay(Duration.ofMillis(-1))
                .run();

        assertThat("Callable should execute twice", callCounter.get(), is(2));
        assertThat("Second result should be returned", result, is(true));
    }

    @Test
    void shouldReturnResultWhenRetryIfThrowsError() throws Exception {
        AtomicInteger callCounter = new AtomicInteger();

        Boolean result = RetryExecutor
                .call(() -> {
                    callCounter.incrementAndGet();
                    return true;
                })
                .attempts(3)
                .delay(Duration.ZERO)
                .retryIf(_ -> {
                    throw new AssertionError("retryIf failed");
                })
                .run();

        assertThat("Result should be returned after retryIf failure", result, is(true));
        assertThat("Callable should be invoked only once", callCounter.get(), is(1));
    }

    @Test
    void shouldPropagateCallableExceptionWhenRetryIfThrowsError() {
        AtomicInteger callCounter = new AtomicInteger();
        IOException original = new IOException("Callable failed");

        ExecutionException thrown = assertThrows(
                ExecutionException.class,
                () -> RetryExecutor
                        .call(() -> {
                            callCounter.incrementAndGet();
                            throw original;
                        })
                        .attempts(3)
                        .delay(Duration.ZERO)
                        .retryIf(_ -> {
                            throw new AssertionError("retryIf failed");
                        })
                        .run()
        );

        assertThat("Original callable exception should be propagated", thrown.getCause(), sameInstance(original));
        assertThat("Callable should not be retried", callCounter.get(), is(1));
    }
}
