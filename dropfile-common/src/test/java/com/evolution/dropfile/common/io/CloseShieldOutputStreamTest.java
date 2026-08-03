package com.evolution.dropfile.common.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloseShieldOutputStreamTest {

    @Mock
    private OutputStream underlyingStream;

    private CloseShieldOutputStream shieldStream;

    @BeforeEach
    void setUp() {
        shieldStream = CloseShieldOutputStream.stream(underlyingStream);
    }

    @Test
    void stream_ShouldThrowNullPointerException_WhenOutputStreamIsNull() {
        assertThrows(NullPointerException.class, () -> CloseShieldOutputStream.stream(null));
    }

    @Test
    void stream_ShouldNotReWrapExistingCloseShieldStream() {
        CloseShieldOutputStream secondShieldStream = CloseShieldOutputStream.stream(shieldStream);

        assertSame(shieldStream, secondShieldStream);
    }

    @Test
    void testWriteSingleByte_Delegated() throws IOException {
        shieldStream.write(42);

        verify(underlyingStream, times(1)).write(42);
    }

    @Test
    void testWriteByteArraySlice_Direct() throws IOException {
        byte[] data = {10, 20, 30, 40, 50};

        shieldStream.write(data, 1, 3);

        verify(underlyingStream, times(1)).write(data, 1, 3);
        verify(underlyingStream, never()).write(anyInt());
    }

    @Test
    void testWriteByteArrayImplicit_NoByteByByteLoop() throws IOException {
        byte[] data = {1, 2, 3};

        shieldStream.write(data);

        verify(underlyingStream, times(1)).write(data, 0, 3);
        verify(underlyingStream, never()).write(anyInt());
    }

    @Test
    void testWrite_ExceptionPropagated() throws IOException {
        doThrow(new IOException("Write error"))
                .when(underlyingStream).write(any(byte[].class), anyInt(), anyInt());

        byte[] data = {1, 2, 3};
        assertThrows(IOException.class, () -> shieldStream.write(data, 0, 3));
    }

    @Test
    void testFlush_ShouldDelegateToUnderlyingStream() throws IOException {
        shieldStream.flush();

        verify(underlyingStream, times(1)).flush();
    }

    @Test
    void testClose_ShouldFlushButNotClose() throws IOException {
        shieldStream.close();

        verify(underlyingStream, times(1)).flush();
        verify(underlyingStream, never()).close();
    }

    @Test
    void testCloseFlush_ExceptionPropagated() throws IOException {
        doThrow(new IOException("Flush failed")).when(underlyingStream).flush();

        assertThrows(IOException.class, () -> shieldStream.close());
    }

    @Test
    void close_ShouldFlushOnlyOnce_OnMultipleCalls() throws IOException {
        shieldStream.close();
        shieldStream.close();
        shieldStream.close();

        verify(underlyingStream, times(1)).flush();
        verify(underlyingStream, never()).close();
    }

    @Test
    void write_ShouldDelegateDirectlyWithoutStateChecks() throws IOException {
        byte[] data = {1, 2, 3};
        shieldStream.write(data, 0, 3);

        verify(underlyingStream).write(data, 0, 3);
    }

    @Test
    void flush_ShouldDelegateDirectlyWithoutStateChecks() throws IOException {
        shieldStream.flush();

        verify(underlyingStream).flush();
    }

    @Test
    void stream_ShouldThrowNullPointerException_WhenNullPassed() {
        assertThrows(NullPointerException.class, () -> CloseShieldOutputStream.stream(null));
    }

    @Test
    void stream_ShouldNotReWrapExistingShield() {
        CloseShieldOutputStream wrappedAgain = CloseShieldOutputStream.stream(shieldStream);

        assertSame(shieldStream, wrappedAgain);
    }
}
