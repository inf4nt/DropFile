package com.evolution.dropfiledaemon.download.procedure.manifest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class FileManifestServiceTest {

    private static final String FILE_HASH = "632e36884eab27fb6bd3d6ddd08e19a935373710984981b816347c5c5128655e";

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
    void build_ShouldReturnEmptyChunks_WhenFileSizeIsZero() {
        FileManifestService service = new FileManifestService(1000);

        FileManifest manifest = service.build(FILE_HASH, 0);

        assertThat(manifest.size(), is(0L));
        assertThat(manifest.chunks(), is(empty()));
    }
}
