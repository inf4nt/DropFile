package com.evolution.dropfiledaemon.tunnel.command;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.store.share.ShareFileStore;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareLsTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareLsTunnelResponse;
import com.evolution.dropfiledaemon.tunnel.framework.server.command.CommandHandler;
import com.evolution.dropfiledaemon.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ShareLsCommandHandler
        implements CommandHandler<ShareLsTunnelRequest, List<ShareLsTunnelResponse>> {

    public static final String COMMAND_NAME = "share-ls";

    private final ShareFileStore shareFileStore;

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<ShareLsTunnelRequest> getPayloadType() {
        return ShareLsTunnelRequest.class;
    }

    @Override
    public List<ShareLsTunnelResponse> handle(ShareLsTunnelRequest request) {
        Collection<CriteriaEnvelope> criteriaEnvelopes = request.criteriaEnvelopes();

        return shareFileStore
                .getAll()
                .entrySet()
                .stream()
                .filter(it -> Utils.isAccessible(it.getValue()))
                .filter(entry -> {
                    if (ObjectUtils.isEmpty(request.criteriaEnvelopes())) {
                        return true;
                    }
                    return criteriaEnvelopes.stream()
                            .anyMatch(criteriaEnvelope -> entry.getKey().startsWith(criteriaEnvelope.value()));
                })
                .map(it -> new ShareLsTunnelResponse(
                        it.getKey(),
                        it.getValue().alias(),
                        it.getValue().hash(),
                        it.getValue().size(),
                        it.getValue().created()
                ))
                .toList();
    }
}
