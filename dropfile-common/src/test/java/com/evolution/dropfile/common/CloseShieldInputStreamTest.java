package com.evolution.dropfile.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CloseShieldInputStreamTest {

    private InputStream underlyingStream;

    private CloseShieldInputStream shieldStream;

    @BeforeEach
    void setUp() {
        underlyingStream = mock(InputStream.class);
        shieldStream = new CloseShieldInputStream(underlyingStream);
    }

    @Test
    void testReadSingleByte_Delegated() throws IOException {
        when(underlyingStream.read()).thenReturn(42);

        int result = shieldStream.read();

        assertEquals(42, result);
        verify(underlyingStream, times(1)).read();
    }

    @Test
    void testReadByteArraySlice_Delegated() throws IOException {
        byte[] buffer = new byte[10];
        when(underlyingStream.read(buffer, 2, 5)).thenReturn(5);

        int bytesRead = shieldStream.read(buffer, 2, 5);

        assertEquals(5, bytesRead);
        verify(underlyingStream, times(1)).read(buffer, 2, 5);
    }

    @Test
    void testReadByteArrayImplicit_Delegated() throws IOException {
        byte[] buffer = new byte[10];
        when(underlyingStream.read(buffer, 0, 10)).thenReturn(10);

        int bytesRead = shieldStream.read(buffer);

        assertEquals(10, bytesRead);
        verify(underlyingStream, times(1)).read(buffer, 0, 10);
    }

    @Test
    void testClose_ShouldNotCloseUnderlying() throws IOException {
        shieldStream.close();

        verify(underlyingStream, never()).close();
    }

    @Test
    void testRead_ExceptionPropagated() throws IOException {
        when(underlyingStream.read()).thenThrow(new IOException("Read timeout"));

        assertThrows(IOException.class, () -> shieldStream.read());
    }
}