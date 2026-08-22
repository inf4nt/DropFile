package com.evolution.dropfile.common.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputStreamPipelineTest {

    @Test
    void shouldCreatePipelineWithoutWrappers() throws IOException {
        byte[] data = "test-data".getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream initial = new ByteArrayInputStream(data);
             InputStream result = InputStreamPipeline.from(initial).get()) {

            assertThat(result, notNullValue());
            assertThat(result.readAllBytes(), is(data));
        }
    }

    @Test
    void shouldSuccessfullyExecutePipelineWithMultipleWrappers() throws Exception {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        InputStreamPipeline.StreamWrapper upperCaseWrapper = in -> new InputStream() {
            private final InputStream delegate = in;

            @Override
            public int read() throws IOException {
                int read = delegate.read();
                if (read == -1) {
                    return -1;
                }
                return Character.toUpperCase((char) read);
            }
        };

        try (ByteArrayInputStream initial = new ByteArrayInputStream(data);
             InputStream result = InputStreamPipeline.from(initial)
                     .add(upperCaseWrapper)
                     .get()) {

            assertThat(new String(result.readAllBytes(), StandardCharsets.UTF_8), is("HELLO"));
        }
    }

    @Test
    void shouldThrowExceptionWhenInitialStreamIsNull() {
        assertThrows(NullPointerException.class, () -> InputStreamPipeline.from(null));
    }

    @Test
    void shouldCloseStreamAndThrowExceptionWhenWrapperThrowsError() {
        SpyInputStream spyStream = new SpyInputStream();

        assertThrows(RuntimeException.class, () ->
                InputStreamPipeline.from(spyStream)
                        .add(in -> {
                            throw new IOException("Failed to wrap");
                        })
                        .get()
        );

        assertThat(spyStream.isClosed(), is(true));
    }

    @Test
    void shouldCloseStreamWhenWrapperReturnsNull() {
        SpyInputStream spyStream = new SpyInputStream();

        assertThrows(NullPointerException.class, () ->
                InputStreamPipeline.from(spyStream)
                        .add(in -> null)
                        .get()
        );

        assertThat(spyStream.isClosed(), is(true));
    }

    @Test
    void shouldThrowExceptionWhenAddAfterGet() throws Exception {
        byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream initial = new ByteArrayInputStream(data);
             InputStreamPipeline pipeline = InputStreamPipeline.from(initial)) {

            InputStream result = pipeline.get();

            assertThrows(RuntimeException.class, () ->
                    pipeline.add(in -> in)
            );

            assertThat(result.readAllBytes(), is(data));
        }
    }

    @Test
    void shouldNotCloseStreamOnPipelineCloseAfterGet() throws Exception {
        SpyInputStream spyStream = new SpyInputStream();
        InputStream resultStream;

        try (InputStreamPipeline pipeline = InputStreamPipeline.from(spyStream)) {
            resultStream = pipeline.get();
        }

        assertThat(spyStream.isClosed(), is(false));

        resultStream.close();
        assertThat(spyStream.isClosed(), is(true));
    }

    @Test
    void shouldBeIdempotentOnMultipleCloseCalls() throws Exception {
        SpyInputStream spyStream = new SpyInputStream();

        try (InputStreamPipeline pipeline = InputStreamPipeline.from(spyStream)) {
            pipeline.close();
            pipeline.close();
            pipeline.close();
        }

        assertThat(spyStream.getCloseCount(), is(1));
    }

    @Test
    void shouldBeThreadSafeOnConcurrentClose() throws Exception {
        SpyInputStream spyStream = new SpyInputStream();
        InputStreamPipeline pipeline = InputStreamPipeline.from(spyStream);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    pipeline.close();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS), is(true));

        assertThat(spyStream.getCloseCount(), is(1));
    }

    private static class SpyInputStream extends InputStream {
        private final AtomicInteger closeCount = new AtomicInteger(0);
        private boolean closed = false;

        @Override
        public int read() {
            return -1;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            closeCount.incrementAndGet();
            super.close();
        }

        public boolean isClosed() {
            return closed;
        }

        public int getCloseCount() {
            return closeCount.get();
        }
    }
}
