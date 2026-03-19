package com.evolution.dropfiledaemon.download.procedure;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.manifest.FileManifestBuilder;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;

@RequiredArgsConstructor
@Component
public class DownloadProcedureFactory {

    private final TunnelClient tunnelClient;

    private final FileHelper fileHelper;

    private final FileManifestBuilder fileManifestBuilder;

    private final DaemonApplicationProperties daemonApplicationProperties;

    public DownloadProcedure get(String operation,
                                 String fingerprint,
                                 String fileId,
                                 String filename,
                                 File destinationFile,
                                 File temporaryFile) {
        int downloadProcedureThreadSize = daemonApplicationProperties.downloadProcedureThreadSize;
        int downloadProcedureManifestCallTimeoutMillis = daemonApplicationProperties.downloadProcedureManifestCallTimeoutMillis;
        int downloadProcedureChunkCallTimeoutMillis = daemonApplicationProperties.downloadProcedureChunkCallTimeoutMillis;

        return new DownloadProcedure(
                tunnelClient,
                fileHelper,
                fileManifestBuilder,
                new DownloadProcedureConfiguration(
                        downloadProcedureThreadSize,
                        downloadProcedureManifestCallTimeoutMillis,
                        downloadProcedureChunkCallTimeoutMillis
                ),
                new DownloadProcedureRequest(
                        operation,
                        fingerprint,
                        fileId,
                        filename,
                        destinationFile,
                        temporaryFile
                )
        );
    }
}
