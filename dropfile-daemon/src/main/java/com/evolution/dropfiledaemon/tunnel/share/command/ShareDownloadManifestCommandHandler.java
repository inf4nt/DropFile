package com.evolution.dropfiledaemon.tunnel.share.command;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.manifest.FileManifest;
import com.evolution.dropfiledaemon.manifest.FileManifestBuilder;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestTunnelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;

@RequiredArgsConstructor
@Component
public class ShareDownloadManifestCommandHandler
        implements CommandHandler<String, ShareDownloadManifestTunnelResponse> {

    private final ApplicationConfigStore applicationConfigStore;

    private final FileManifestBuilder fileManifestBuilder;

    @Override
    public String getCommandName() {
        return "share-download-manifest";
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @Override
    public ShareDownloadManifestTunnelResponse handle(String id) {
        ShareFileEntry fileEntry = applicationConfigStore.getShareFileEntryStore()
                .getRequired(id).getValue();
        FileManifest fileManifest = fileManifestBuilder.build(new File(fileEntry.absolutePath()));

        return new ShareDownloadManifestTunnelResponse(
                id,
                fileManifest.hash(),
                fileManifest.size(),
                fileManifest.chunkManifests()
                        .stream()
                        .map(it -> new ShareDownloadManifestTunnelResponse.ChunkManifest(
                                it.hash(),
                                it.size(),
                                it.startPosition(),
                                it.endPosition()
                        ))
                        .toList()
        );
    }
}
