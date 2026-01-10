package com.evolution.dropfiledaemon.tunnel.share;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.manifest.FileManifest;
import com.evolution.dropfiledaemon.manifest.FileManifestBuilder;
import com.evolution.dropfiledaemon.tunnel.framework.ActionHandler;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;

@Component
public class ShareDownloadManifestActionHandler
        implements ActionHandler<String, ShareDownloadManifestResponse> {

    private final ShareFileEntryStore shareFileEntryStore;

    private final FileManifestBuilder fileManifestBuilder;

    @Autowired
    public ShareDownloadManifestActionHandler(ShareFileEntryStore shareFileEntryStore,
                                              FileManifestBuilder fileManifestBuilder) {
        this.shareFileEntryStore = shareFileEntryStore;
        this.fileManifestBuilder = fileManifestBuilder;
    }

    @Override
    public String getAction() {
        return "share-download-manifest";
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @Override
    public ShareDownloadManifestResponse handle(String id) {
        ShareFileEntry fileEntry = shareFileEntryStore.get(id)
                .map(it -> it.getValue())
                .orElse(null);

        if (fileEntry == null) {
            throw new RuntimeException("No shared file entry found: " + id);
        }

        File file = new File(fileEntry.absolutePath());

        if (!Files.exists(file.toPath())) {
            throw new RuntimeException("No shared file found: " + file.getAbsolutePath());
        }

        FileManifest fileManifest = fileManifestBuilder.build(file);
        return new ShareDownloadManifestResponse(
                id,
                fileManifest.size(),
                fileManifest.chunkManifests()
                        .stream()
                        .map(it -> new ShareDownloadManifestResponse.ChunkManifest(
                                it.startPosition(),
                                it.endPosition(),
                                it.size(),
                                it.hash()
                        ))
                        .toList()
        );
    }
}
