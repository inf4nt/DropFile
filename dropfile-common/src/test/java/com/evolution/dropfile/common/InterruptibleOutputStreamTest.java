package com.evolution.dropfile.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterruptibleOutputStreamTest {

    @Mock
    private OutputStream delegateMock;

    @AfterEach
    void tearDown() {
        Thread.interrupted();
    }

    @Test
    void stream_ShouldWrapStandardStream() {
        OutputStream rawStream = new ByteArrayOutputStream();
        InterruptibleOutputStream interruptibleStream = InterruptibleOutputStream.stream(rawStream);

        assertThat(interruptibleStream).isNotNull();
        assertThat(interruptibleStream).isNotSameAs(rawStream);
    }

    @Test
    void stream_ShouldNotReWrapExistingInterruptibleStream() {
        InterruptibleOutputStream firstStream = InterruptibleOutputStream.stream(delegateMock);
        InterruptibleOutputStream secondStream = InterruptibleOutputStream.stream(firstStream);

        assertThat(secondStream).isSameAs(firstStream);
    }

    @Test
    void stream_ShouldThrowNpeOnNull() {
        assertThatThrownBy(() -> InterruptibleOutputStream.stream(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("write(int) должен делегировать вызов в обернутый поток при нормальной работе")
    void writeInt_ShouldDelegate_WhenNotInterrupted() throws IOException {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);

        stream.write(42);

        verify(delegateMock).write(42);
    }

    @Test
    void writeByteArray_ShouldDelegate_WhenNotInterrupted() throws IOException {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);
        byte[] buffer = new byte[]{1, 2, 3};

        stream.write(buffer, 0, 3);

        verify(delegateMock).write(buffer, 0, 3);
    }

    @Test
    void flush_ShouldDelegate_WhenNotInterrupted() throws IOException {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);

        stream.flush();

        verify(delegateMock).flush();
    }

    @Test
    void close_ShouldDelegateFlush_WhenNotInterrupted() throws IOException {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);

        stream.close();

        verify(delegateMock).flush();
    }

    @Test
    void writeInt_ShouldThrowAndNotDelegate_WhenInterrupted() throws Exception {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> stream.write(1))
                .isInstanceOf(IOException.class)
                .hasCauseInstanceOf(InterruptedException.class);

        verify(delegateMock, never()).write(1);
    }

    @Test
    void writeByteArray_ShouldThrowAndNotDelegate_WhenInterrupted() throws Exception {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);
        byte[] buffer = new byte[]{1, 2, 3};
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> stream.write(buffer, 0, 3))
                .isInstanceOf(IOException.class)
                .hasCauseInstanceOf(InterruptedException.class);

        verify(delegateMock, never()).write(buffer, 0, 3);
    }

    @Test
    void flush_ShouldThrowAndNotDelegate_WhenInterrupted() throws Exception {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);
        Thread.currentThread().interrupt();

        assertThatThrownBy(stream::flush)
                .isInstanceOf(IOException.class)
                .hasCauseInstanceOf(InterruptedException.class);

        verify(delegateMock, never()).flush();
    }

    @Test
    void close_ShouldThrowAndNotDelegate_WhenInterrupted() throws Exception {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);
        Thread.currentThread().interrupt();

        assertThatThrownBy(stream::close)
                .isInstanceOf(IOException.class)
                .hasCauseInstanceOf(InterruptedException.class);

        verify(delegateMock, never()).flush();
    }

    @Test
    void abortedLatch_ShouldStayAborted_EvenIfThreadInterruptStatusIsCleared() throws Exception {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);

        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> stream.write(100))
                .isInstanceOf(IOException.class);

        boolean wasInterrupted = Thread.interrupted();
        assertThat(wasInterrupted).isTrue();
        assertThat(Thread.currentThread().isInterrupted()).isFalse();

        assertThatThrownBy(() -> stream.write(200))
                .isInstanceOf(IOException.class);

        assertThatThrownBy(() -> stream.write(new byte[]{1, 2}, 0, 2))
                .isInstanceOf(IOException.class);

        assertThatThrownBy(stream::flush)
                .isInstanceOf(IOException.class);

        assertThatThrownBy(stream::close)
                .isInstanceOf(IOException.class);

        verify(delegateMock, never()).write(100);
        verify(delegateMock, never()).write(200);
        verify(delegateMock, never()).flush();
    }

    @Test
    void writeByteArraySimple_ShouldDelegate_WhenNotInterrupted() throws IOException {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);
        byte[] buffer = new byte[]{10, 20, 30};

        stream.write(buffer);

        verify(delegateMock).write(buffer, 0, buffer.length);
    }

    @Test
    void writeByteArraySimple_ShouldThrowAndNotDelegate_WhenInterrupted() throws Exception {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);
        byte[] buffer = new byte[]{10, 20, 30};
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> stream.write(buffer))
                .isInstanceOf(IOException.class)
                .hasCauseInstanceOf(InterruptedException.class);

        verify(delegateMock, never()).write(buffer, 0, buffer.length);
    }

    @Test
    void writeByteArraySimple_ShouldThrowNpe_WhenNullPassed() {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);

        assertThatThrownBy(() -> stream.write(null))
                .isInstanceOf(NullPointerException.class);
    }
}
