package com.evolution.dropfiledaemon.tunnel.command;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareDownloadChunkStreamTunnelRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;

@RequiredArgsConstructor
@Component
public class ShareDownloadChunkStreamCommandHandler
        implements CommandHandler<ShareDownloadChunkStreamTunnelRequest, InputStream> {

    public static final String COMMAND_NAME = "share-download-chunk-stream";

    private final FileHelper fileHelper;

    private final ShareFileEntryStore shareFileEntryStore;

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<ShareDownloadChunkStreamTunnelRequest> getPayloadType() {
        return ShareDownloadChunkStreamTunnelRequest.class;
    }

    @SneakyThrows
    @Override
    public InputStream handle(ShareDownloadChunkStreamTunnelRequest request) {
        ShareFileEntry shareFileEntry = shareFileEntryStore
                .getRequired(request.id())
                .getValue();

        File file = new File(shareFileEntry.resourcePath());
        long skip = request.position();
        int take = request.size();

        return fileHelper.readStream(file.toPath(), skip, take);
    }
}
