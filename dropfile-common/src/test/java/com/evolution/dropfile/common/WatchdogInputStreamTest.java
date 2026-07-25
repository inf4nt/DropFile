package com.evolution.dropfile.common;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WatchdogInputStreamTest {

    @Test
    public void shouldNotCreateWatchdogTaskWhenDurationIsNull() {
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(new ByteArrayInputStream("12345".getBytes()));
        assertThat(watchdogInputStream.watchdogTask, nullValue());
    }

    @Test
    public void shouldReadFullContentWhenNoLimitIsPresent() throws Exception {
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream("12345".getBytes());
        InputStream inputStream = new WatchdogInputStream(originalInputStream);

        byte[] bytes = inputStream.readAllBytes();

        assertArrayEquals("12345".getBytes(), bytes);
        assertThat(inputStream.read(), is(-1));
        assertThat(inputStream.read(new byte[5]), is(-1));
        assertThat(inputStream.read(new byte[5], 0, 5), is(-1));
        assertThat(inputStream.readAllBytes().length, is(0));
        assertThat(inputStream.readNBytes(5).length, is(0));
        assertThat(inputStream.readNBytes(new byte[5], 0, 5), is(0));

        assertThat(originalInputStream.read(), is(-1));
        assertThat(originalInputStream.readAllBytes().length, is(0));
    }

    @Test
    public void shouldReadFullContentWhenLimitEqualsContentLength() throws Exception {
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream("12345".getBytes());
        InputStream inputStream = new WatchdogInputStream(originalInputStream, 5);

        byte[] bytes = inputStream.readAllBytes();

        assertArrayEquals("12345".getBytes(), bytes);
        assertThat(inputStream.read(), is(-1));
        assertThat(inputStream.read(new byte[5]), is(-1));
        assertThat(inputStream.readAllBytes().length, is(0));
        assertThat(inputStream.readNBytes(5).length, is(0));

        assertThat(originalInputStream.read(), is(-1));
        assertThat(originalInputStream.readAllBytes().length, is(0));
    }

    @Test
    public void shouldTruncateContentWhenLimitIsLessThanContentLength() throws Exception {
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream("12345".getBytes());
        InputStream inputStream = new WatchdogInputStream(originalInputStream, 3);

        byte[] bytes = inputStream.readAllBytes();

        assertArrayEquals("123".getBytes(), bytes);
        assertThat(inputStream.read(), is(-1));
        assertThat(inputStream.read(new byte[5]), is(-1));
        assertThat(inputStream.readAllBytes().length, is(0));
        assertThat(inputStream.readNBytes(5).length, is(0));

        // В оригинальном стриме должно остаться "45"
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
        assertThat(stringBuilder.toString(), is("123"));
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
        assertThat(stringBuilder.toString(), is("123"));
    }

    @Test
    public void shouldSupportReadNBytesWithOffsetAndLen() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("1234567890".getBytes()), 6);

        byte[] target = new byte[10];
        int readCount = inputStream.readNBytes(target, 2, 4);

        assertThat(readCount, is(4));
        assertArrayEquals("1234".getBytes(), Arrays.copyOfRange(target, 2, 6));

        byte[] remaining = inputStream.readAllBytes();
        assertArrayEquals("56".getBytes(), remaining);
        assertThat(inputStream.read(), is(-1));
    }

    @Test
    public void shouldRespectLimitWhenSkippingBytes() throws Exception {
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream("1234567890".getBytes());
        InputStream inputStream = new WatchdogInputStream(originalInputStream, 5);

        long skipped = inputStream.skip(3);
        assertThat(skipped, is(3L));

        byte[] remaining = inputStream.readAllBytes();
        assertArrayEquals("45".getBytes(), remaining);

        assertThat(inputStream.read(), is(-1));
        assertThat(inputStream.skip(10), is(0L));

        assertArrayEquals("67890".getBytes(), originalInputStream.readAllBytes());
    }

    @Test
    public void shouldTransferToOutputStreamRespectingLimit() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("1234567890".getBytes()), 4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        long transferred = inputStream.transferTo(outputStream);

        assertThat(transferred, is(4L));
        assertArrayEquals("1234".getBytes(), outputStream.toByteArray());
        assertThat(inputStream.read(), is(-1));
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
        assertThat(watchdogInputStream.watchdogTask, notNullValue());

        assertArrayEquals("12".getBytes(), watchdogInputStream.readNBytes(2));
        assertArrayEquals("345".getBytes(), watchdogInputStream.readNBytes(3));
        assertThat(watchdogInputStream.readNBytes(3).length, is(0));

        watchdogInputStream.close();

        assertThat(watchdogInputStream.watchdogTask.isCancelled(), is(true));
        assertAllStreamOperationsThrowClosedException(watchdogInputStream);
        assertAllStreamOperationsThrowClosedException(originalInputStream);
    }

    @Test
    public void shouldCloseOriginalInputStreamWhenWatchdogIsClosed() throws Exception {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(originalInputStream);
        assertThat(watchdogInputStream.watchdogTask, nullValue());

        assertArrayEquals("12".getBytes(), watchdogInputStream.readNBytes(2));
        assertArrayEquals("345".getBytes(), watchdogInputStream.readNBytes(3));
        assertThat(watchdogInputStream.readNBytes(3).length, is(0));
        assertThat(watchdogInputStream.read(), is(-1));

        watchdogInputStream.close();

        assertAllStreamOperationsThrowClosedException(watchdogInputStream);
        assertAllStreamOperationsThrowClosedException(originalInputStream);
    }

    @Test
    public void shouldAutomaticallyCloseStreamsWhenUsedInTryWithResources() throws IOException {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        try (InputStream watchdogInputStream = new WatchdogInputStream(originalInputStream)) {
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
        assertThat(watchdogInputStream.watchdogTask.isCancelled(), is(true));

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

        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(originalInputStream);

        watchdogInputStream.close();

        assertThat(interruptedDuringClose.get(), is(false));
    }

    @Test
    public void shouldReturnEofOrEmptyForAll6ReadMethodsWhenLimitIsReached() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("1234567890".getBytes()), 3);

        byte[] initialRead = inputStream.readNBytes(3);
        assertThat(initialRead.length, is(3));

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
        assertThat(bytesRead, is(4));
        assertArrayEquals("1234".getBytes(), Arrays.copyOf(buffer, 4));

        assertThat(watchdogStream.read(buffer), is(-1));

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

        assertThat(stream.watchdogTask, notNullValue());
        assertThat(stream.watchdogTask.isDone(), is(true));
        assertThat(stream.watchdogTask.isCancelled(), is(true));
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

        assertThat(watchdogInputStream.watchdogTask, notNullValue());
        assertThat(watchdogInputStream.watchdogTask.isCancelled(), is(true));

        assertThat(originalStreamClosed.get(), is(false));

        watchdogInputStream.close();
        assertThat(originalStreamClosed.get(), is(true));
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
                    assertThat(originalStreamClosed.get(), is(true));
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

        assertThat(watchdogStream.read(), is((int) '1'));
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
