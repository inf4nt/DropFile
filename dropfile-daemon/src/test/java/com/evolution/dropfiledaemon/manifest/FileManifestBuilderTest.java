package com.evolution.dropfiledaemon.manifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileManifestBuilderTest {

    private File file;

    @BeforeEach
    public void before() {
        file = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("readStream.txt")).getFile()
        );
    }

    @Test
    public void noFileFound() {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, Integer.MAX_VALUE);

        assertThrows(FileNotFoundException.class, () -> {
            underTest.build(new File("fake-file.txt"));
        });
    }

    @Test
    public void directoriesAreUnsupported() {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, Integer.MAX_VALUE);

        assertThrows(
                UnsupportedOperationException.class,
                () -> underTest.build(new File(""))
        );
    }

    @Test
    public void buildManifestChunkSize3() {
        FileManifestBuilder underTest = new FileManifestBuilder(3, 4);

        FileManifest actual = underTest.build(file);
        assertThat(
                actual.hash(),
                is("c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646")
        );
        assertThat(
                actual.size(),
                is(10L)
        );
        assertThat(
                actual.fileName(),
                is("readStream.txt")
        );
        assertThat(
                actual.chunkManifests().size(),
                is(4)
        );
        assertThat(
                actual.chunkManifests(),
                hasItems(
                        new ChunkManifest(
                                "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
                                3,
                                0,
                                3
                        ),
                        new ChunkManifest(
                                "b3a8e0e1f9ab1bfe3a36f231f676f78bb30a519d2b21e6c530c0eee8ebb4a5d0",
                                3,
                                3,
                                6
                        ),
                        new ChunkManifest(
                                "35a9e381b1a27567549b5f8a6f783c167ebf809f1c4d6a9e367240484d8ce281",
                                3,
                                6,
                                9
                        ),
                        new ChunkManifest(
                                "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9",
                                1,
                                9,
                                10
                        )
                )
        );
    }

    @Test
    public void buildManifestChunkSize3Buffer1() {
        FileManifestBuilder underTest = new FileManifestBuilder(3, 1);

        FileManifest actual = underTest.build(file);
        assertThat(
                actual.hash(),
                is("c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646")
        );
        assertThat(
                actual.size(),
                is(10L)
        );
        assertThat(
                actual.fileName(),
                is("readStream.txt")
        );
        assertThat(
                actual.chunkManifests().size(),
                is(4)
        );
        assertThat(
                actual.chunkManifests(),
                hasItems(
                        new ChunkManifest(
                                "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
                                3,
                                0,
                                3
                        ),
                        new ChunkManifest(
                                "b3a8e0e1f9ab1bfe3a36f231f676f78bb30a519d2b21e6c530c0eee8ebb4a5d0",
                                3,
                                3,
                                6
                        ),
                        new ChunkManifest(
                                "35a9e381b1a27567549b5f8a6f783c167ebf809f1c4d6a9e367240484d8ce281",
                                3,
                                6,
                                9
                        ),
                        new ChunkManifest(
                                "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9",
                                1,
                                9,
                                10
                        )
                )
        );
    }

    @Test
    public void buildManifestChunkSize3BufferMax() {
        FileManifestBuilder underTest = new FileManifestBuilder(3, Integer.MAX_VALUE);

        FileManifest actual = underTest.build(file);
        assertThat(
                actual.hash(),
                is("c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646")
        );
        assertThat(
                actual.size(),
                is(10L)
        );
        assertThat(
                actual.fileName(),
                is("readStream.txt")
        );
        assertThat(
                actual.chunkManifests().size(),
                is(4)
        );
        assertThat(
                actual.chunkManifests(),
                hasItems(
                        new ChunkManifest(
                                "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
                                3,
                                0,
                                3
                        ),
                        new ChunkManifest(
                                "b3a8e0e1f9ab1bfe3a36f231f676f78bb30a519d2b21e6c530c0eee8ebb4a5d0",
                                3,
                                3,
                                6
                        ),
                        new ChunkManifest(
                                "35a9e381b1a27567549b5f8a6f783c167ebf809f1c4d6a9e367240484d8ce281",
                                3,
                                6,
                                9
                        ),
                        new ChunkManifest(
                                "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9",
                                1,
                                9,
                                10
                        )
                )
        );
    }

    @Test
    public void buildManifestChunkSize9() {
        FileManifestBuilder underTest = new FileManifestBuilder(9, 4);

        FileManifest actual = underTest.build(file);
        assertThat(
                actual.hash(),
                is("c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646")
        );
        assertThat(
                actual.size(),
                is(10L)
        );
        assertThat(
                actual.fileName(),
                is("readStream.txt")
        );
        assertThat(
                actual.chunkManifests().size(),
                is(2)
        );
        assertThat(
                actual.chunkManifests(),
                hasItems(
                        new ChunkManifest(
                                "15e2b0d3c33891ebb0f1ef609ec419420c20e320ce94c65fbc8c3312448eb225",
                                9,
                                0,
                                9
                        ),
                        new ChunkManifest(
                                "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9",
                                1,
                                9,
                                10
                        )
                )
        );
    }

    @Test
    public void buildManifestChunkSize() {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, 4);

        FileManifest actual = underTest.build(file);
        assertThat(
                actual.hash(),
                is("c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646")
        );
        assertThat(
                actual.size(),
                is(10L)
        );
        assertThat(
                actual.fileName(),
                is("readStream.txt")
        );
        assertThat(
                actual.chunkManifests().size(),
                is(1)
        );
        assertThat(
                actual.chunkManifests(),
                hasItems(
                        new ChunkManifest(
                                "c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646",
                                10,
                                0,
                                10
                        )
                )
        );
    }

    @Test
    public void validateChunkSize() {
        FileManifestBuilder underTest = new FileManifestBuilder(10, Integer.MAX_VALUE);

        assertDoesNotThrow(() -> {
            underTest.validate(new FileManifest("filename", "hash", Integer.MAX_VALUE, List.of(
                    new ChunkManifest("hash", 1, 1, 2)
            )));
        });
        assertDoesNotThrow(() -> {
            underTest.validate(new FileManifest("filename", "hash", Integer.MAX_VALUE, List.of(
                    new ChunkManifest("hash", 9, 1, 2)
            )));
        });
        assertDoesNotThrow(() -> {
            underTest.validate(new FileManifest("filename", "hash", Integer.MAX_VALUE, List.of(
                    new ChunkManifest("hash", 10, 1, 2)
            )));
        });
    }

    @Test
    public void validateChunkSizeNegative() {
        FileManifestBuilder underTest = new FileManifestBuilder(10, Integer.MAX_VALUE);

        assertThrows(RuntimeException.class, () -> {
            underTest.validate(new FileManifest("filename", "hash", Integer.MAX_VALUE, List.of(
            )));
        });

        assertThrows(RuntimeException.class, () -> {
            underTest.validate(new FileManifest("filename", "hash", Integer.MAX_VALUE, List.of(
                    new ChunkManifest("hash", 11, 1, 2)
            )));
        });

        assertThrows(RuntimeException.class, () -> {
            underTest.validate(new FileManifest("filename", "hash", Integer.MAX_VALUE, List.of(
                    new ChunkManifest("hash", 0, 1, 2)
            )));
        });

        assertThrows(RuntimeException.class, () -> {
            underTest.validate(new FileManifest("filename", "hash", Integer.MAX_VALUE, List.of(
                    new ChunkManifest("hash", 5, 2, 2)
            )));
        });

        assertThrows(RuntimeException.class, () -> {
            underTest.validate(new FileManifest("filename", "hash", Integer.MAX_VALUE, List.of(
                    new ChunkManifest("hash", 5, 3, 2)
            )));
        });
    }

    @Test
    public void readBuffer2Skip1Take5() throws Exception {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, Integer.MAX_VALUE);

        try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(2);
            List<String> numbers = new ArrayList<>();
            underTest.read(fileChannel, 1, 5, byteBuffer, buffer -> {
                byte[] bytes = new byte[byteBuffer.remaining()];
                buffer.get(bytes);
                numbers.add(new String(bytes));
            });
            assertThat(numbers.size(), is(3));
            assertThat(
                    numbers,
                    hasItems(
                            "23",
                            "45",
                            "6"
                    )
            );
        }
    }

    @Test
    public void readBuffer8Skip0Take12() throws Exception {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, Integer.MAX_VALUE);

        try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            List<String> numbers = new ArrayList<>();
            underTest.read(fileChannel, 0, 12, byteBuffer, buffer -> {
                byte[] bytes = new byte[byteBuffer.remaining()];
                buffer.get(bytes);
                numbers.add(new String(bytes));
            });
            assertThat(numbers.size(), is(2));
            assertThat(
                    numbers,
                    hasItems(
                            "12345678",
                            "90"
                    )
            );
        }
    }

    @Test
    public void readBuffer12Skip0Take12() throws Exception {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, Integer.MAX_VALUE);

        try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(12);
            List<String> numbers = new ArrayList<>();
            underTest.read(fileChannel, 0, 12, byteBuffer, buffer -> {
                byte[] bytes = new byte[byteBuffer.remaining()];
                buffer.get(bytes);
                numbers.add(new String(bytes));
            });
            assertThat(numbers.size(), is(1));
            assertThat(
                    numbers,
                    hasItems(
                            "1234567890"
                    )
            );
        }
    }
}
