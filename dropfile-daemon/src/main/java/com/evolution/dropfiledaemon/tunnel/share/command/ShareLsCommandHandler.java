package com.evolution.dropfiledaemon.tunnel.share.command;

import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareLsTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareLsTunnelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ShareLsCommandHandler implements CommandHandler<ShareLsTunnelRequest, List<ShareLsTunnelResponse>> {

    private final ApplicationConfigStore applicationConfigStore;

    @Override
    public String getCommandName() {
        return "share-ls";
    }

    @Override
    public Class<ShareLsTunnelRequest> getPayloadType() {
        return ShareLsTunnelRequest.class;
    }

    @Override
    public List<ShareLsTunnelResponse> handle(ShareLsTunnelRequest request) {
        List<String> ids = request.ids();

        return applicationConfigStore
                .getShareFileEntryStore()
                .getAll()
                .entrySet()
                .stream()
                .filter(entry -> {
                    if (ObjectUtils.isEmpty(request.ids())) {
                        return true;
                    }
                    return ids.stream().anyMatch(id -> entry.getKey().startsWith(id));
                })
                .map(it -> new ShareLsTunnelResponse(
                        it.getKey(),
                        it.getValue().alias(),
                        it.getValue().size(),
                        it.getValue().created()
                ))
                .toList();
    }
}
