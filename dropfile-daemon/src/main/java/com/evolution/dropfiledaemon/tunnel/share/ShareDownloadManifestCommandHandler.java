package com.evolution.dropfiledaemon.tunnel.share;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.manifest.FileManifest;
import com.evolution.dropfiledaemon.manifest.FileManifestBuilder;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class ShareDownloadManifestCommandHandler
        implements CommandHandler<String, ShareDownloadManifestResponse> {

    private final ShareFileEntryStore shareFileEntryStore;

    private final FileManifestBuilder fileManifestBuilder;

    @Autowired
    public ShareDownloadManifestCommandHandler(ShareFileEntryStore shareFileEntryStore,
                                               FileManifestBuilder fileManifestBuilder) {
        this.shareFileEntryStore = shareFileEntryStore;
        this.fileManifestBuilder = fileManifestBuilder;
    }

    @Override
    public String getCommandName() {
        return "share-download-manifest";
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @Override
    public ShareDownloadManifestResponse handle(String id) {
        ShareFileEntry fileEntry = shareFileEntryStore.getRequired(id).getValue();
        FileManifest fileManifest = fileManifestBuilder.build(new File(fileEntry.absolutePath()));

        return new ShareDownloadManifestResponse(
                id,
                fileManifest.hash(),
                fileManifest.size(),
                fileManifest.chunkManifests()
                        .stream()
                        .map(it -> new ShareDownloadManifestResponse.ChunkManifest(
                                it.hash(),
                                it.size(),
                                it.startPosition(),
                                it.endPosition()
                        ))
                        .toList()
        );
    }
}
