package com.evolution.dropfiledaemon.download.procedure;

import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfiledaemon.download.procedure.manifest.FileManifest;
import com.evolution.dropfiledaemon.service.ConcurrentTaskService;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClientGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@RequiredArgsConstructor
@Component
public class DownloadProcedureFactory {

    private final TunnelClientGateway tunnelClientGateway;

    private final FileHelper fileHelper;

    private final ConcurrentTaskService concurrentTaskService;

    public SingleRunDownloadProcedure get(String operation,
                                          String fingerprint,
                                          String fileId,
                                          String filename,
                                          FileManifest fileManifest,
                                          Path destinationFilePath,
                                          Path temporaryFilePath) {
        return new SingleRunDownloadProcedure(
                concurrentTaskService,
                tunnelClientGateway,
                fileHelper,
                new DownloadProcedureRequest(
                        operation,
                        fingerprint,
                        fileId,
                        filename,
                        fileManifest,
                        destinationFilePath,
                        temporaryFilePath
                )
        );
    }
}
