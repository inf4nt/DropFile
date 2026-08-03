package com.evolution.dropfile.common.io;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WatchdogInputStreamTest {

    @Test
    public void shouldNotCreateWatchdogTaskWhenDurationIsNull() {
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(new ByteArrayInputStream("12345".getBytes()), Long.MAX_VALUE);
        assertThat("Watchdog task should be null when duration is null", watchdogInputStream.watchdogTask, nullValue());
    }

    @Test
    public void shouldReadFullContentWhenNoLimitIsPresent() throws Exception {
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream("12345".getBytes());
        InputStream inputStream = new WatchdogInputStream(originalInputStream, Long.MAX_VALUE);

        byte[] bytes = inputStream.readAllBytes();

        assertArrayEquals("12345".getBytes(), bytes);
        assertThat("Single byte read should return -1 at EOF", inputStream.read(), is(-1));
        assertThat("Buffer read should return -1 at EOF", inputStream.read(new byte[5]), is(-1));
        assertThat("Buffer read with off/len should return -1 at EOF", inputStream.read(new byte[5], 0, 5), is(-1));
        assertThat("readAllBytes() length should be 0 at EOF", inputStream.readAllBytes().length, is(0));
        assertThat("readNBytes(len) length should be 0 at EOF", inputStream.readNBytes(5).length, is(0));
        assertThat("readNBytes(buffer, off, len) should return 0 at EOF", inputStream.readNBytes(new byte[5], 0, 5), is(0));

        assertThat("Original input stream read should return -1 at EOF", originalInputStream.read(), is(-1));
        assertThat("Original stream readAllBytes() length should be 0 at EOF", originalInputStream.readAllBytes().length, is(0));
    }

    @Test
    public void shouldReadFullContentWhenLimitEqualsContentLength() throws Exception {
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream("12345".getBytes());
        InputStream inputStream = new WatchdogInputStream(originalInputStream, 5);

        byte[] bytes = inputStream.readAllBytes();

        assertArrayEquals("12345".getBytes(), bytes);
        assertThat("read() should return -1 when limit equals content length", inputStream.read(), is(-1));
        assertThat("read(byte[]) should return -1 when limit equals content length", inputStream.read(new byte[5]), is(-1));
        assertThat("readAllBytes() length should be 0 after limit is reached", inputStream.readAllBytes().length, is(0));
        assertThat("readNBytes(int) length should be 0 after limit is reached", inputStream.readNBytes(5).length, is(0));

        assertThat("Original stream should be fully consumed", originalInputStream.read(), is(-1));
        assertThat("Original stream readAllBytes() length should be 0", originalInputStream.readAllBytes().length, is(0));
    }

    @Test
    public void shouldTruncateContentWhenLimitIsLessThanContentLength() throws Exception {
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream("12345".getBytes());
        InputStream inputStream = new WatchdogInputStream(originalInputStream, 3);

        byte[] bytes = inputStream.readAllBytes();

        assertArrayEquals("123".getBytes(), bytes);
        assertThat("read() should return -1 after content truncation", inputStream.read(), is(-1));
        assertThat("read(byte[]) should return -1 after content truncation", inputStream.read(new byte[5]), is(-1));
        assertThat("readAllBytes() length should be 0 after truncation", inputStream.readAllBytes().length, is(0));
        assertThat("readNBytes(int) length should be 0 after truncation", inputStream.readNBytes(5).length, is(0));

        assertArrayEquals("45".getBytes(), originalInputStream.readAllBytes());
    }

    @Test
    public void shouldReadCorrectlyUsingSmallBufferWhenLimitIsEnforced() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("12345".getBytes()), 3);
        byte[] buffer = new byte[2];
        StringBuilder stringBuilder = new StringBuilder();
        while (true) {
            int read = inputStream.read(buffer, 0, buffer.length);
            if (read == -1) {
                break;
            }
            stringBuilder.append(new String(Arrays.copyOf(buffer, read)));
        }
        assertThat("Content read with small buffer should be truncated to limit", stringBuilder.toString(), is("123"));
    }

    @Test
    public void shouldReadCorrectlyUsingBufferLargerThanLimit() throws Exception {
        int limit = 3;
        int bufferSize = limit + 1;

        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("12345".getBytes()), limit);
        byte[] buffer = new byte[bufferSize];
        StringBuilder stringBuilder = new StringBuilder();
        while (true) {
            int read = inputStream.read(buffer);
            if (read == -1) {
                break;
            }
            stringBuilder.append(new String(Arrays.copyOf(buffer, read)));
        }
        assertThat("Content read with buffer larger than limit should be truncated to limit", stringBuilder.toString(), is("123"));
    }

    @Test
    public void shouldSupportReadNBytesWithOffsetAndLen() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("1234567890".getBytes()), 6);

        byte[] target = new byte[10];
        int readCount = inputStream.readNBytes(target, 2, 4);

        assertThat("readNBytes should return exact number of requested bytes", readCount, is(4));
        assertArrayEquals("1234".getBytes(), Arrays.copyOfRange(target, 2, 6));

        byte[] remaining = inputStream.readAllBytes();
        assertArrayEquals("56".getBytes(), remaining);
        assertThat("Stream read should return -1 after reading up to limit", inputStream.read(), is(-1));
    }

    @Test
    public void shouldRespectLimitWhenSkippingBytes() throws Exception {
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream("1234567890".getBytes());
        InputStream inputStream = new WatchdogInputStream(originalInputStream, 5);

        long skipped = inputStream.skip(3);
        assertThat("skip() should return actual number of skipped bytes", skipped, is(3L));

        byte[] remaining = inputStream.readAllBytes();
        assertArrayEquals("45".getBytes(), remaining);

        assertThat("read() should return -1 after limit is reached via skip and read", inputStream.read(), is(-1));
        assertThat("skip() should return 0 after limit is reached", inputStream.skip(10), is(0L));

        assertArrayEquals("67890".getBytes(), originalInputStream.readAllBytes());
    }

    @Test
    public void shouldTransferToOutputStreamRespectingLimit() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("1234567890".getBytes()), 4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        long transferred = inputStream.transferTo(outputStream);

        assertThat("transferTo should return exact number of bytes transferred up to limit", transferred, is(4L));
        assertArrayEquals("1234".getBytes(), outputStream.toByteArray());
        assertThat("read() should return -1 after transferTo reaches limit", inputStream.read(), is(-1));
    }

    @Test
    public void shouldThrowIOExceptionWhenTimeoutExpiresDuringRead() throws Exception {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        InputStream inputStream = new WatchdogInputStream(originalInputStream, Long.MAX_VALUE, Duration.ofMillis(100));

        assertArrayEquals("12".getBytes(), inputStream.readNBytes(2));

        Awaitility.await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertAllStreamOperationsThrowClosedException(inputStream));

        assertAllStreamOperationsThrowClosedException(originalInputStream);
    }

    @Test
    public void shouldReadSuccessfullyBeforeTimeoutAndCancelTaskOnClose() throws Exception {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(
                originalInputStream,
                Long.MAX_VALUE,
                Duration.ofSeconds(10)
        );
        assertThat("Watchdog task should be initialized", watchdogInputStream.watchdogTask, notNullValue());

        assertArrayEquals("12".getBytes(), watchdogInputStream.readNBytes(2));
        assertArrayEquals("345".getBytes(), watchdogInputStream.readNBytes(3));
        assertThat("readNBytes should return 0 bytes when stream reaches EOF", watchdogInputStream.readNBytes(3).length, is(0));

        watchdogInputStream.close();

        assertThat("Watchdog task should be cancelled when stream is closed", watchdogInputStream.watchdogTask.isCancelled(), is(true));
        assertAllStreamOperationsThrowClosedException(watchdogInputStream);
        assertAllStreamOperationsThrowClosedException(originalInputStream);
    }

    @Test
    public void shouldCloseOriginalInputStreamWhenWatchdogIsClosed() throws Exception {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(originalInputStream, Long.MAX_VALUE);
        assertThat("Watchdog task should be null when no duration is specified", watchdogInputStream.watchdogTask, nullValue());

        assertArrayEquals("12".getBytes(), watchdogInputStream.readNBytes(2));
        assertArrayEquals("345".getBytes(), watchdogInputStream.readNBytes(3));
        assertThat("readNBytes length should be 0 at EOF", watchdogInputStream.readNBytes(3).length, is(0));
        assertThat("read() should return -1 at EOF", watchdogInputStream.read(), is(-1));

        watchdogInputStream.close();

        assertAllStreamOperationsThrowClosedException(watchdogInputStream);
        assertAllStreamOperationsThrowClosedException(originalInputStream);
    }

    @Test
    public void shouldAutomaticallyCloseStreamsWhenUsedInTryWithResources() throws IOException {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        try (InputStream watchdogInputStream = new WatchdogInputStream(originalInputStream, Long.MAX_VALUE)) {
            assertArrayEquals("123".getBytes(), watchdogInputStream.readNBytes(3));
        }

        assertAllStreamOperationsThrowClosedException(originalInputStream);
    }

    @Test
    public void shouldBeIdempotentWhenCloseIsCalledMultipleTimes() throws Exception {
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(
                new ByteArrayInputStream("12345".getBytes()),
                Long.MAX_VALUE,
                Duration.ofSeconds(10)
        );

        watchdogInputStream.close();
        assertThat("Watchdog task should be cancelled on first close", watchdogInputStream.watchdogTask.isCancelled(), is(true));

        watchdogInputStream.close();
        watchdogInputStream.close();

        assertAllStreamOperationsThrowClosedException(watchdogInputStream);
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenDurationIsZeroOrNegative() {
        InputStream in = new ByteArrayInputStream("data".getBytes());

        assertThrows(IllegalArgumentException.class, () ->
                new WatchdogInputStream(in, 100, Duration.ZERO));

        assertThrows(IllegalArgumentException.class, () ->
                new WatchdogInputStream(in, 100, Duration.ofMillis(-500)));
    }

    @Test
    public void shouldNotHaveInterruptedStatusWhenClosingStreamNormally() throws Exception {
        AtomicBoolean interruptedDuringClose = new AtomicBoolean(true);

        InputStream originalInputStream = new ByteArrayInputStream("12345".getBytes()) {
            @Override
            public void close() throws IOException {
                interruptedDuringClose.set(Thread.currentThread().isInterrupted());
                super.close();
            }
        };

        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(originalInputStream, Long.MAX_VALUE);

        watchdogInputStream.close();

        assertThat("Thread should not be interrupted when closing stream normally", interruptedDuringClose.get(), is(false));
    }

    @Test
    public void shouldReturnEofOrEmptyForAll6ReadMethodsWhenLimitIsReached() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("1234567890".getBytes()), 3);

        byte[] initialRead = inputStream.readNBytes(3);
        assertThat("Initial readNBytes length should match requested count", initialRead.length, is(3));

        assertThat("read() should return -1", inputStream.read(), is(-1));

        assertThat("read(byte[]) should return -1", inputStream.read(new byte[5]), is(-1));

        assertThat("read(byte[], off, len) should return -1", inputStream.read(new byte[5], 0, 5), is(-1));

        assertThat("readAllBytes() should return empty array", inputStream.readAllBytes().length, is(0));

        assertThat("readNBytes(int) should return empty array", inputStream.readNBytes(5).length, is(0));

        assertThat("readNBytes(byte[], off, len) should return 0", inputStream.readNBytes(new byte[5], 0, 5), is(0));
    }

    @Test
    public void shouldThrowIOExceptionForAll6ReadMethodsWhenTimeoutExpires() throws Exception {
        InputStream originalInputStream = new ByteArrayInputStream("1234567890".getBytes());
        InputStream watchdogStream = new WatchdogInputStream(originalInputStream, Long.MAX_VALUE, Duration.ofMillis(50));

        Awaitility.await()
                .atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertAllStreamOperationsThrowClosedException(watchdogStream);
                });
    }

    @Test
    public void shouldCorrectlyEnforceLimitAndClosedStateUsingSingleArgumentRead() throws Exception {
        InputStream originalInputStream = new ByteArrayInputStream("1234567890".getBytes());
        WatchdogInputStream watchdogStream = new WatchdogInputStream(originalInputStream, 4);

        byte[] buffer = new byte[10];

        int bytesRead = watchdogStream.read(buffer);
        assertThat("Single argument read should return number of bytes up to limit", bytesRead, is(4));
        assertArrayEquals("1234".getBytes(), Arrays.copyOf(buffer, 4));

        assertThat("Single argument read should return -1 when limit is reached", watchdogStream.read(buffer), is(-1));

        watchdogStream.close();
        assertThrows(IOException.class, () -> watchdogStream.read(buffer));
    }

    @Test
    public void shouldThrowExceptionWhenLimitIsZeroOrNegative() {
        InputStream in = new ByteArrayInputStream("data".getBytes());

        assertThrows(IllegalArgumentException.class, () -> new WatchdogInputStream(in, 0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new WatchdogInputStream(in, -1, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new WatchdogInputStream(in, -100, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new WatchdogInputStream(in, Long.MIN_VALUE, Duration.ofSeconds(1)));
    }

    @Test
    public void shouldThrowNpeWhenInputStreamIsNull() {
        assertThrows(NullPointerException.class, () -> new WatchdogInputStream(null, 100, Duration.ofSeconds(1)));
    }

    @Test
    public void shouldCancelWatchdogTaskOnEof() throws Exception {
        byte[] data = "ab".getBytes();
        WatchdogInputStream stream = new WatchdogInputStream(new ByteArrayInputStream(data), 100, Duration.ofSeconds(10));

        stream.readAllBytes();

        assertThat("Watchdog task should not be null", stream.watchdogTask, notNullValue());
        assertThat("Watchdog task should be done on EOF", stream.watchdogTask.isDone(), is(true));
        assertThat("Watchdog task should be cancelled on EOF", stream.watchdogTask.isCancelled(), is(true));
    }

    @Test
    public void shouldCancelWatchdogAndNotCloseStreamWhenReadFully() throws Exception {
        AtomicBoolean originalStreamClosed = new AtomicBoolean(false);

        InputStream originalInputStream = new ByteArrayInputStream("hello".getBytes()) {
            @Override
            public void close() throws IOException {
                originalStreamClosed.set(true);
                super.close();
            }
        };

        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(
                originalInputStream,
                Long.MAX_VALUE,
                Duration.ofSeconds(10)
        );

        byte[] bytes = watchdogInputStream.readAllBytes();
        assertArrayEquals("hello".getBytes(), bytes);

        assertThat("Watchdog task should be initialized", watchdogInputStream.watchdogTask, notNullValue());
        assertThat("Watchdog task should be cancelled when stream is read fully", watchdogInputStream.watchdogTask.isCancelled(), is(true));

        assertThat("Original stream should not be closed automatically on EOF", originalStreamClosed.get(), is(false));

        watchdogInputStream.close();
        assertThat("Original stream should be closed when watchdog stream is closed", originalStreamClosed.get(), is(true));
        assertAllStreamOperationsThrowClosedException(watchdogInputStream);
    }

    @Test
    public void shouldCloseStreamWhenTimeoutExpires() throws Exception {
        AtomicBoolean originalStreamClosed = new AtomicBoolean(false);

        InputStream originalInputStream = new ByteArrayInputStream("12345".getBytes()) {
            @Override
            public void close() throws IOException {
                originalStreamClosed.set(true);
                super.close();
            }
        };

        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(
                originalInputStream,
                Long.MAX_VALUE,
                Duration.ofMillis(100)
        );

        Awaitility.await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat("Original stream should be closed when timeout expires", originalStreamClosed.get(), is(true));
                    assertAllStreamOperationsThrowClosedException(watchdogInputStream);
                });
    }

    @Test
    public void shouldKeepStreamOpenBeforeDurationExpires() throws Exception {
        InputStream originalInputStream = new ByteArrayInputStream("12345".getBytes());

        WatchdogInputStream watchdogStream = new WatchdogInputStream(
                originalInputStream,
                Long.MAX_VALUE,
                Duration.ofSeconds(10)
        );

        Thread.sleep(100);

        assertThat("Task should not be done yet", watchdogStream.watchdogTask.isDone(), is(false));

        assertThat("Stream should return first byte before timeout expires", watchdogStream.read(), is((int) '1'));
    }

    @Test
    void testCancelFalseFailsWhenThreadIsAlreadySleeping() throws Exception {
        var underlyingClosedLatch = new CountDownLatch(1);

        InputStream source = new ByteArrayInputStream(new byte[]{1}) {
            @Override
            public void close() throws IOException {
                underlyingClosedLatch.countDown();
                super.close();
            }
        };

        WatchdogInputStream watchdogStream = new WatchdogInputStream(source, 5, Duration.ofMillis(300));

        Thread.sleep(100);

        watchdogStream.readAllBytes();

        boolean closedByWatchdog = underlyingClosedLatch.await(500, TimeUnit.MILLISECONDS);

        assertThat("Watchdog task should be cancelled when stream is read fully", watchdogStream.watchdogTask.isCancelled(), is(true));

        assertThat("Underlying stream should not be closed by watchdog when cancelled", closedByWatchdog, is(false));
    }

    @Test
    public void shouldNotBlockOtherStreamsWhenOneCloseHangs() throws Exception {
        CountDownLatch stream1CloseStarted = new CountDownLatch(1);
        CountDownLatch stream2Closed = new CountDownLatch(1);

        InputStream slowInputStream = new FilterInputStream(new ByteArrayInputStream(new byte[0])) {
            @Override
            public void close() throws IOException {
                stream1CloseStarted.countDown();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                super.close();
            }
        };

        InputStream fastInputStream = new FilterInputStream(new ByteArrayInputStream(new byte[0])) {
            @Override
            public void close() throws IOException {
                super.close();
                stream2Closed.countDown();
            }
        };

        WatchdogInputStream watchdog1 = new WatchdogInputStream(slowInputStream, Long.MAX_VALUE, Duration.ofMillis(50));

        assertThat("Stream 1 should start closing", stream1CloseStarted.await(1, TimeUnit.SECONDS), is(true));

        WatchdogInputStream watchdog2 = new WatchdogInputStream(fastInputStream, Long.MAX_VALUE, Duration.ofMillis(100));

        boolean closedOnTime = stream2Closed.await(500, TimeUnit.MILLISECONDS);
        assertThat("Stream 2 should close on its own timeout without waiting for Stream 1", closedOnTime, is(true));

        watchdog1.close();
        watchdog2.close();
    }


    @Test
    public void shouldNotSupportMarkAndReset() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("12345".getBytes()), Long.MAX_VALUE);

        assertThat("markSupported() should return false", inputStream.markSupported(), is(false));

        inputStream.mark(10);

        assertThrows(IOException.class, inputStream::reset);
    }

    @Test
    public void shouldReturnZeroWhenSkippingZeroOrNegativeBytes() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("12345".getBytes()), Long.MAX_VALUE);

        assertThat("skip(0) should return 0", inputStream.skip(0), is(0L));
        assertThat("skip(-1) should return 0", inputStream.skip(-1), is(0L));

        assertArrayEquals("12345".getBytes(), inputStream.readAllBytes());
    }

    @Test
    public void shouldReturnZeroWhenReadingZeroBytes() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("12345".getBytes()), Long.MAX_VALUE);

        byte[] buffer = new byte[5];

        assertThat("read(byte[], off, 0) should return 0", inputStream.read(buffer, 0, 0), is(0));

        assertArrayEquals("12345".getBytes(), inputStream.readAllBytes());
    }

    @Test
    public void shouldNotChangeStreamStateWhenMarkIsCalled() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("12345".getBytes()), Long.MAX_VALUE);

        inputStream.mark(100);

        assertArrayEquals("12345".getBytes(), inputStream.readAllBytes());

        assertThat("read() should return -1 at EOF", inputStream.read(), is(-1));
    }

    @Test
    public void shouldRespectLimitWhenSkippingNBytes() throws Exception {
        InputStream inputStream = new WatchdogInputStream(
                new ByteArrayInputStream("12345".getBytes()), 2);

        inputStream.skipNBytes(2);

        assertThat("available() should return 0 after reaching limit", inputStream.available(), is(0));

        assertThat("read() should return -1 after reaching limit", inputStream.read(), is(-1));
    }

    @Test
    public void shouldThrowWhenSkippingMoreThanLimit() throws Exception {
        InputStream inputStream = new WatchdogInputStream(
                new ByteArrayInputStream("12345".getBytes()), 2);

        assertThrows(IOException.class, () -> inputStream.skipNBytes(3));
    }

    @Test
    public void shouldReturnZeroWhenReadingZeroBytesAfterReachingLimit() throws Exception {
        try (InputStream inputStream = new WatchdogInputStream(
                new ByteArrayInputStream("12345".getBytes()), 3)) {

            byte[] buffer = new byte[3];
            inputStream.read(buffer, 0, 3);

            assertThat(
                    "Reading zero bytes should return 0 even after reaching the limit",
                    inputStream.read(new byte[10], 0, 0),
                    is(0)
            );
        }
    }

    @Test
    public void shouldCancelWatchdogAfterReadingLastAllowedByte() throws Exception {
        try (WatchdogInputStream inputStream = new WatchdogInputStream(
                new ByteArrayInputStream("A".getBytes()), 1, Duration.ofHours(1))) {

            assertThat("Watchdog task should be created", inputStream.watchdogTask, is(notNullValue()));
            assertThat("Watchdog task should be active", inputStream.watchdogTask.isCancelled(), is(false));

            assertThat("Should read the only available byte", inputStream.read(), is((int) 'A'));

            assertThat(
                    "Watchdog should be cancelled after reaching the limit",
                    inputStream.watchdogTask.isCancelled(),
                    is(true));
        }
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenBufferIsNullInRead() {
        WatchdogInputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("12345".getBytes()), Long.MAX_VALUE);

        assertThrows(NullPointerException.class, () -> inputStream.read(null, 0, 0));
    }

    @Test
    public void shouldEnsureAvailableIsNeverNegative() throws Exception {
        InputStream faultyStream = new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public int available() {
                return -5;
            }
        };

        WatchdogInputStream inputStream = new WatchdogInputStream(faultyStream, 10);

        assertThat("available() should not return negative numbers", inputStream.available(), greaterThanOrEqualTo(0));
    }

    @Test
    public void shouldReturnAvailableBytes() throws Exception {
        try (InputStream inputStream = new WatchdogInputStream(
                new ByteArrayInputStream("12345".getBytes()), Long.MAX_VALUE)) {

            assertThat(
                    "available() should return number of remaining bytes",
                    inputStream.available(),
                    is(5)
            );

            assertThat(
                    "read() should return first byte",
                    inputStream.read(),
                    is((int) '1')
            );

            assertThat(
                    "available() should decrease after reading",
                    inputStream.available(),
                    is(4)
            );
        }
    }

    @Test
    public void shouldRespectLimitWhenReturningAvailableBytes() throws Exception {
        try (InputStream inputStream = new WatchdogInputStream(
                new ByteArrayInputStream("12345".getBytes()), 2)) {

            assertThat(
                    "available() should not exceed limit",
                    inputStream.available(),
                    is(2)
            );

            assertThat(
                    "read() should return first byte",
                    inputStream.read(),
                    is((int) '1')
            );

            assertThat(
                    "available() should decrease after reading",
                    inputStream.available(),
                    is(1)
            );

            assertThat(
                    "read() should return second byte",
                    inputStream.read(),
                    is((int) '2')
            );

            assertThat(
                    "available() should be 0 after reaching limit",
                    inputStream.available(),
                    is(0)
            );
        }
    }

    @Test
    public void shouldCancelWatchdogAfterReadingLastAllowedBytesUsingBufferRead() throws Exception {
        try (WatchdogInputStream inputStream = new WatchdogInputStream(
                new ByteArrayInputStream("12345".getBytes()), 3, Duration.ofHours(1))) {

            assertThat("Watchdog task should be created", inputStream.watchdogTask, is(notNullValue()));
            assertThat("Watchdog task should be active", inputStream.watchdogTask.isCancelled(), is(false));

            byte[] buffer = new byte[5];

            assertThat(
                    "read(byte[], off, len) should read up to the limit",
                    inputStream.read(buffer, 0, buffer.length),
                    is(3)
            );

            assertArrayEquals(
                    "123".getBytes(),
                    Arrays.copyOf(buffer, 3)
            );

            assertThat(
                    "Watchdog should be cancelled after reaching the limit",
                    inputStream.watchdogTask.isCancelled(),
                    is(true)
            );
        }
    }

    private void assertAllStreamOperationsThrowClosedException(InputStream stream) {
        assertThrows(IOException.class, stream::read, "read() should throw IOException");
        assertThrows(IOException.class, () -> stream.read(new byte[10]), "read(byte[]) should throw IOException");
        assertThrows(IOException.class, () -> stream.read(new byte[10], 0, 5), "read(byte[], int, int) should throw IOException");
        assertThrows(IOException.class, stream::readAllBytes, "readAllBytes() should throw IOException");
        assertThrows(IOException.class, () -> stream.readNBytes(10), "readNBytes(int) should throw IOException");
        assertThrows(IOException.class, () -> stream.readNBytes(new byte[10], 0, 5), "readNBytes(byte[], int, int) should throw IOException");
        assertThrows(IOException.class, () -> stream.skip(1), "skip() should throw IOException");
        assertThrows(IOException.class, () -> stream.transferTo(OutputStream.nullOutputStream()), "transferTo() should throw IOException");
    }
}
