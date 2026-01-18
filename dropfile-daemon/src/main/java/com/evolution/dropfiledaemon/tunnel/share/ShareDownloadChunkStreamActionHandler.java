package com.evolution.dropfiledaemon.tunnel.share;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.manifest.FileHelper;
import com.evolution.dropfiledaemon.tunnel.framework.ActionHandler;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkTunnelRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

@Component
public class ShareDownloadChunkStreamActionHandler
        implements ActionHandler<ShareDownloadChunkTunnelRequest, InputStream> {

    private final ShareFileEntryStore shareFileEntryStore;

    private final FileHelper fileHelper;

    @Autowired
    public ShareDownloadChunkStreamActionHandler(ShareFileEntryStore shareFileEntryStore,
                                                 FileHelper fileHelper) {
        this.shareFileEntryStore = shareFileEntryStore;
        this.fileHelper = fileHelper;
    }

    @Override
    public String getAction() {
        return "share-download-chunk-stream";
    }

    @Override
    public Class<ShareDownloadChunkTunnelRequest> getPayloadType() {
        return ShareDownloadChunkTunnelRequest.class;
    }

    int count = 0;

    @Override
    public InputStream handle(ShareDownloadChunkTunnelRequest request) {
        ShareFileEntry shareFileEntry = shareFileEntryStore.get(request.id())
                .map(it -> it.getValue())
                .orElseThrow(() -> new RuntimeException("No found shared file: " + request.id()));

        File file = new File(shareFileEntry.absolutePath());
        if (!Files.exists(file.toPath())) {
            throw new RuntimeException("File does not exist: " + file.getAbsolutePath());
        }

//        if (count <= 1) {
//            count++;
//            System.out.println("SLEEPING!");
//            try {
//                Thread.sleep(35000);
//            } catch (Exception e) {
//                System.out.println("ERROR HAS HAPPENED: " + e.getMessage());
//                throw new RuntimeException(e);
//            }
//        }

//        System.out.println("SLEEPING 50 SEC !");
//        try {
//            Thread.sleep(50000);
//        } catch (Exception e) {
//            System.out.println("ERROR HAS HAPPENED: " + e.getMessage());
//            throw new RuntimeException(e);
//        }
//        System.out.println("DONE I AM AWAKE !!!");

        long skip = request.startPosition();
        int take = toInt(request.endPosition() - request.startPosition());
        byte[] data = fileHelper.read(file, skip, take);

        return new ByteArrayInputStream(data);
    }

    private int toInt(long value) {
        if (Integer.MAX_VALUE < value) {
            throw new IllegalArgumentException("Long value is greater than: " + Integer.MAX_VALUE);
        }
        return Math.toIntExact(value);
    }
}
