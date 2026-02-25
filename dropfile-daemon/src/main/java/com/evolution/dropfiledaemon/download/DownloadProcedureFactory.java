package com.evolution.dropfiledaemon.download;

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

    public DownloadProcedure get(String operation,
                                 String fingerprint,
                                 String fileId,
                                 String filename,
                                 File destinationFile,
                                 File temporaryFile) {
        return new DownloadProcedure(
                tunnelClient, fileHelper, operation, fingerprint, fileId, filename, destinationFile, temporaryFile
        );
    }
}
