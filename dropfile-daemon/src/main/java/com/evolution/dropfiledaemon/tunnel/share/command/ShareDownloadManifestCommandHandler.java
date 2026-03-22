package com.evolution.dropfiledaemon.tunnel.share.command;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.manifest.FileManifest;
import com.evolution.dropfiledaemon.manifest.FileManifestBuilder;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;

@RequiredArgsConstructor
@Component
public class ShareDownloadManifestCommandHandler
        implements CommandHandler<ShareDownloadManifestCommandRequest, FileManifest> {

    private final ApplicationConfigStore applicationConfigStore;

    private final FileManifestBuilder fileManifestBuilder;

    @Override
    public String getCommandName() {
        return "share-download-manifest";
    }

    @Override
    public Class<ShareDownloadManifestCommandRequest> getPayloadType() {
        return ShareDownloadManifestCommandRequest.class;
    }

    @Override
    public FileManifest handle(ShareDownloadManifestCommandRequest request) {
        ShareFileEntry fileEntry = applicationConfigStore.getShareFileEntryStore()
                .getRequired(request.fileId()).getValue();

        int chunkSize = fileManifestBuilder.getChunkSize(request.chunkSize());

        return fileManifestBuilder.build(new File(fileEntry.absolutePath()), chunkSize);
    }
}
