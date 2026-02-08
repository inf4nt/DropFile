package com.evolution.dropfiledaemon.util;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileHelperTest {

    private FileHelper fileHelper;

    private File file;

    @BeforeEach
    public void before() {
        fileHelper = new FileHelper();
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
    public void testReadChunks() throws Exception {
        List<FileHelper.ChunkContainer> containers = new ArrayList<>();
        fileHelper.read(file, 3, chunkContainer -> {
            containers.add(chunkContainer);
        });

        assertThat(containers.size(), is(4));

        assertThat(
                containers,
                hasItems(
                        new FileHelper.ChunkContainer("123".getBytes(), 0, 3),
                        new FileHelper.ChunkContainer("456".getBytes(), 3, 6),
                        new FileHelper.ChunkContainer("789".getBytes(), 6, 9),
                        new FileHelper.ChunkContainer("0".getBytes(), 9, 10)
                )
        );
    }

    @Test
    public void readBytes() throws Exception {
        try (FileInputStream inputStream = new FileInputStream(file);
             FileChannel fileChannel = inputStream.getChannel()) {
            byte[] bytes = fileHelper.readBytes(fileChannel, 0, 2);
            assertThat(
                    bytes,
                    is("12".getBytes())
            );
        }

        try (FileInputStream inputStream = new FileInputStream(file);
             FileChannel fileChannel = inputStream.getChannel()) {
            byte[] bytes = fileHelper.readBytes(fileChannel, 2, 4);
            assertThat(
                    bytes,
                    is("3456".getBytes())
            );
        }
    }

    @Test
    public void sha256() {
        String message = "abcdefg";

        String sha256_1 = fileHelper.sha256(message.getBytes());
        String sha256_2 = fileHelper.sha256(message.getBytes());

        assertThat(
                sha256_1,
                is(sha256_2)
        );

        assertThat(
                sha256_1,
                is("7d1a54127b222502f5b79b5fb0803061152a44f92b37e23c6527baf665d4da9a")
        );

        message = "123456";
        sha256_1 = fileHelper.sha256(message.getBytes());
        sha256_2 = fileHelper.sha256(message.getBytes());

        assertThat(
                sha256_1,
                is(sha256_2)
        );

        assertThat(
                sha256_1,
                is("8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92")
        );
    }

    @Test
    public void sha256File() {
        String expected = "c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646";

        assertThat(
                fileHelper.sha256(file, 3),
                is(expected)
        );
        assertThat(
                fileHelper.sha256(file, 3),
                is(expected)
        );

        assertThat(
                fileHelper.sha256(file),
                is(expected)
        );
        assertThat(
                fileHelper.sha256(file),
                is(expected)
        );
    }

    @Test
    public void toDisplaySize() {
        assertThrows(IllegalArgumentException.class, () -> fileHelper.toDisplaySize(-1));

        assertThat(
                fileHelper.toDisplaySize(0),
                is("0B")
        );

        assertThat(
                fileHelper.toDisplaySize(12 * 1024),
                is("12KB")
        );

        assertThat(
                fileHelper.toDisplaySize(123 * 1024),
                is("123KB")
        );

        assertThat(
                fileHelper.toDisplaySize(1023 * 1024),
                is("1023KB")
        );

        assertThat(
                fileHelper.toDisplaySize(1 * 1024 * 1024),
                is("1MB")
        );

        assertThat(
                fileHelper.toDisplaySize(13 * 1024 * 1024),
                is("13MB")
        );

        assertThat(
                fileHelper.toDisplaySize(1023 * 1024 * 1024),
                is("1023MB")
        );

        assertThat(
                fileHelper.toDisplaySize(1 * (1024 * 1024 * 1024L)),
                is("1GB")
        );

        assertThat(
                fileHelper.toDisplaySize(14 * (1024 * 1024 * 1024L)),
                is("14GB")
        );

        assertThat(
                fileHelper.toDisplaySize(1023 * (1024 * 1024 * 1024L)),
                is("1023GB")
        );

        assertThat(
                fileHelper.toDisplaySize(5000 * (1024 * 1024 * 1024L)),
                is("5000GB")
        );
    }
}
