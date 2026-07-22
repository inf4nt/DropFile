package com.evolution.dropfiledaemon.tunnel.command;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.manifest.FileManifest;
import com.evolution.dropfiledaemon.manifest.FileManifestBuilder;
import com.evolution.dropfiledaemon.tunnel.framework.server.command.CommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareDownloadManifestCommandRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.File;

@RequiredArgsConstructor
@Component
public class ShareDownloadManifestCommandHandler
        implements CommandHandler<ShareDownloadManifestCommandRequest, FileManifest> {

    public static final String COMMAND_NAME = "share-download-manifest";

    private final FileManifestBuilder fileManifestBuilder;

    private final ShareFileEntryStore shareFileEntryStore;

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<ShareDownloadManifestCommandRequest> getPayloadType() {
        return ShareDownloadManifestCommandRequest.class;
    }

    @SneakyThrows
    @Override
    public FileManifest handle(ShareDownloadManifestCommandRequest request) {
        ShareFileEntry fileEntry = shareFileEntryStore
                .getRequired(request.fileId()).getValue();

        int chunkSize = fileManifestBuilder.getChunkSize(request.chunkSize());

        return fileManifestBuilder.build(
                new File(fileEntry.resourcePath()).toPath(),
                fileEntry.alias(),
                chunkSize
        );
    }
}
