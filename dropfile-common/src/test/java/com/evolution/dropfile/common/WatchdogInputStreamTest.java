package com.evolution.dropfile.common;

import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WatchdogInputStreamTest {

    @Test
    public void durationNull() {
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(new ByteArrayInputStream("12345".getBytes()));
        assertThat(
                watchdogInputStream.watchdogTask,
                nullValue()
        );
    }

    @Test
    public void readAll() throws Exception {
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream("12345".getBytes());
        InputStream inputStream = new WatchdogInputStream(originalInputStream);
        byte[] bytes = inputStream.readAllBytes();

        assertThat(bytes, is("12345".getBytes()));
        assertThat(inputStream.read(), is(-1));
        assertThat(inputStream.readAllBytes().length, is(0));
        assertThat(originalInputStream.read(), is(-1));
        assertThat(originalInputStream.readAllBytes().length, is(0));
    }

    @Test
    public void readAllLimit5() throws Exception {
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream("12345".getBytes());
        InputStream inputStream = new WatchdogInputStream(originalInputStream);
        byte[] bytes = inputStream.readAllBytes();

        assertThat(bytes, is("12345".getBytes()));
        assertThat(inputStream.read(), is(-1));
        assertThat(inputStream.readAllBytes().length, is(0));
        assertThat(originalInputStream.read(), is(-1));
        assertThat(originalInputStream.readAllBytes().length, is(0));
    }

    @Test
    public void readAllLimit3() throws Exception {
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream("12345".getBytes());
        InputStream inputStream = new WatchdogInputStream(originalInputStream, 3);
        byte[] bytes = inputStream.readAllBytes();

        assertThat(bytes, is("123".getBytes()));
        assertThat(inputStream.read(), is(-1));
        assertThat(inputStream.readAllBytes().length, is(0));

        assertThat(originalInputStream.readAllBytes(), is("45".getBytes()));
    }

    @Test
    public void readBufferLimit3() throws Exception {
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
    public void readBigBufferLimit3() throws Exception {
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
    public void durationNegative() throws Exception {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        InputStream inputStream = new WatchdogInputStream(originalInputStream, Long.MAX_VALUE, Duration.ofMillis(200));

        assertThat(inputStream.readNBytes(2), is("12".getBytes()));
        Thread.sleep(250);

        assertThrows(IOException.class, () -> inputStream.read());
        assertThrows(IOException.class, () -> inputStream.readAllBytes());

        assertThrows(IOException.class, () -> originalInputStream.read());
        assertThrows(IOException.class, () -> originalInputStream.readAllBytes());
    }

    @Test
    public void durationNegative2() throws Exception {
        InputStream inputStream = new WatchdogInputStream(
                new ByteArrayInputStream("12345".getBytes()),
                Long.MAX_VALUE,
                Duration.ofMillis(200)
        );

        assertThat(inputStream.readNBytes(2), is("12".getBytes()));
        Thread.sleep(400);

        assertThrows(IOException.class, () -> inputStream.read());
        assertThrows(IOException.class, () -> inputStream.readAllBytes());
    }

    @Test
    public void duration() throws Exception {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(
                originalInputStream,
                Long.MAX_VALUE,
                Duration.ofMillis(500)
        );
        assertThat(watchdogInputStream.watchdogTask, notNullValue());

        assertThat(watchdogInputStream.readNBytes(2), is("12".getBytes()));
        Thread.sleep(10);
        assertThat(watchdogInputStream.readNBytes(3), is("345".getBytes()));
        Thread.sleep(10);
        assertThat(watchdogInputStream.readNBytes(3).length, is(0));
        watchdogInputStream.close();

        assertThrows(IOException.class, () -> watchdogInputStream.readAllBytes());
        assertThat(watchdogInputStream.watchdogTask.isCancelled(), is(true));

        assertThrows(IOException.class, () -> originalInputStream.read());
        assertThrows(IOException.class, () -> originalInputStream.readAllBytes());
    }

    @Test
    public void closeClosesOriginalInputStream() throws Exception {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(
                originalInputStream
        );
        assertThat(watchdogInputStream.watchdogTask, nullValue());

        assertThat(watchdogInputStream.readNBytes(2), is("12".getBytes()));
        assertThat(watchdogInputStream.readNBytes(3), is("345".getBytes()));
        assertThat(watchdogInputStream.readNBytes(3).length, is(0));
        assertThat(watchdogInputStream.read(), is(-1));
        watchdogInputStream.close();

        assertThrows(IOException.class, () -> watchdogInputStream.readAllBytes());
        assertThrows(IOException.class, () -> watchdogInputStream.read());
        assertThrows(IOException.class, () -> originalInputStream.read());
        assertThrows(IOException.class, () -> originalInputStream.readAllBytes());
    }

    @Test
    public void tryWithResources() throws IOException {
        InputStream originalInputStream = new BufferedInputStream(new ByteArrayInputStream("12345".getBytes()));
        try (InputStream watchdogInputStream = new WatchdogInputStream(originalInputStream)) {
            assertThat(watchdogInputStream.readNBytes(3), is("123".getBytes()));
        }

        assertThrows(IOException.class, () -> originalInputStream.read());
        assertThrows(IOException.class, () -> originalInputStream.readAllBytes());
    }
}
