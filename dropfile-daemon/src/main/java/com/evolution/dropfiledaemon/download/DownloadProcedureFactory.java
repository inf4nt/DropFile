package com.evolution.dropfiledaemon.download;

import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.manifest.FileManifestService;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;

@RequiredArgsConstructor
@Component
public class DownloadProcedureFactory {

    private final TunnelClient tunnelClient;

    private final FileHelper fileHelper;

    private final FileManifestService fileManifestService;

    private final ApplicationConfigStore applicationConfigStore;

    public DownloadProcedure get(String operation,
                                 String fingerprint,
                                 String fileId,
                                 String filename,
                                 File destinationFile,
                                 File temporaryFile) {
        Integer downloadProcedureThreadSize = applicationConfigStore.getAppConfigStore().getRequired()
                .daemonAppConfig().downloadProcedureThreadSize();
        return new DownloadProcedure(
                tunnelClient, fileHelper, fileManifestService, downloadProcedureThreadSize,
                operation, fingerprint, fileId, filename, destinationFile, temporaryFile
        );
    }
}
