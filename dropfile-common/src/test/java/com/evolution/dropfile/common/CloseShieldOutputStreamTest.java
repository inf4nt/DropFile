package com.evolution.dropfile.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CloseShieldOutputStreamTest {

    private OutputStream underlyingStream;

    private CloseShieldOutputStream shieldStream;

    @BeforeEach
    void setUp() {
        underlyingStream = mock(OutputStream.class);
        shieldStream = new CloseShieldOutputStream(underlyingStream);
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
    void testClose_ShouldFlushButNotClose() throws IOException {
        shieldStream.close();

        verify(underlyingStream, times(1)).flush();

        verify(underlyingStream, never()).close();
    }

    @Test
    void testWrite_ExceptionPropagated() throws IOException {
        doThrow(new IOException("Write error")).when(underlyingStream).write(any(byte[].class), anyInt(), anyInt());

        byte[] data = {1, 2, 3};
        assertThrows(IOException.class, () -> shieldStream.write(data, 0, 3));
    }

    @Test
    void testCloseFlush_ExceptionPropagated() throws IOException {
        doThrow(new IOException("Flush failed")).when(underlyingStream).flush();

        assertThrows(IOException.class, () -> shieldStream.close());
    }
}