package com.evolution.dropfiledaemon.tunnel.share.command;

import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareLsTunnelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ShareLsCommandHandler implements CommandHandler<Void, List<ShareLsTunnelResponse>> {

    private final ApplicationConfigStore applicationConfigStore;

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
        return applicationConfigStore
                .getShareFileEntryStore()
                .getAll()
                .entrySet()
                .stream()
                .map(it -> new ShareLsTunnelResponse(
                        it.getKey(),
                        it.getValue().alias(),
                        it.getValue().size(),
                        it.getValue().created()
                ))
                .toList();
    }
}
