package com.evolution.dropfiledaemon.download.procedure;

import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.download.procedure.manifest.FileManifest;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClientGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@RequiredArgsConstructor
@Component
public class DownloadProcedureFactory {

    private final TunnelClientGateway tunnelClientGateway;

    private final FileHelper fileHelper;

    private final DaemonApplicationProperties daemonApplicationProperties;

    public SingleRunDownloadProcedure get(String operation,
                                          String fingerprint,
                                          String fileId,
                                          String filename,
                                          FileManifest fileManifest,
                                          Path destinationFilePath,
                                          Path temporaryFilePath,
                                          Path manifestFilePath) {
        int downloadProcedureThreadSize = daemonApplicationProperties.daemonDownloadProcedureThreadSize;
        int manifestChunkMaxSize = daemonApplicationProperties.daemonManifestChunkSize;

        return new SingleRunDownloadProcedure(
                tunnelClientGateway,
                fileHelper,
                new DownloadProcedureConfiguration(
                        downloadProcedureThreadSize,
                        manifestChunkMaxSize
                ),
                new DownloadProcedureRequest(
                        operation,
                        fingerprint,
                        fileId,
                        filename,
                        fileManifest,
                        destinationFilePath,
                        temporaryFilePath,
                        manifestFilePath
                )
        );
    }
}
