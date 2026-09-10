package com.evolution.dropfiledaemon.manifest;

import com.evolution.dropfile.common.io.FileHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Deprecated
public class FileManifestBuilderTest {

    private File file;

    private FileHelper fileHelper;

    @BeforeEach
    public void before() {
        fileHelper = Mockito.mock(FileHelper.class);
        file = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("numbers.txt")).getFile()
        );
    }

    @Test
    public void sha256_ShouldReturnCorrectHash_WhenFileExists() throws Exception {
        String expected = "1234";

        when(fileHelper.sha256(ArgumentMatchers.any())).thenReturn(expected);

        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, fileHelper);

        assertThat(
                underTest.build(file.toPath(), "test", 1).hash(),
                is(expected)
        );

        assertThat(
                underTest.build(file.toPath(), "test", Integer.MAX_VALUE).hash(),
                is(expected)
        );

        assertThat(
                underTest.build(file.toPath(), "test", (int) file.length()).hash(),
                is(expected)
        );
    }

    @Test
    public void build_ShouldThrow_WhenNoFileFound() {

        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, fileHelper);

        assertThrows(FileNotFoundException.class, () -> {
            underTest.build(new File("fake-file.txt").toPath(), "fake-file.txt", Integer.MAX_VALUE);
        });
    }

    @Test
    public void build_ShouldThrow_WhenGivenPathIsDirectory() {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, fileHelper);

        assertThrows(
                IllegalArgumentException.class,
                () -> underTest.build(new File("").toPath(), "fake-file.txt", Integer.MAX_VALUE)
        );
    }

    @Test
    public void build_ShouldPass_WhenChunkSizeIs3() throws Exception {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, fileHelper);

        FileManifest actual = underTest.build(file.toPath(), "alias.txt", 3);

        assertDoesNotThrow(() -> {
            underTest.validate(actual);
        });

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
                        new ChunkManifest(3, 0),
                        new ChunkManifest(3, 3),
                        new ChunkManifest(3, 6),
                        new ChunkManifest(1, 9)
                )
        );
    }

    @Test
    public void build_ShouldPass_WhenChunkSizeIs9() throws Exception {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, fileHelper);

        FileManifest actual = underTest.build(file.toPath(), "alias.txt", 9);

        assertDoesNotThrow(() -> {
            underTest.validate(actual);
        });

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
                        new ChunkManifest(9, 0),
                        new ChunkManifest(1, 9)
                )
        );
    }

    @Test
    public void build_ShouldPass_WhenChunkSizeIsIntegerMaxValue() throws Exception {
        FileManifestBuilder underTest = new FileManifestBuilder(Integer.MAX_VALUE, fileHelper);

        FileManifest actual = underTest.build(file.toPath(), "alias.txt", Integer.MAX_VALUE);

        assertDoesNotThrow(() -> {
            underTest.validate(actual);
        });

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
                        new ChunkManifest(10, 0)
                )
        );
    }

    @Test
    public void getChunkSize_ShouldReturnMaxSize_WhenRequestedIsMaxInteger() {
        FileManifestBuilder underTest = new FileManifestBuilder(10, fileHelper);

        assertThat(
                underTest.getChunkSize(Integer.MAX_VALUE),
                is(10)
        );
    }

    @Test
    public void getChunkSize_ShouldReturnMaxSize_WhenRequestedIsGreaterThanMaxSize() {
        FileManifestBuilder underTest = new FileManifestBuilder(10, fileHelper);

        assertThat(
                underTest.getChunkSize(11),
                is(10)
        );
    }

    @Test
    public void getChunkSize_ShouldReturnMaxSize_WhenRequestedIsEqualToMaxSize() {
        FileManifestBuilder underTest = new FileManifestBuilder(10, fileHelper);

        assertThat(
                underTest.getChunkSize(10),
                is(10)
        );
    }

    @Test
    public void getChunkSize_ShouldReturnRequestedSize_WhenRequestedIsLessThanMaxSize() {
        FileManifestBuilder underTest = new FileManifestBuilder(10, fileHelper);

        assertThat(
                underTest.getChunkSize(9),
                is(9)
        );
    }

    @Test
    void validate_ShouldPass_WhenManifestIsValidAndOrdered() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest(3, 0),
                new ChunkManifest(2, 3)
        );
        FileManifest manifest = new FileManifest("file", "hash", 5, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5, fileHelper);
        assertDoesNotThrow(() -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldPass_WhenManifestIsValidButUnordered() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest(1, 9),
                new ChunkManifest(5, 0),
                new ChunkManifest(4, 5)
        );
        FileManifest manifest = new FileManifest("file", "hash", 10, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5, fileHelper);
        assertDoesNotThrow(() -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunksListIsNull() {
        FileManifest manifest = new FileManifest("file", "hash", 0, null);

        FileManifestBuilder underTest = new FileManifestBuilder(5, fileHelper);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunksListIsEmpty() {
        FileManifest manifest = new FileManifest("file", "hash", 0, List.of());

        FileManifestBuilder underTest = new FileManifestBuilder(5, fileHelper);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunkSizeIsZeroOrNegative() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest(0, 0)
        );
        FileManifest manifest = new FileManifest("file", "hash", 0, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5, fileHelper);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunkPositionIsNegative() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest(3, -1)
        );
        FileManifest manifest = new FileManifest("file", "hash", 3, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5, fileHelper);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunkIsOversized() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest(6, 0)
        );
        FileManifest manifest = new FileManifest("file", "hash", 6, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5, fileHelper);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenTotalSizeDoesNotMatchSumOfChunks() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest(3, 0),
                new ChunkManifest(2, 3)
        );
        FileManifest manifest = new FileManifest("file", "hash", 10, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5, fileHelper);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenThereIsAGapBetweenChunks() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest(3, 0),
                new ChunkManifest(2, 4)
        );
        FileManifest manifest = new FileManifest("file", "hash", 5, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5, fileHelper);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenChunksOverlap() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest(3, 0),
                new ChunkManifest(2, 2)
        );
        FileManifest manifest = new FileManifest("file", "hash", 5, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5, fileHelper);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }

    @Test
    void validate_ShouldThrow_WhenNoChunkStartsAtZero() {
        List<ChunkManifest> chunks = List.of(
                new ChunkManifest(3, 1)
        );
        FileManifest manifest = new FileManifest("file", "hash", 3, chunks);

        FileManifestBuilder underTest = new FileManifestBuilder(5, fileHelper);
        assertThrows(IllegalArgumentException.class, () -> underTest.validate(manifest));
    }
}
