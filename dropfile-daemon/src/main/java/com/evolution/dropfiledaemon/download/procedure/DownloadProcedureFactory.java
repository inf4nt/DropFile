package com.evolution.dropfiledaemon.download.procedure;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.manifest.FileManifestBuilder;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClientGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;

@RequiredArgsConstructor
@Component
public class DownloadProcedureFactory {

    private final ObjectMapper objectMapper;

    private final TunnelClientGateway tunnelClientGateway;

    private final FileHelper fileHelper;

    private final FileManifestBuilder fileManifestBuilder;

    private final DaemonApplicationProperties daemonApplicationProperties;

    public DownloadProcedure get(String operation,
                                 String fingerprint,
                                 String fileId,
                                 String filename,
                                 Path destinationFilePath,
                                 Path temporaryFilePath,
                                 Path manifestFilePath) {
        int downloadProcedureThreadSize = daemonApplicationProperties.daemonDownloadProcedureThreadSize;
        int manifestChunkMaxSize = daemonApplicationProperties.daemonManifestChunkMaxSize;

        return new DownloadProcedure(
                objectMapper,
                tunnelClientGateway,
                fileHelper,
                fileManifestBuilder,
                new DownloadProcedureConfiguration(
                        downloadProcedureThreadSize,
                        manifestChunkMaxSize
                ),
                new DownloadProcedureRequest(
                        operation,
                        fingerprint,
                        fileId,
                        filename,
                        destinationFilePath,
                        temporaryFilePath,
                        manifestFilePath
                )
        );
    }
}
