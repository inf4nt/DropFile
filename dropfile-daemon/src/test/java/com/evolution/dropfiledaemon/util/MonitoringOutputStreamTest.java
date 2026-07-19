package com.evolution.dropfiledaemon.util;

import com.evolution.dropfiledaemon.tunnel.framework.monitor.MonitoringOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoringOutputStreamTest {

    private ThroughputMeter speedMeter;

    @BeforeEach
    void setUp() {
        speedMeter = mock(ThroughputMeter.class);
    }

    @Test
    void testWriteSingleByte_Success() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MonitoringOutputStream mos = new MonitoringOutputStream(baos, speedMeter)) {

            mos.write(42);

            byte[] result = baos.toByteArray();
            assertEquals(1, result.length);
            assertEquals(42, result[0]);

            verify(speedMeter, times(1)).add(1);
        }
    }

    @Test
    void testWriteByteArraySlice_Success() throws IOException {
        byte[] data = {10, 20, 30, 40, 50};
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MonitoringOutputStream mos = new MonitoringOutputStream(baos, speedMeter)) {

            mos.write(data, 1, 3);

            byte[] result = baos.toByteArray();
            assertArrayEquals(new byte[]{20, 30, 40}, result);

            verify(speedMeter, times(1)).add(3);
        }
    }

    @Test
    void testWriteByteArrayImplicit_NoDoubleCounting() throws IOException {
        byte[] data = {1, 2, 3, 4};
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MonitoringOutputStream mos = new MonitoringOutputStream(baos, speedMeter)) {

            mos.write(data);

            assertArrayEquals(data, baos.toByteArray());

            verify(speedMeter, times(1)).add(4);

            verify(speedMeter, never()).add(1);
        }
    }

    @Test
    void testWriteSingleByte_ExceptionPropagated() throws IOException {
        OutputStream brokenStream = mock(OutputStream.class);
        doThrow(new IOException("Connection reset")).when(brokenStream).write(anyInt());

        try (MonitoringOutputStream mos = new MonitoringOutputStream(brokenStream, speedMeter)) {
            assertThrows(IOException.class, () -> mos.write(100));

            verifyNoInteractions(speedMeter);
        }
    }

    @Test
    void testWriteByteArray_ExceptionPropagated() throws IOException {
        OutputStream brokenStream = mock(OutputStream.class);
        doThrow(new IOException("Write failed")).when(brokenStream).write(any(byte[].class), anyInt(), anyInt());

        try (MonitoringOutputStream mos = new MonitoringOutputStream(brokenStream, speedMeter)) {
            byte[] data = {1, 2, 3};
            assertThrows(IOException.class, () -> mos.write(data, 0, 3));

            verifyNoInteractions(speedMeter);
        }
    }
}
