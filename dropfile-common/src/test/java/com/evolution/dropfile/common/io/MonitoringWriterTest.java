package com.evolution.dropfile.common.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MonitoringWriterTest {

    private ThroughputMeter speedMeter;

    @BeforeEach
    void setUp() {
        speedMeter = mock(ThroughputMeter.class);
    }

    @Test
    void testWriteSingleChar_Success() throws IOException {
        try (StringWriter sw = new StringWriter();
             MonitoringWriter mw = new MonitoringWriter(sw, speedMeter)) {

            mw.write('A');

            assertEquals("A", sw.toString());
            verify(speedMeter, times(1)).add(1);
        }
    }

    @Test
    void testWriteCharArraySlice_Success() throws IOException {
        char[] data = {'a', 'b', 'c', 'd', 'e'};
        try (StringWriter sw = new StringWriter();
             MonitoringWriter mw = new MonitoringWriter(sw, speedMeter)) {

            mw.write(data, 1, 3);

            assertEquals("bcd", sw.toString());
            verify(speedMeter, times(1)).add(3);
        }
    }

    @Test
    void testWriteCharArrayImplicit_NoDoubleCounting() throws IOException {
        char[] data = {'h', 'e', 'l', 'l', 'o'};
        try (StringWriter sw = new StringWriter();
             MonitoringWriter mw = new MonitoringWriter(sw, speedMeter)) {

            mw.write(data);

            assertEquals("hello", sw.toString());
            verify(speedMeter, times(1)).add(5);
            verify(speedMeter, never()).add(1);
        }
    }

    @Test
    void testWriteStringSlice_Success() throws IOException {
        String data = "Hello World";
        try (StringWriter sw = new StringWriter();
             MonitoringWriter mw = new MonitoringWriter(sw, speedMeter)) {

            mw.write(data, 0, 5);

            assertEquals("Hello", sw.toString());
            verify(speedMeter, times(1)).add(5);
        }
    }

    @Test
    void testWriteStringImplicit_NoDoubleCounting() throws IOException {
        String data = "Spring Boot";
        try (StringWriter sw = new StringWriter();
             MonitoringWriter mw = new MonitoringWriter(sw, speedMeter)) {

            mw.write(data);

            assertEquals("Spring Boot", sw.toString());
            verify(speedMeter, times(1)).add(11);
            verify(speedMeter, never()).add(1);
        }
    }

    @Test
    void testWriteCharArray_ZeroLength_DoesNotInvokeSpeedMeter() throws IOException {
        char[] data = {'a', 'b', 'c'};
        try (StringWriter sw = new StringWriter();
             MonitoringWriter mw = new MonitoringWriter(sw, speedMeter)) {

            mw.write(data, 0, 0);

            assertEquals("", sw.toString());
            verifyNoInteractions(speedMeter);
        }
    }

    @Test
    void testWriteString_ZeroLength_DoesNotInvokeSpeedMeter() throws IOException {
        String data = "test";
        try (StringWriter sw = new StringWriter();
             MonitoringWriter mw = new MonitoringWriter(sw, speedMeter)) {

            mw.write(data, 0, 0);

            assertEquals("", sw.toString());
            verifyNoInteractions(speedMeter);
        }
    }

    @Test
    void testFlush_DelegatesToUnderlyingWriter() throws IOException {
        Writer brokenWriter = mock(Writer.class);
        try (MonitoringWriter mw = new MonitoringWriter(brokenWriter, speedMeter)) {

            mw.flush();

            verify(brokenWriter, times(1)).flush();
        }
    }

    @Test
    void testWriteSingleChar_ExceptionPropagated() throws IOException {
        Writer brokenWriter = mock(Writer.class);
        doThrow(new IOException("Write failed")).when(brokenWriter).write(anyInt());

        try (MonitoringWriter mw = new MonitoringWriter(brokenWriter, speedMeter)) {
            assertThrows(IOException.class, () -> mw.write('X'));

            verifyNoInteractions(speedMeter);
        }
    }

    @Test
    void testWriteCharArray_ExceptionPropagated() throws IOException {
        Writer brokenWriter = mock(Writer.class);
        doThrow(new IOException("Write failed")).when(brokenWriter).write(any(char[].class), anyInt(), anyInt());

        try (MonitoringWriter mw = new MonitoringWriter(brokenWriter, speedMeter)) {
            char[] data = {'a', 'b'};
            assertThrows(IOException.class, () -> mw.write(data, 0, 2));

            verifyNoInteractions(speedMeter);
        }
    }

    @Test
    void testWriteString_ExceptionPropagated() throws IOException {
        Writer brokenWriter = mock(Writer.class);
        doThrow(new IOException("Write failed")).when(brokenWriter).write(anyString(), anyInt(), anyInt());

        try (MonitoringWriter mw = new MonitoringWriter(brokenWriter, speedMeter)) {
            assertThrows(IOException.class, () -> mw.write("test", 0, 4));

            verifyNoInteractions(speedMeter);
        }
    }
}
