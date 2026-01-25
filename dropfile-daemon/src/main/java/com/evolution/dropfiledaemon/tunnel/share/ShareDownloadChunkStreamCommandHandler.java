package com.evolution.dropfiledaemon.tunnel.share;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkTunnelRequest;
import com.evolution.dropfiledaemon.util.FileHelper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;

@Component
public class ShareDownloadChunkStreamCommandHandler
        implements CommandHandler<ShareDownloadChunkTunnelRequest, InputStream> {

    private final ShareFileEntryStore shareFileEntryStore;

    private final FileHelper fileHelper;

    @Autowired
    public ShareDownloadChunkStreamCommandHandler(ShareFileEntryStore shareFileEntryStore,
                                                  FileHelper fileHelper) {
        this.shareFileEntryStore = shareFileEntryStore;
        this.fileHelper = fileHelper;
    }

    @Override
    public String getCommandName() {
        return "share-download-chunk-stream";
    }

    @Override
    public Class<ShareDownloadChunkTunnelRequest> getPayloadType() {
        return ShareDownloadChunkTunnelRequest.class;
    }

    @SneakyThrows
    @Override
    public InputStream handle(ShareDownloadChunkTunnelRequest request) {
        ShareFileEntry shareFileEntry = shareFileEntryStore.getRequired(request.id())
                .getValue();

        File file = new File(shareFileEntry.absolutePath());
        long skip = request.startPosition();
        int take = toInt(request.endPosition() - request.startPosition());

        System.out.println("throttling 3 sec");
        Thread.sleep(3000);

        return fileHelper.readStream(file, skip, take);
    }

    private int toInt(long value) {
        if (Integer.MAX_VALUE < value) {
            throw new IllegalArgumentException("Long value is greater than: " + Integer.MAX_VALUE);
        }
        return Math.toIntExact(value);
    }
}
