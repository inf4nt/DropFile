package com.evolution.dropfiledaemon.tunnel.share.command;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.manifest.FileManifest;
import com.evolution.dropfiledaemon.manifest.FileManifestBuilder;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;

@RequiredArgsConstructor
@Component
public class ShareDownloadManifestCommandHandler
        implements CommandHandler<String, FileManifest> {

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
    public FileManifest handle(String id) {
        ShareFileEntry fileEntry = applicationConfigStore.getShareFileEntryStore()
                .getRequired(id).getValue();
        return fileManifestBuilder.build(new File(fileEntry.absolutePath()));
    }
}
