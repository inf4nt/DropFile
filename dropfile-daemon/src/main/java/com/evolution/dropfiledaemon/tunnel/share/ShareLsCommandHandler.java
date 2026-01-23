package com.evolution.dropfiledaemon.tunnel.share;

import com.evolution.dropfile.common.dto.FileEntryTunnelResponse;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShareLsCommandHandler implements CommandHandler<Void, List<FileEntryTunnelResponse>> {

    private final ShareFileEntryStore shareFileEntryStore;

    @Autowired
    public ShareLsCommandHandler(ShareFileEntryStore shareFileEntryStore) {
        this.shareFileEntryStore = shareFileEntryStore;
    }

    @Override
    public String getCommandName() {
        return "share-ls";
    }

    @Override
    public Class<Void> getPayloadType() {
        return Void.class;
    }

    @Override
    public List<FileEntryTunnelResponse> handle(Void unused) {
        return shareFileEntryStore
                .getAll()
                .entrySet()
                .stream()
                .map(it -> new FileEntryTunnelResponse(it.getKey(), it.getValue().alias()))
                .toList();
    }
}
