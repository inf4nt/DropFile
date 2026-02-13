package com.evolution.dropfiledaemon.tunnel.share.command;

import com.evolution.dropfile.common.dto.DownloadFileTunnelResponse;
import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Deprecated
@RequiredArgsConstructor
@Component
public class ShareDownloadCommandHandler
        implements CommandHandler<String, DownloadFileTunnelResponse> {

    private final ApplicationConfigStore applicationConfigStore;

    @Override
    public String getCommandName() {
        return "share-download";
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @SneakyThrows
    @Override
    public DownloadFileTunnelResponse handle(String id) {
        Map.Entry<String, ShareFileEntry> fileEntry = applicationConfigStore.getShareFileEntryStore()
                .get(id).orElseThrow();
        byte[] allBytes = Files.readAllBytes(Paths.get(fileEntry.getValue().absolutePath()));
        return new DownloadFileTunnelResponse(fileEntry.getKey(), fileEntry.getValue().alias(), allBytes);
    }
}
