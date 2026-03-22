package com.evolution.dropfiledaemon.configuration;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public final int manifestBuildBufferSize;

    public final int manifestChunkMaxSize;

    public final int manifestManifestMaxSize;

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
                                       @Value("${dropfile.manifest-builder.buffer-size}") int manifestBuildBufferSize,
                                       @Value("${dropfile.manifest.chunk-max-size}") int manifestChunkMaxSize,
                                       @Value("${dropfile.manifest.manifest-max-size}") int manifestManifestMaxSize) {
        this.fileOperationsBufferSize = fileOperationsBufferSize;
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
        this.manifestChunkMaxSize = manifestChunkMaxSize;
        this.manifestManifestMaxSize = manifestManifestMaxSize;
    }

    @SneakyThrows
    private String getDownloadDirectory(String downloadDirectory) {
        Path path = Paths.get(downloadDirectory);
        if (Files.notExists(path)) {
            throw new FileNotFoundException(String.format("No %s found", path));
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Must be a directory " + path);
        }
        return path.toString();
    }

    @SneakyThrows
    private String getConfigDirectory(String applicationDirectory) {
        return Paths.get(applicationDirectory, "conf").toString();
    }
}
