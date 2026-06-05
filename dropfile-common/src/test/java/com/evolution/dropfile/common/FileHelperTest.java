package com.evolution.dropfile.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FileHelperTest {

    private FileHelper fileHelper;

    private File file;

    @BeforeEach
    public void before() {
        fileHelper = new FileHelper(64 * 1024);
        file = new File(getClass().getClassLoader().getResource("readStream.txt").getFile());
    }

    @Test
    public void readStreamDoesNotReadAllFile() throws Exception {
        try (InputStream inputStream = fileHelper.readStream(file, 0, 2)) {
            byte[] bytes = inputStream.readAllBytes();
            String actual = new String(bytes);

            assertThat(
                    actual,
                    is("12")
            );
        }
    }

    @Test
    public void readStreamIsReadable() throws Exception {
        try (InputStream inputStream = fileHelper.readStream(file, 0, 4)) {
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
    public void readStreamSkip() throws Exception {
        try (InputStream inputStream = fileHelper.readStream(file, 2, 4)) {
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
    public void readStreamFull() throws Exception {
        try (InputStream inputStream = fileHelper.readStream(file, 0, Integer.MAX_VALUE)) {
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
    public void sha256File() throws Exception {
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
