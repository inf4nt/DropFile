package com.evolution.dropfiledaemon.download;

import com.evolution.dropfiledaemon.manifest.FileManifestService;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.util.FileHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DownloadProcedureFactory {

    private final TunnelClient tunnelClient;

    private final FileHelper fileHelper;

    private final FileManifestService fileManifestService;

    private final int maxThreadSize;

    @Autowired
    public DownloadProcedureFactory(TunnelClient tunnelClient,
                                    FileHelper fileHelper,
                                    FileManifestService fileManifestService,
                                    @Value("${download.procedure.thread-size}") int maxThreadSize) {
        this.tunnelClient = tunnelClient;
        this.fileHelper = fileHelper;
        this.fileManifestService = fileManifestService;
        this.maxThreadSize = maxThreadSize;
    }

    public DownloadProcedure get(String operation,
                                 String fingerprint,
                                 String fileId,
                                 String filename,
                                 File destinationFile,
                                 File temporaryFile) {
        return new DownloadProcedure(
                tunnelClient, fileHelper, fileManifestService, maxThreadSize,
                operation, fingerprint, fileId, filename, destinationFile, temporaryFile
        );
    }
}
