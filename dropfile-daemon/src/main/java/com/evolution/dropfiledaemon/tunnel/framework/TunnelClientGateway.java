package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfile.common.Attributes;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfiledaemon.tunnel.command.ShareDownloadChunkStreamCommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.ShareLsCommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.TunnelPingCommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareDownloadChunkStreamTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareLsTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareLsTunnelResponse;
import com.evolution.dropfiledaemon.tunnel.framework.client.TunnelClientRefreshableSessionDecorator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class TunnelClientGateway {

    private final ObjectMapper objectMapper;

    private final TunnelClientRefreshableSessionDecorator tunnelClient;

    public void ping(String fingerprint) {
        TunnelClient.Request request = TunnelClient.Request.builder(TunnelPingCommandHandler.COMMAND_NAME, fingerprint)
                .build();
        try (InputStream _ = tunnelClient.stream(request)) {
            // nothing to do
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    public List<ShareLsTunnelResponse> shareLs(String fingerprint, Collection<CriteriaEnvelope> criteriaEnvelopes) {
        TunnelClient.Request request = TunnelClient.Request.builder(ShareLsCommandHandler.COMMAND_NAME, fingerprint)
                .body(new ShareLsTunnelRequest(criteriaEnvelopes))
                .build();
        try (InputStream stream = tunnelClient.stream(request)) {
            return objectMapper.readValue(stream, new TypeReference<List<ShareLsTunnelResponse>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    public InputStream shareDownloadChunkStream(String fingerprint,
                                                String fileId,
                                                int size,
                                                long position,
                                                Attributes attributes) {
        TunnelClient.Request tunnelRequest = TunnelClient.Request.builder(
                        ShareDownloadChunkStreamCommandHandler.COMMAND_NAME,
                        fingerprint,
                        attributes
                )
                .body(new ShareDownloadChunkStreamTunnelRequest(
                        fileId,
                        size,
                        position
                ))
                .build();
        try {
            return tunnelClient.stream(tunnelRequest);
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }
}
