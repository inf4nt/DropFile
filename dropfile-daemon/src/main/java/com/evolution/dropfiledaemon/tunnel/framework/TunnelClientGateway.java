package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfiledaemon.manifest.FileManifest;
import com.evolution.dropfiledaemon.tunnel.command.ShareDownloadChunkStreamCommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.ShareDownloadManifestCommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.ShareLsCommandHandler;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareDownloadChunkStreamTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareDownloadManifestCommandRequest;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareLsTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.command.dto.ShareLsTunnelResponse;
import com.evolution.dropfiledaemon.tunnel.framework.client.TunnelRehandshakeClientDecorator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class TunnelClientGateway {

    private final ObjectMapper objectMapper;

    private final TunnelRehandshakeClientDecorator tunnelClient;

    @SneakyThrows
    public List<ShareLsTunnelResponse> shareLs(String fingerprint, List<String> ids) {
        TunnelClient.Request request = TunnelClient.Request.builder(ShareLsCommandHandler.COMMAND_NAME, fingerprint)
                .body(new ShareLsTunnelRequest(ids))
                .build();
        try (InputStream stream = tunnelClient.stream(request)) {
            return objectMapper.readValue(stream, new TypeReference<List<ShareLsTunnelResponse>>() {
            });
        }
    }

    @SneakyThrows
    public FileManifest shareDownloadManifest(String fingerprint, String fileId, int chunkSize) {
        TunnelClient.Request request = TunnelClient.Request.builder(ShareDownloadManifestCommandHandler.COMMAND_NAME, fingerprint)
                .body(new ShareDownloadManifestCommandRequest(
                        fileId,
                        chunkSize
                ))
                .build();
        try (InputStream stream = tunnelClient.stream(request)) {
            return objectMapper.readValue(stream, FileManifest.class);
        }
    }

    @SneakyThrows
    public InputStream shareDownloadChunkStream(String fingerprint, String fileId, int size, long position) {
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
    }
}
