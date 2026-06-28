package com.evolution.dropfile.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FileHelperTest {

    private FileHelper fileHelper;

    private File file;

    @BeforeEach
    public void before() {
        fileHelper = new FileHelper();
        file = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("numbers.txt")).getFile()
        );
    }

    @Test
    public void readStream_ShouldReturnLimitedBytes_WhenUsingReadAllBytes() throws Exception {
        try (InputStream inputStream = fileHelper.readStream(file.toPath(), 0, 2)) {
            byte[] bytes = inputStream.readAllBytes();
            String actual = new String(bytes);

            assertThat(
                    actual,
                    is("12")
            );
        }
    }

    @Test
    public void readStream_ShouldReturnLimitedBytes_WhenReadInChunksFromStart() throws Exception {
        try (InputStream inputStream = fileHelper.readStream(file.toPath(), 0, 4)) {
            StringBuilder actual = new StringBuilder();
            byte[] bytes = new byte[3];
            while (true) {
                int read = inputStream.read(bytes);
                if (read == -1) {
                    break;
                }
                byte[] actualBytes = Arrays.copyOfRange(bytes, 0, read);
                String newString = new String(actualBytes);
                actual.append(newString);
            }
            assertThat(
                    actual.toString(),
                    is("1234")
            );
        }
    }

    @Test
    public void readStream_ShouldStartFromOffset_WhenReadInChunks() throws Exception {
        try (InputStream inputStream = fileHelper.readStream(file.toPath(), 2, 4)) {
            StringBuilder actual = new StringBuilder();
            byte[] bytes = new byte[3];
            while (true) {
                int read = inputStream.read(bytes);
                if (read == -1) {
                    break;
                }
                byte[] actualBytes = Arrays.copyOfRange(bytes, 0, read);
                String newString = new String(actualBytes);
                actual.append(newString);
            }
            assertThat(
                    actual.toString(),
                    is("3456")
            );
        }
    }

    @Test
    public void readStream_ShouldReturnEntireFile_WhenSizeIsMaxInteger() throws Exception {
        try (InputStream inputStream = fileHelper.readStream(file.toPath(), 0, Integer.MAX_VALUE)) {
            StringBuilder actual = new StringBuilder();
            byte[] bytes = new byte[3];
            while (true) {
                int read = inputStream.read(bytes);
                if (read == -1) {
                    break;
                }
                byte[] actualBytes = Arrays.copyOfRange(bytes, 0, read);
                String newString = new String(actualBytes);
                actual.append(newString);
            }
            assertThat(
                    actual.toString(),
                    is("1234567890")
            );
        }
    }

    @Test
    public void sha256_ShouldBeIdempotent_WhenCalledMultipleTimes() throws Exception {
        String expected = "c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646";

        assertThat(
                fileHelper.sha256(file.toPath()),
                is(expected)
        );
        assertThat(
                fileHelper.sha256(file.toPath()),
                is(expected)
        );
    }
}
