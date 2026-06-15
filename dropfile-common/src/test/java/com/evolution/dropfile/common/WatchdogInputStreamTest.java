package com.evolution.dropfile.common;

import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
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
        assertThat(inputStream.readAllBytes().length, is(0));
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
        assertThat(inputStream.readAllBytes().length, is(0));
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
        assertThat(inputStream.readAllBytes().length, is(0));

        assertArrayEquals("45".getBytes(), originalInputStream.readAllBytes());
    }

    @Test
    public void shouldReadCorrectlyUsingSmallBufferWhenLimitIsEnforced() throws Exception {
        InputStream inputStream = new WatchdogInputStream(new ByteArrayInputStream("12345".getBytes()), 3);
        byte[] buffer = new byte[2];
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
    public void shouldThrowIOExceptionWhenTimeoutExpiresDuringRead() throws Exception {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        InputStream inputStream = new WatchdogInputStream(originalInputStream, Long.MAX_VALUE, Duration.ofMillis(100));

        assertArrayEquals("12".getBytes(), inputStream.readNBytes(2));
        Thread.sleep(200);

        assertThrows(IOException.class, () -> inputStream.read());
        assertThrows(IOException.class, () -> inputStream.readAllBytes());

        assertThrows(IOException.class, () -> originalInputStream.read());
        assertThrows(IOException.class, () -> originalInputStream.readAllBytes());
    }

    @Test
    public void shouldReadSuccessfullyBeforeTimeoutAndCancelTaskOnClose() throws Exception {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(
                originalInputStream,
                Long.MAX_VALUE,
                Duration.ofMillis(1000)
        );
        assertThat(watchdogInputStream.watchdogTask, notNullValue());

        assertArrayEquals("12".getBytes(), watchdogInputStream.readNBytes(2));
        Thread.sleep(10);
        assertArrayEquals("345".getBytes(), watchdogInputStream.readNBytes(3));
        Thread.sleep(10);
        assertThat(watchdogInputStream.readNBytes(3).length, is(0));
        watchdogInputStream.close();

        assertThrows(IOException.class, () -> watchdogInputStream.readAllBytes());
        assertThat(watchdogInputStream.watchdogTask.isCancelled(), is(true));

        assertThrows(IOException.class, () -> originalInputStream.read());
        assertThrows(IOException.class, () -> originalInputStream.readAllBytes());
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

        assertThrows(IOException.class, () -> watchdogInputStream.readAllBytes());
        assertThrows(IOException.class, () -> watchdogInputStream.read());
        assertThrows(IOException.class, () -> originalInputStream.read());
        assertThrows(IOException.class, () -> originalInputStream.readAllBytes());
    }

    @Test
    public void shouldAutomaticallyCloseStreamsWhenUsedInTryWithResources() throws IOException {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        try (InputStream watchdogInputStream = new WatchdogInputStream(originalInputStream)) {
            assertArrayEquals("123".getBytes(), watchdogInputStream.readNBytes(3));
        }

        assertThrows(IOException.class, () -> originalInputStream.read());
        assertThrows(IOException.class, () -> originalInputStream.readAllBytes());
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
}