package com.evolution.dropfile.common.io;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnceCloseableInputStreamTest {

    @Test
    void constructor_ShouldThrowNullPointerException_WhenInputStreamIsNull() {
        assertThrows(NullPointerException.class, () -> new OnceCloseableInputStream(null));
    }

    @Test
    void read_SingleByte_ShouldDelegateToUnderlyingStream() throws IOException {
        try (OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)))
        ) {
            assertEquals('1', stream.read());
            assertEquals('2', stream.read());
        }
    }

    @Test
    void read_ByteArray_ShouldDelegateToUnderlyingStream() throws IOException {
        try (OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)))
        ) {
            byte[] buffer = new byte[2];
            int bytesRead = stream.read(buffer);

            assertEquals(2, bytesRead);
            assertArrayEquals("12".getBytes(StandardCharsets.UTF_8), buffer);
        }
    }

    @Test
    void read_ByteArrayWithOffsetAndLength_ShouldDelegateToUnderlyingStream() throws IOException {
        try (OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)))
        ) {
            byte[] buffer = new byte[4];
            int bytesRead = stream.read(buffer, 1, 2);

            assertEquals(2, bytesRead);
            assertEquals((byte) '1', buffer[1]);
            assertEquals((byte) '2', buffer[2]);
        }
    }

    @Test
    void skip_ShouldDelegateToUnderlyingStream() throws IOException {
        try (OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)))
        ) {
            long skipped = stream.skip(2);

            assertEquals(2, skipped);
            assertEquals('3', stream.read());
        }
    }

    @Test
    void available_ShouldDelegateToUnderlyingStream() throws IOException {
        try (OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)))
        ) {
            assertEquals(4, stream.available());
            stream.read();
            assertEquals(3, stream.available());
        }
    }

    @Test
    void markAndReset_ShouldDelegateToUnderlyingStream() throws IOException {
        try (OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)))
        ) {
            assertTrue(stream.markSupported());

            assertEquals('1', stream.read());
            stream.mark(10);

            assertEquals('2', stream.read());
            assertEquals('3', stream.read());

            stream.reset();
            assertEquals('2', stream.read());
        }
    }

    @Test
    void read_SingleByte_ShouldThrowIOException() throws IOException {
        OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)));
        stream.close();

        assertThrows(IOException.class, stream::read);
    }

    @Test
    void read_ByteArray_ShouldThrowIOException() throws IOException {
        OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)));
        stream.close();

        byte[] buffer = new byte[4];
        assertThrows(IOException.class, () -> stream.read(buffer));
    }

    @Test
    void read_ByteArrayWithOffset_ShouldThrowIOException() throws IOException {
        OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)));
        stream.close();

        byte[] buffer = new byte[4];
        assertThrows(IOException.class, () -> stream.read(buffer, 0, 2));
    }

    @Test
    void skip_ShouldThrowIOException() throws IOException {
        OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)));
        stream.close();

        assertThrows(IOException.class, () -> stream.skip(1));
    }

    @Test
    void close_CalledMultipleTimes_ShouldCloseUnderlyingStreamOnlyOnce() throws IOException {
        InputStream delegateMock = mock(InputStream.class);
        OnceCloseableInputStream stream = new OnceCloseableInputStream(delegateMock);

        stream.close();
        stream.close();
        stream.close();

        verify(delegateMock, times(1)).close();
    }

    @Test
    void available_ShouldThrowIOException_WhenClosed() throws IOException {
        OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)));
        stream.close();

        assertThrows(IOException.class, stream::available);
    }

    @Test
    void reset_ShouldThrowIOException_WhenClosed() throws IOException {
        OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)));
        stream.close();

        assertThrows(IOException.class, stream::reset);
    }

    @Test
    void readAllBytes_ShouldDelegateToUnderlyingStream() throws IOException {
        try (OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)))) {

            assertArrayEquals(
                    "1234".getBytes(StandardCharsets.UTF_8),
                    stream.readAllBytes());
        }
    }

    @Test
    void readNBytes_ShouldDelegateToUnderlyingStream() throws IOException {
        try (OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)))) {

            assertArrayEquals(
                    "12".getBytes(StandardCharsets.UTF_8),
                    stream.readNBytes(2));
        }
    }

    @Test
    void readNBytesWithOffset_ShouldDelegateToUnderlyingStream() throws IOException {
        try (OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)))) {

            byte[] buffer = new byte[4];

            assertEquals(2, stream.readNBytes(buffer, 1, 2));
            assertEquals((byte) '1', buffer[1]);
            assertEquals((byte) '2', buffer[2]);
        }
    }

    @Test
    void transferTo_ShouldDelegateToUnderlyingStream() throws IOException {
        try (OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8)))) {

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            assertEquals(4, stream.transferTo(output));
            assertArrayEquals("1234".getBytes(StandardCharsets.UTF_8), output.toByteArray());
        }
    }

    @Test
    void close_ShouldNotRetryClose_WhenUnderlyingCloseThrowsException() throws IOException {
        InputStream delegateMock = mock(InputStream.class);
        doThrow(new IOException("Close failed")).when(delegateMock).close();

        OnceCloseableInputStream stream = new OnceCloseableInputStream(delegateMock);

        assertThrows(IOException.class, stream::close);

        stream.close();

        verify(delegateMock, times(1)).close();
    }

    @Test
    void skipNBytes_ShouldDelegateToUnderlyingStream() throws IOException {
        try (OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("12345".getBytes(StandardCharsets.UTF_8)))) {

            stream.skipNBytes(2);
            assertEquals('3', stream.read());
        }
    }

    @Test
    void skipNBytes_ShouldThrowIOException_WhenClosed() throws IOException {
        OnceCloseableInputStream stream = new OnceCloseableInputStream(
                new ByteArrayInputStream("12345".getBytes(StandardCharsets.UTF_8)));
        stream.close();

        assertThrows(IOException.class, () -> stream.skipNBytes(2));
    }
}
