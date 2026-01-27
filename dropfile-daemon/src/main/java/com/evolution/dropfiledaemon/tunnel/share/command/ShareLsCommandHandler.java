package com.evolution.dropfiledaemon.tunnel.share.command;

import com.evolution.dropfiledaemon.tunnel.share.dto.ShareLsTunnelResponse;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShareLsCommandHandler implements CommandHandler<Void, List<ShareLsTunnelResponse>> {

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
    public List<ShareLsTunnelResponse> handle(Void unused) {
        return shareFileEntryStore
                .getAll()
                .entrySet()
                .stream()
                .map(it -> new ShareLsTunnelResponse(it.getKey(), it.getValue().alias()))
                .toList();
    }
}
