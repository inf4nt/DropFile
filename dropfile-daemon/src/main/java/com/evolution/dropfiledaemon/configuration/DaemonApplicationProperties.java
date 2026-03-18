package com.evolution.dropfiledaemon.configuration;

import jakarta.annotation.Nullable;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class DaemonApplicationProperties {

    public final String downloadDirectory;

    public final String configDirectory;

    public final int daemonPort;

    public final int fileOperationsBufferSize;

    public final int downloadProcedureThreadSize;

    public final int downloadProcedureManifestCallTimeoutMillis;

    public final int downloadProcedureChunkCallTimeoutMillis;

    public final boolean tunnelCompressEnabled;

    public final int tunnelCompressLevel;

    public final int manifestBuildChunkSize;

    public final int manifestBuildBufferSize;

    public DaemonApplicationProperties(@Value("${user.dir}") String applicationDirectory,
                                       @Value("${dropfile.download.directory}") String downloadDirectory,
                                       @Value("${dropfile.daemon.port}") int daemonPort,
                                       @Value("${dropfile.file.operations.buffer-size}") int fileOperationsBufferSize,
                                       @Value("${dropfile.download.procedure.thread-size}") int downloadProcedureThreadSize,
                                       @Value("${dropfile.download.procedure.manifest.call.timeout-millis}") int downloadProcedureManifestCallTimeoutMillis,
                                       @Value("${dropfile.download.procedure.chunk.call.timeout-millis}") int downloadProcedureChunkCallTimeoutMillis,
                                       @Value("${dropfile.tunnel.compress.enabled}") boolean tunnelCompressEnabled,
                                       @Value("${dropfile.tunnel.compress.level}") int tunnelCompressLevel,
                                       @Value("${dropfile.manifest-builder.chunk-size}") int manifestBuildChunkSize,
                                       @Value("${dropfile.manifest-builder.buffer-size}") int manifestBuildBufferSize) {
        this.fileOperationsBufferSize = fileOperationsBufferSize;
        this.manifestBuildChunkSize = manifestBuildChunkSize;
        this.manifestBuildBufferSize = manifestBuildBufferSize;
        this.downloadDirectory = getDownloadDirectory(applicationDirectory, downloadDirectory);
        this.configDirectory = getConfigDirectory(applicationDirectory);
        this.daemonPort = daemonPort;
        this.downloadProcedureThreadSize = downloadProcedureThreadSize;
        this.downloadProcedureManifestCallTimeoutMillis = downloadProcedureManifestCallTimeoutMillis;
        this.downloadProcedureChunkCallTimeoutMillis = downloadProcedureChunkCallTimeoutMillis;
        this.tunnelCompressEnabled = tunnelCompressEnabled;
        this.tunnelCompressLevel = tunnelCompressLevel;
    }

    @SneakyThrows
    private String getDownloadDirectory(String applicationDirectory, @Nullable String downloadDirectory) {
        String directory;
        if (!ObjectUtils.isEmpty(downloadDirectory)) {
            directory = Paths.get(downloadDirectory).toString();
        } else {
            directory = Paths.get(applicationDirectory, "downloads").toString();

        }
        if (Files.notExists(Paths.get(directory))) {
            throw new FileNotFoundException(String.format("No %s found", directory));
        }
        return directory;
    }

    @SneakyThrows
    private String getConfigDirectory(String applicationDirectory) {
        return Paths.get(applicationDirectory, "conf").toString();
    }
}
