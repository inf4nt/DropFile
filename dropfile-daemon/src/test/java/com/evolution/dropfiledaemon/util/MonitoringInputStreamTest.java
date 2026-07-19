package com.evolution.dropfiledaemon.util;

import com.evolution.dropfiledaemon.tunnel.framework.monitor.MonitoringInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoringInputStreamTest {

    private ThroughputMeter speedMeter;

    @BeforeEach
    void setUp() {
        speedMeter = mock(ThroughputMeter.class);
    }

    @Test
    void testReadSingleByte_Success() throws IOException {
        byte[] data = {42};
        try (InputStream bis = new ByteArrayInputStream(data);
             MonitoringInputStream mis = new MonitoringInputStream(bis, speedMeter)) {

            int result = mis.read();

            assertEquals(42, result);
            verify(speedMeter, times(1)).add(1);
        }
    }

    @Test
    void testReadSingleByte_EOF() throws IOException {
        byte[] data = {};
        try (InputStream bis = new ByteArrayInputStream(data);
             MonitoringInputStream mis = new MonitoringInputStream(bis, speedMeter)) {

            int result = mis.read();

            assertEquals(-1, result);
            verifyNoInteractions(speedMeter);
        }
    }

    @Test
    void testReadByteArray_Success() throws IOException {
        byte[] data = {10, 20, 30, 40, 50};
        try (InputStream bis = new ByteArrayInputStream(data);
             MonitoringInputStream mis = new MonitoringInputStream(bis, speedMeter)) {

            byte[] buffer = new byte[3];
            int bytesRead = mis.read(buffer, 0, 3);

            assertEquals(3, bytesRead);
            assertArrayEquals(new byte[]{10, 20, 30}, buffer);
            verify(speedMeter, times(1)).add(3);
        }
    }

    @Test
    void testReadByteArray_EOF() throws IOException {
        byte[] data = {};
        try (InputStream bis = new ByteArrayInputStream(data);
             MonitoringInputStream mis = new MonitoringInputStream(bis, speedMeter)) {

            byte[] buffer = new byte[3];
            int bytesRead = mis.read(buffer, 0, 3);

            assertEquals(-1, bytesRead);
            verifyNoInteractions(speedMeter);
        }
    }

    @Test
    void testReadByteArrayImplicit_NoDoubleCounting() throws IOException {
        byte[] data = {1, 2, 3, 4, 5};
        try (InputStream bis = new ByteArrayInputStream(data);
             MonitoringInputStream mis = new MonitoringInputStream(bis, speedMeter)) {

            byte[] buffer = new byte[3];
            int bytesRead = mis.read(buffer);

            assertEquals(3, bytesRead);

            verify(speedMeter, times(1)).add(3);

            verify(speedMeter, never()).add(1);
        }
    }

    @Test
    void testRead_ExceptionPropagated() throws IOException {
        InputStream brokenStream = mock(InputStream.class);
        when(brokenStream.read()).thenThrow(new IOException("Connection reset"));

        try (MonitoringInputStream mis = new MonitoringInputStream(brokenStream, speedMeter)) {
            assertThrows(IOException.class, mis::read);

            verifyNoInteractions(speedMeter);
        }
    }
}