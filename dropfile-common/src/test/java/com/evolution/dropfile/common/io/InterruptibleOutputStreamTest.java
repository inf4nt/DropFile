package com.evolution.dropfile.common.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

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
    void writeInt_ShouldDelegate_WhenNotInterrupted() throws IOException {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);

        stream.write(42);

        verify(delegateMock).write(42);
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
    void writeByteArrayWithOffset_ShouldDelegate_WhenNotInterrupted() throws IOException {
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
    void close_ShouldDelegateClose_WhenNotInterrupted() throws IOException {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);

        stream.close();

        verify(delegateMock).close();
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
    void writeByteArrayWithOffset_ShouldThrowAndNotDelegate_WhenInterrupted() throws Exception {
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

        verify(delegateMock, never()).close();
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

        assertThatThrownBy(() -> stream.write(new byte[]{1, 2}))
                .isInstanceOf(IOException.class);

        assertThatThrownBy(() -> stream.write(new byte[]{1, 2}, 0, 2))
                .isInstanceOf(IOException.class);

        assertThatThrownBy(stream::flush)
                .isInstanceOf(IOException.class);

        assertThatThrownBy(stream::close)
                .isInstanceOf(IOException.class);

        verifyNoInteractions(delegateMock);
    }

    @Test
    void normalIOException_ShouldNotMarkStreamAsAborted() throws IOException {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);
        doThrow(new IOException("Disk full")).when(delegateMock).write(1);

        assertThatThrownBy(() -> stream.write(1))
                .isInstanceOf(IOException.class)
                .hasMessage("Disk full")
                .hasNoCause();

        reset(delegateMock);
        stream.write(2);
        verify(delegateMock).write(2);
    }

    @Test
    void close_ShouldCallFlushAndCloseInCorrectOrder() throws IOException {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);

        stream.close();

        InOrder inOrder = inOrder(delegateMock);
        inOrder.verify(delegateMock).flush();
        inOrder.verify(delegateMock).close();
    }

    @Test
    void close_ShouldStillCallUnderlyingClose_EvenIfFlushThrowsException() throws IOException {
        doThrow(new IOException("Flush failed")).when(delegateMock).flush();
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(delegateMock);

        assertThrows(IOException.class, stream::close);

        verify(delegateMock).close();
    }
}
