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

    private final FileHelper fileHelper;

    private final ShareFileEntryStore shareFileEntryStore;

    @Override
    public String getCommandName() {
        return "share-download-chunk-stream";
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

        File file = new File(shareFileEntry.absolutePath());
        long skip = request.position();
        int take = request.size();

        return fileHelper.readStream(file.toPath(), skip, take);
    }
}
