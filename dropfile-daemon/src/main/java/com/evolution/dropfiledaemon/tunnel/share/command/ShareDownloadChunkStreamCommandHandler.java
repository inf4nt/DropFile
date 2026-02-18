package com.evolution.dropfiledaemon.tunnel.share.command;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkStreamTunnelRequest;
import com.evolution.dropfiledaemon.util.FileHelper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class ShareDownloadChunkStreamCommandHandler
        implements CommandHandler<ShareDownloadChunkStreamTunnelRequest, InputStream> {

    private final ApplicationConfigStore applicationConfigStore;

    private final FileHelper fileHelper;

    @Override
    public String getCommandName() {
        return "share-download-chunk-stream";
    }

    @Override
    public Class<ShareDownloadChunkStreamTunnelRequest> getPayloadType() {
        return ShareDownloadChunkStreamTunnelRequest.class;
    }

//    int count = 0;
//    int error = 0;

    @SneakyThrows
    @Override
    public InputStream handle(ShareDownloadChunkStreamTunnelRequest request) {
//        count++;
//        if (count > 100 && error <= 5) {
//            error++;
//            return new ByteArrayInputStream(UUID.randomUUID().toString().getBytes());
//        }

        ShareFileEntry shareFileEntry = applicationConfigStore.getShareFileEntryStore()
                .getRequired(request.id())
                .getValue();

        File file = new File(shareFileEntry.absolutePath());
        long skip = request.startPosition();
        int take = toInt(request.endPosition() - request.startPosition());

        return fileHelper.readStream(file, skip, take);
    }

    private int toInt(long value) {
        if (Integer.MAX_VALUE < value) {
            throw new IllegalArgumentException("Long value is greater than: " + Integer.MAX_VALUE);
        }
        return Math.toIntExact(value);
    }
}
