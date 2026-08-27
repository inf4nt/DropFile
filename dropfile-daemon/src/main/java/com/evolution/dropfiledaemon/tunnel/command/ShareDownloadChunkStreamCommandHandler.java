package com.evolution.dropfiledaemon.tunnel.command;

import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfile.store.share.ShareFile;
import com.evolution.dropfile.store.share.ShareFileStore;
import com.evolution.dropfiledaemon.tunnel.framework.server.command.CommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareDownloadChunkStreamTunnelRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
@Component
public class ShareDownloadChunkStreamCommandHandler
        implements CommandHandler<ShareDownloadChunkStreamTunnelRequest, InputStream> {

    public static final String COMMAND_NAME = "share-download-chunk-stream";

    private final FileHelper fileHelper;

    private final ShareFileStore shareFileStore;

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
        ShareFile shareFile = shareFileStore
                .getRequired(request.id())
                .getValue();

        Path path = Paths.get(shareFile.resourcePath());
        long skip = request.position();
        int take = request.size();

        return fileHelper.readStream(path, skip, take);
    }
}
