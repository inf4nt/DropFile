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

    public final int downloadOrchestratorMaxQueueSize;

    public final int downloadOrchestratorActiveQueueSize;

    public final int downloadProcedureManifestCallTimeoutMillis;

    public final int downloadProcedureChunkCallTimeoutMillis;

    public final int downloadProcedureThreadSize;

    public final boolean tunnelCompressEnabled;

    public final int tunnelCompressLevel;

    public final int manifestBuildChunkSize;

    public final int manifestBuildBufferSize;

    public DaemonApplicationProperties(@Value("${user.dir}") String applicationDirectory,
                                       @Value("${dropfile.download.directory}") String downloadDirectory,
                                       @Value("${dropfile.daemon.port}") int daemonPort,
                                       @Value("${dropfile.download.orchestrator.max-queue-size}") int downloadOrchestratorMaxQueueSize,
                                       @Value("${dropfile.download.orchestrator.active-queue-size}") int downloadOrchestratorActiveQueueSize,
                                       @Value("${dropfile.download.procedure.manifest.call.timeout-millis}") int downloadProcedureManifestCallTimeoutMillis,
                                       @Value("${dropfile.download.procedure.chunk.call.timeout-millis}") int downloadProcedureChunkCallTimeoutMillis,
                                       @Value("${dropfile.download.procedure.thread-size}") int downloadProcedureThreadSize,
                                       @Value("${dropfile.file.operations.buffer-size}") int fileOperationsBufferSize,
                                       @Value("${dropfile.tunnel.compress.enabled}") boolean tunnelCompressEnabled,
                                       @Value("${dropfile.tunnel.compress.level}") int tunnelCompressLevel,
                                       @Value("${dropfile.manifest-builder.chunk-size}") int manifestBuildChunkSize,
                                       @Value("${dropfile.manifest-builder.buffer-size}") int manifestBuildBufferSize) {
        this.fileOperationsBufferSize = fileOperationsBufferSize;
        this.manifestBuildChunkSize = manifestBuildChunkSize;
        this.manifestBuildBufferSize = manifestBuildBufferSize;
        this.downloadDirectory = getDownloadDirectory(downloadDirectory);
        this.configDirectory = getConfigDirectory(applicationDirectory);
        this.daemonPort = daemonPort;
        this.downloadOrchestratorMaxQueueSize = downloadOrchestratorMaxQueueSize;
        this.downloadOrchestratorActiveQueueSize = downloadOrchestratorActiveQueueSize;
        this.downloadProcedureThreadSize = downloadProcedureThreadSize;
        this.downloadProcedureManifestCallTimeoutMillis = downloadProcedureManifestCallTimeoutMillis;
        this.downloadProcedureChunkCallTimeoutMillis = downloadProcedureChunkCallTimeoutMillis;
        this.tunnelCompressEnabled = tunnelCompressEnabled;
        this.tunnelCompressLevel = tunnelCompressLevel;
    }

    @SneakyThrows
    private String getDownloadDirectory(String downloadDirectory) {
        if (Files.notExists(Paths.get(downloadDirectory))) {
            throw new FileNotFoundException(String.format("No %s found", downloadDirectory));
        }
        return downloadDirectory;
    }

    @SneakyThrows
    private String getConfigDirectory(String applicationDirectory) {
        return Paths.get(applicationDirectory, "conf").toString();
    }
}
