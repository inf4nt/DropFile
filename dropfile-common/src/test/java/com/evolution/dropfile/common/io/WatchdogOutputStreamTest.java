package com.evolution.dropfile.common.io;

import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WatchdogOutputStreamTest {

    @Test
    public void shouldNotCreateWatchdogTaskWhenDurationIsNullOrZeroOrNegative() {
        WatchdogOutputStream watchdogOutputStream = new WatchdogOutputStream(OutputStream.nullOutputStream(), null);
        assertThat("Watchdog task should be null when duration is null", watchdogOutputStream.watchdogTask, nullValue());

        watchdogOutputStream = new WatchdogOutputStream(OutputStream.nullOutputStream(), Duration.ofMillis(0));
        assertThat("Watchdog task should be null when duration is zero", watchdogOutputStream.watchdogTask, nullValue());

        watchdogOutputStream = new WatchdogOutputStream(OutputStream.nullOutputStream(), Duration.ofMillis(-500));
        assertThat("Watchdog task should be null when duration is negative", watchdogOutputStream.watchdogTask, nullValue());
    }

    @Test
    public void shouldWriteFullContentWhenNoTimeoutExpires() throws Exception {
        ByteArrayOutputStream originalOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = new WatchdogOutputStream(originalOutputStream, null);

        outputStream.write("12345".getBytes(StandardCharsets.UTF_8));
        outputStream.flush();

        assertArrayEquals("12345".getBytes(StandardCharsets.UTF_8), originalOutputStream.toByteArray());
    }

    @Test
    public void shouldWriteSuccessfullyUsingSingleByteAndBufferMethods() throws Exception {
        ByteArrayOutputStream originalOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = new WatchdogOutputStream(originalOutputStream, Duration.ofSeconds(10));

        outputStream.write('1');
        outputStream.write(new byte[]{'2', '3'});
        outputStream.write(new byte[]{'X', '4', '5', 'Y'}, 1, 2);
        outputStream.flush();

        assertArrayEquals("12345".getBytes(StandardCharsets.UTF_8), originalOutputStream.toByteArray());
    }

    @Test
    public void shouldThrowIOExceptionWhenTimeoutExpiresDuringWrite() throws Exception {
        OutputStream originalOutputStream = new MockClosedOutputStream();
        OutputStream outputStream = new WatchdogOutputStream(originalOutputStream, Duration.ofMillis(100));

        outputStream.write("12".getBytes(StandardCharsets.UTF_8));

        Awaitility.await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertAllStreamOperationsThrowClosedException(outputStream));

        assertAllStreamOperationsThrowClosedException(originalOutputStream);
    }

    @Test
    public void shouldWriteSuccessfullyBeforeTimeoutAndCancelTaskOnClose() throws Exception {
        ByteArrayOutputStream originalOutputStream = new ByteArrayOutputStream();
        WatchdogOutputStream watchdogOutputStream = new WatchdogOutputStream(
                originalOutputStream,
                Duration.ofSeconds(10)
        );
        assertThat("Watchdog task should be initialized", watchdogOutputStream.watchdogTask, notNullValue());

        watchdogOutputStream.write("12345".getBytes(StandardCharsets.UTF_8));
        watchdogOutputStream.close();

        assertThat("Watchdog task should be cancelled when stream is closed", watchdogOutputStream.watchdogTask.isCancelled(), is(true));
        assertArrayEquals("12345".getBytes(StandardCharsets.UTF_8), originalOutputStream.toByteArray());

        assertAllStreamOperationsThrowClosedException(watchdogOutputStream);
    }

    @Test
    public void shouldCloseOriginalOutputStreamWhenWatchdogIsClosed() throws Exception {
        OutputStream originalOutputStream = new MockClosedOutputStream();
        WatchdogOutputStream watchdogOutputStream = new WatchdogOutputStream(originalOutputStream, null);
        assertThat("Watchdog task should be null when no duration is specified", watchdogOutputStream.watchdogTask, nullValue());

        watchdogOutputStream.write("12345".getBytes(StandardCharsets.UTF_8));
        watchdogOutputStream.close();

        assertAllStreamOperationsThrowClosedException(watchdogOutputStream);
        assertAllStreamOperationsThrowClosedException(originalOutputStream);
    }

    @Test
    public void shouldAutomaticallyCloseStreamsWhenUsedInTryWithResources() throws IOException {
        MockClosedOutputStream originalOutputStream = new MockClosedOutputStream();
        try (OutputStream watchdogOutputStream = new WatchdogOutputStream(originalOutputStream, null)) {
            watchdogOutputStream.write("123".getBytes(StandardCharsets.UTF_8));
        }

        assertAllStreamOperationsThrowClosedException(originalOutputStream);
        assertArrayEquals("123".getBytes(StandardCharsets.UTF_8), originalOutputStream.toByteArray());
    }

    @Test
    public void shouldBeIdempotentWhenCloseIsCalledMultipleTimes() throws Exception {
        WatchdogOutputStream watchdogOutputStream = new WatchdogOutputStream(
                new ByteArrayOutputStream(),
                Duration.ofSeconds(10)
        );

        watchdogOutputStream.close();
        assertThat("Watchdog task should be cancelled on first close", watchdogOutputStream.watchdogTask.isCancelled(), is(true));

        watchdogOutputStream.close();
        watchdogOutputStream.close();

        assertAllStreamOperationsThrowClosedException(watchdogOutputStream);
    }

    @Test
    public void shouldNotHaveInterruptedStatusWhenClosingStreamNormally() throws Exception {
        AtomicBoolean interruptedDuringClose = new AtomicBoolean(true);

        OutputStream originalOutputStream = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                interruptedDuringClose.set(Thread.currentThread().isInterrupted());
                super.close();
            }
        };

        WatchdogOutputStream watchdogOutputStream = new WatchdogOutputStream(originalOutputStream, null);

        watchdogOutputStream.close();

        assertThat("Thread should not be interrupted when closing stream normally", interruptedDuringClose.get(), is(false));
    }

    @Test
    public void shouldThrowIOExceptionForAllWriteAndFlushMethodsWhenTimeoutExpires() throws Exception {
        OutputStream originalOutputStream = new ByteArrayOutputStream();
        OutputStream watchdogStream = new WatchdogOutputStream(originalOutputStream, Duration.ofMillis(50));

        Awaitility.await()
                .atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertAllStreamOperationsThrowClosedException(watchdogStream));
    }

    @Test
    public void shouldThrowNpeWhenOutputStreamIsNull() {
        assertThrows(NullPointerException.class, () -> new WatchdogOutputStream(null, Duration.ofSeconds(1)));
    }

    @Test
    public void shouldCloseStreamWhenTimeoutExpires() throws Exception {
        AtomicBoolean originalStreamClosed = new AtomicBoolean(false);

        OutputStream originalOutputStream = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                originalStreamClosed.set(true);
                super.close();
            }
        };

        WatchdogOutputStream watchdogOutputStream = new WatchdogOutputStream(
                originalOutputStream,
                Duration.ofMillis(100)
        );

        Awaitility.await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat("Original stream should be closed when timeout expires", originalStreamClosed.get(), is(true));
                    assertAllStreamOperationsThrowClosedException(watchdogOutputStream);
                });
    }

    @Test
    public void shouldKeepStreamOpenBeforeDurationExpires() throws Exception {
        ByteArrayOutputStream originalOutputStream = new ByteArrayOutputStream();

        WatchdogOutputStream watchdogStream = new WatchdogOutputStream(
                originalOutputStream,
                Duration.ofSeconds(10)
        );

        Thread.sleep(100);

        assertThat("Task should not be done yet", watchdogStream.watchdogTask.isDone(), is(false));

        watchdogStream.write('1');
        watchdogStream.flush();

        assertArrayEquals("1".getBytes(StandardCharsets.UTF_8), originalOutputStream.toByteArray());
    }

    @Test
    void testCancelFalseFailsWhenThreadIsAlreadySleeping() throws Exception {
        var underlyingClosedLatch = new CountDownLatch(1);

        OutputStream destination = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                underlyingClosedLatch.countDown();
                super.close();
            }
        };

        WatchdogOutputStream watchdogStream = new WatchdogOutputStream(destination, Duration.ofMillis(300));

        Thread.sleep(100);

        watchdogStream.write(new byte[]{1, 2, 3});
        watchdogStream.close();

        boolean closedByWatchdog = underlyingClosedLatch.await(500, TimeUnit.MILLISECONDS);

        assertThat("Watchdog task should be cancelled when stream is closed explicitly", watchdogStream.watchdogTask.isCancelled(), is(true));
        assertThat("Underlying stream should be closed by explicit close call", closedByWatchdog, is(true));
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenBufferIsNullInWrite() {
        WatchdogOutputStream outputStream = new WatchdogOutputStream(new ByteArrayOutputStream(), null);

        assertThrows(NullPointerException.class, () -> outputStream.write(null, 0, 0));
    }

    @Test
    public void shouldNotBlockOtherStreamsWhenOneCloseHangs() throws Exception {
        CountDownLatch stream1CloseStarted = new CountDownLatch(1);
        CountDownLatch stream2Closed = new CountDownLatch(1);

        OutputStream slowOutputStream = new FilterOutputStream(new ByteArrayOutputStream()) {
            @Override
            public void close() throws IOException {
                stream1CloseStarted.countDown();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                super.close();
            }
        };

        OutputStream fastOutputStream = new FilterOutputStream(new ByteArrayOutputStream()) {
            @Override
            public void close() throws IOException {
                super.close();
                stream2Closed.countDown();
            }
        };

        WatchdogOutputStream watchdog1 = new WatchdogOutputStream(slowOutputStream, Duration.ofMillis(50));

        assertThat("Stream 1 should start closing", stream1CloseStarted.await(1, TimeUnit.SECONDS), is(true));

        WatchdogOutputStream watchdog2 = new WatchdogOutputStream(fastOutputStream, Duration.ofSeconds(10));

        watchdog2.close();

        boolean closedOnTime = stream2Closed.await(500, TimeUnit.MILLISECONDS);
        assertThat("Stream 2 should close quickly without waiting for Stream 1's slow close", closedOnTime, is(true));

        watchdog1.close();
    }

    private void assertAllStreamOperationsThrowClosedException(OutputStream stream) {
        assertThrows(IOException.class, () -> stream.write(1), "write(int) should throw IOException");
        assertThrows(IOException.class, () -> stream.write(new byte[10]), "write(byte[]) should throw IOException");
        assertThrows(IOException.class, () -> stream.write(new byte[10], 0, 5), "write(byte[], int, int) should throw IOException");
        assertThrows(IOException.class, stream::flush, "flush() should throw IOException");
    }

    private static class MockClosedOutputStream extends ByteArrayOutputStream {

        private volatile boolean closed = false;

        @Override
        public void write(int b) {
            checkClosed();
            super.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            checkClosed();
            super.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            checkClosed();
            super.flush();
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }

        @SneakyThrows
        private void checkClosed() {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }
    }
}