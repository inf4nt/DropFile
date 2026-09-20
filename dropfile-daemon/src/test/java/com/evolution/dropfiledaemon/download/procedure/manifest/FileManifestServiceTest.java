package com.evolution.dropfiledaemon.download.procedure.manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileManifestServiceTest {

    private static final String FILE_HASH = "632e36884eab27fb6bd3d6ddd08e19a935373710984981b816347c5c5128655e";

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void shouldThrowException_WhenChunkSizeIsInvalid(int invalidChunkSize) {
        assertThrows(IllegalArgumentException.class, () -> new FileManifestService(invalidChunkSize));
    }

    @Test
    void shouldThrowException_WhenFileSizeIsNegative() {
        FileManifestService service = new FileManifestService(1000);

        assertThrows(IllegalArgumentException.class, () -> service.build(FILE_HASH, -1L));
    }

    @Test
    void shouldThrowException_WhenFileSizeExceedsMaxSupportedLimit() {
        FileManifestService service = new FileManifestService(1000);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.build(FILE_HASH, FileManifestService.MAX_SUPPORTED_FILE_SIZE + 1L)
        );
    }

    @Test
    void shouldThrowException_WhenChunkCountExceedsIntegerMaxValue() {
        FileManifestService service = new FileManifestService(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.build(FILE_HASH, (long) Integer.MAX_VALUE + 1L)
        );
    }

    @Test
    void shouldBuildManifestCorrectly_WithLazyChunks() {
        int chunkSize = 1000;
        FileManifestService service = new FileManifestService(chunkSize);

        long fileSize = 2500L;
        FileManifest manifest = service.build(FILE_HASH, fileSize);

        assertThat(manifest, notNullValue());
        assertThat(manifest.size(), is(fileSize));
        assertThat(manifest.chunks().size(), is(3));

        ChunkManifest chunk0 = manifest.chunks().getFirst();
        assertThat(chunk0.position(), is(0L));
        assertThat(chunk0.size(), is(1000));

        ChunkManifest chunk1 = manifest.chunks().get(1);
        assertThat(chunk1.position(), is(1000L));
        assertThat(chunk1.size(), is(1000));

        ChunkManifest chunk2 = manifest.chunks().get(2);
        assertThat(chunk2.position(), is(2000L));
        assertThat(chunk2.size(), is(500));
    }

    @Test
    void shouldThrowException_WhenAccessingInvalidChunkIndex() {
        FileManifestService service = new FileManifestService(1000);
        FileManifest manifest = service.build(FILE_HASH, 2500L); // 3 чанка (индексы 0, 1, 2)

        assertThrows(IndexOutOfBoundsException.class, () -> manifest.chunks().get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> manifest.chunks().get(3));
    }

    @Test
    void build_ShouldCreateMultipleChunks_WhenFileSizeIsGreaterThanChunkSize() {
        FileManifestService service = new FileManifestService(300);
        long fileSize = 1000;

        FileManifest manifest = service.build(FILE_HASH, fileSize);

        assertThat(manifest, notNullValue());
        assertThat(manifest.hash(), is(FILE_HASH));
        assertThat(manifest.size(), is(fileSize));

        assertThat(manifest.chunks(), contains(
                new ChunkManifest(300, 0),
                new ChunkManifest(300, 300),
                new ChunkManifest(300, 600),
                new ChunkManifest(100, 900)
        ));
    }

    @Test
    void build_ShouldCreateExactChunks_WhenFileSizeIsExactMultipleOfChunkSize() {
        FileManifestService service = new FileManifestService(300);
        long fileSize = 900;

        FileManifest manifest = service.build(FILE_HASH, fileSize);

        assertThat(manifest.chunks(), contains(
                new ChunkManifest(300, 0),
                new ChunkManifest(300, 300),
                new ChunkManifest(300, 600)
        ));
    }

    @Test
    void build_ShouldCreateSingleChunk_WhenFileSizeIsLessThanChunkSize() {
        FileManifestService service = new FileManifestService(1000);
        long fileSize = 250;

        FileManifest manifest = service.build(FILE_HASH, fileSize);

        assertThat(manifest.chunks(), contains(
                new ChunkManifest(250, 0)
        ));
    }

    @Test
    void shouldBuildManifest_WhenFileSizeIsZero() {
        FileManifestService service = new FileManifestService(1000);

        FileManifest manifest = service.build(FILE_HASH, 0);

        assertThat(manifest, notNullValue());
        assertThat(manifest.size(), is(0L));
        assertThat(manifest.chunks().size(), is(0));
        assertThat(manifest.chunks().isEmpty(), is(true));
    }
}
