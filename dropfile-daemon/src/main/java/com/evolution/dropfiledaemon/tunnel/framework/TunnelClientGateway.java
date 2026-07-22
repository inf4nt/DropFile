package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfiledaemon.facade.ApiHandshakeFacade;
import com.evolution.dropfiledaemon.manifest.FileManifest;
import com.evolution.dropfiledaemon.tunnel.command.ShareDownloadChunkStreamCommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.ShareDownloadManifestCommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.ShareLsCommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareDownloadChunkStreamTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareDownloadManifestCommandRequest;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareLsTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareLsTunnelResponse;
import com.evolution.dropfiledaemon.tunnel.framework.client.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.framework.client.exception.TunnelClientSessionExpiredException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@RequiredArgsConstructor
@Component
public class TunnelClientGateway {

    private final ObjectMapper objectMapper;

    private final TunnelClient tunnelClient;

    private final ApiHandshakeFacade apiHandshakeFacade;

    public List<ShareLsTunnelResponse> shareLs(String fingerprint, List<String> ids) {
        return callTunnelMightRefreshSessions(fingerprint, () -> {
            TunnelClient.Request request = TunnelClient.Request.builder(ShareLsCommandHandler.COMMAND_NAME, fingerprint)
                    .body(new ShareLsTunnelRequest(ids))
                    .build();
            try (InputStream stream = tunnelClient.stream(request)) {
                return objectMapper.readValue(stream, new TypeReference<List<ShareLsTunnelResponse>>() {
                });
            }
        });
    }

    public FileManifest shareDownloadManifest(String fingerprint, String fileId, int chunkSize) {
        return callTunnelMightRefreshSessions(fingerprint, () -> {
            TunnelClient.Request request = TunnelClient.Request.builder(ShareDownloadManifestCommandHandler.COMMAND_NAME, fingerprint)
                    .body(new ShareDownloadManifestCommandRequest(
                            fileId,
                            chunkSize
                    ))
                    .build();
            try (InputStream stream = tunnelClient.stream(request)) {
                return objectMapper.readValue(stream, FileManifest.class);
            }
        });
    }

    public InputStream shareDownloadChunkStream(String fingerprint, String fileId, int size, long position) {
        return callTunnelMightRefreshSessions(fingerprint, () -> {
            TunnelClient.Request tunnelRequest = TunnelClient.Request.builder(
                            ShareDownloadChunkStreamCommandHandler.COMMAND_NAME, fingerprint
                    )
                    .body(new ShareDownloadChunkStreamTunnelRequest(
                            fileId,
                            size,
                            position
                    ))
                    .build();
            return tunnelClient.stream(tunnelRequest);
        });
    }

    @SneakyThrows
    private <T> T callTunnelMightRefreshSessions(String fingerprint, Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            TunnelClientSessionExpiredException expiredException = CommonUtils.getThrowable(e, TunnelClientSessionExpiredException.class);
            if (expiredException != null) {
                log.info("Session has been expired fingerprint {}. Refreshing", fingerprint);
                apiHandshakeFacade.systemHandshakeSessionRefresh(fingerprint, expiredException.getTimestamp());
                return callable.call();
            }
            throw e;
        }
    }
}
