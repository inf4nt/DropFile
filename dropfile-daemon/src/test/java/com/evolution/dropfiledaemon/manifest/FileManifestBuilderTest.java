package com.evolution.dropfiledaemon.manifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
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
                Objects.requireNonNull(getClass().getClassLoader().getResource("numbers.txt")).getFile()
        );
    }

    @Test
    public void build_ShouldThrow_WhenNoFileFound() {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE);

        assertThrows(FileNotFoundException.class, () -> {
            underTest.build(new File("fake-file.txt").toPath(), "fake-file.txt", Integer.MAX_VALUE);
        });
    }

    @Test
    public void build_ShouldThrow_WhenGivenPathIsDirectory() {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE);

        assertThrows(
                UnsupportedOperationException.class,
                () -> underTest.build(new File("").toPath(), "fake-file.txt", Integer.MAX_VALUE)
        );
    }

    @Test
    public void build_ShouldPass_WhenChunkSizeIs3() throws Exception {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE);

        FileManifest actual = underTest.build(file.toPath(), "alias.txt", 3);

        assertDoesNotThrow(() -> {
            underTest.validate(actual);
        });

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
                is("alias.txt")
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
                                0
                        ),
                        new ChunkManifest(
                                "b3a8e0e1f9ab1bfe3a36f231f676f78bb30a519d2b21e6c530c0eee8ebb4a5d0",
                                3,
                                3
                        ),
                        new ChunkManifest(
                                "35a9e381b1a27567549b5f8a6f783c167ebf809f1c4d6a9e367240484d8ce281",
                                3,
                                6
                        ),
                        new ChunkManifest(
                                "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9",
                                1,
                                9
                        )
                )
        );
    }

    @Test
    public void build_ShouldPass_WhenChunkSizeIs9() throws Exception {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE);

        FileManifest actual = underTest.build(file.toPath(), "alias.txt", 9);

        assertDoesNotThrow(() -> {
            underTest.validate(actual);
        });

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
                is("alias.txt")
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
                                0
                        ),
                        new ChunkManifest(
                                "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9",
                                1,
                                9
                        )
                )
        );
    }

    @Test
    public void build_ShouldPass_WhenChunkSizeIsIntegerMaxValue() throws Exception {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE);

        FileManifest actual = underTest.build(file.toPath(), "alias.txt", Integer.MAX_VALUE);

        assertDoesNotThrow(() -> {
            underTest.validate(actual);
        });

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
                is("alias.txt")
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
                                0
                        )
                )
        );
    }

    @Test
    public void getChunkSize_ShouldReturnMaxSize_WhenRequestedIsMaxInteger() {
        FileManifestBuilder underTest = new FileManifestBuilder(10);

        assertThat(
                underTest.getChunkSize(Integer.MAX_VALUE),
                is(10)
        );
    }

    @Test
    public void getChunkSize_ShouldReturnMaxSize_WhenRequestedIsGreaterThanMaxSize() {
        FileManifestBuilder underTest = new FileManifestBuilder(10);

        assertThat(
                underTest.getChunkSize(11),
                is(10)
        );
    }

    @Test
    public void getChunkSize_ShouldReturnMaxSize_WhenRequestedIsEqualToMaxSize() {
        FileManifestBuilder underTest = new FileManifestBuilder(10);

        assertThat(
                underTest.getChunkSize(10),
                is(10)
        );
    }

    @Test
    public void getChunkSize_ShouldReturnRequestedSize_WhenRequestedIsLessThanMaxSize() {
        FileManifestBuilder underTest = new FileManifestBuilder(10);

        assertThat(
                underTest.getChunkSize(9),
                is(9)
        );
    }

    @Test
    void validate_ShouldPass_WhenManifestIsValidAndOrdered() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest("hash1", 3, 0),
                new ChunkManifest("hash2", 2, 3)
        );
        FileManifest manifest = new FileManifest("file", "hash", 5, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5);
        assertDoesNotThrow(() -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldPass_WhenManifestIsValidButUnordered() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest("hash3", 1, 9),
                new ChunkManifest("hash1", 5, 0),
                new ChunkManifest("hash2", 4, 5)
        );
        FileManifest manifest = new FileManifest("file", "hash", 10, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5);
        assertDoesNotThrow(() -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunksListIsNull() {
        FileManifest manifest = new FileManifest("file", "hash", 0, null);

        FileManifestBuilder underTest = new FileManifestBuilder(5);
        assertThrows(IllegalStateException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunksListIsEmpty() {
        FileManifest manifest = new FileManifest("file", "hash", 0, List.of());

        FileManifestBuilder underTest = new FileManifestBuilder(5);
        assertThrows(IllegalStateException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunkSizeIsZeroOrNegative() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest("hash1", 0, 0)
        );
        FileManifest manifest = new FileManifest("file", "hash", 0, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunkPositionIsNegative() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest("hash1", 3, -1)
        );
        FileManifest manifest = new FileManifest("file", "hash", 3, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunkIsOversized() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest("hash1", 6, 0)
        );
        FileManifest manifest = new FileManifest("file", "hash", 6, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenTotalSizeDoesNotMatchSumOfChunks() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest("hash1", 3, 0),
                new ChunkManifest("hash2", 2, 3)
        );
        FileManifest manifest = new FileManifest("file", "hash", 10, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenThereIsAGapBetweenChunks() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest("hash1", 3, 0),
                new ChunkManifest("hash2", 2, 4)
        );
        FileManifest manifest = new FileManifest("file", "hash", 5, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunksOverlap() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest("hash1", 3, 0),
                new ChunkManifest("hash2", 2, 2)
        );
        FileManifest manifest = new FileManifest("file", "hash", 5, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenNoChunkStartsAtZero() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest("hash1", 3, 1)
        );
        FileManifest manifest = new FileManifest("file", "hash", 3, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }
}
