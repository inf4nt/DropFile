package com.evolution.dropfiledaemon.share;

import com.evolution.dropfile.common.dto.DownloadFileTunnelResponse;
import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.ActionHandler;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Component
public class ShareDownloadActionHandler
        implements ActionHandler<String, DownloadFileTunnelResponse> {

    private final ShareFileEntryStore shareFileEntryStore;

    @Autowired
    public ShareDownloadActionHandler(ShareFileEntryStore shareFileEntryStore) {
        this.shareFileEntryStore = shareFileEntryStore;
    }

    @Override
    public String getAction() {
        return "share-download";
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @SneakyThrows
    @Override
    public DownloadFileTunnelResponse handle(String id) {
        Map.Entry<String, ShareFileEntry> fileEntry = shareFileEntryStore.get(id).orElseThrow();
        byte[] allBytes = Files.readAllBytes(Paths.get(fileEntry.getValue().absolutePath()));
        return new DownloadFileTunnelResponse(fileEntry.getKey(), fileEntry.getValue().alias(), allBytes);
    }
}
