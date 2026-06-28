package com.evolution.dropfiledaemon.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class DaemonApplicationProperties {

    public final Path applicationDirectory;

    public final Path applicationDownloadDirectory;

    public final int daemonPort;

    public final int downloadOrchestratorMaxQueueSize;

    public final int downloadOrchestratorActiveQueueSize;

    public final int downloadProcedureThreadSize;

    public final boolean tunnelClientCompressEnabled;

    public final int tunnelClientStreamMaxSize;

    public final int tunnelClientStreamDeadlineTimeoutMillis;

    public final int tunnelClientHttpRequestTimeoutMillis;

    public final int tunnelServerCompressLevel;

    public final int tunnelServerPayloadLifeTime;

    public final int manifestChunkMaxSize;

    public final int handshakeClientHttpRequestTimeoutMillis;

    public final int handshakeServerPayloadLiveTime;

    public DaemonApplicationProperties(@Value("${user.dir}") Path applicationDirectory,
                                       @Value("${dropfile.download.directory}") Path applicationDownloadDirectory,
                                       @Value("${dropfile.daemon.port}") int daemonPort,
                                       @Value("${dropfile.download.orchestrator.max-queue-size}") int downloadOrchestratorMaxQueueSize,
                                       @Value("${dropfile.download.orchestrator.active-queue-size}") int downloadOrchestratorActiveQueueSize,
                                       @Value("${dropfile.download.procedure.thread-size}") int downloadProcedureThreadSize,
                                       @Value("${dropfile.handshake.client.http.request-timeout-millis}") int handshakeClientHttpRequestTimeoutMillis,
                                       @Value("${dropfile.handshake.server.payload.life-time}") int handshakeServerPayloadLiveTime,
                                       @Value("${dropfile.tunnel.client.compress.enabled}") boolean tunnelClientCompressEnabled,
                                       @Value("${dropfile.tunnel.client.stream.max-size}") int tunnelClientStreamMaxSize,
                                       @Value("${dropfile.tunnel.client.stream.deadline-timeout-millis}") int tunnelClientStreamDeadlineTimeoutMillis,
                                       @Value("${dropfile.tunnel.client.http.request-timeout-millis}") int tunnelClientHttpRequestTimeoutMillis,
                                       @Value("${dropfile.tunnel.server.compress.level}") int tunnelServerCompressLevel,
                                       @Value("${dropfile.tunnel.server.payload.life-time}") int tunnelServerPayloadLifeTime,
                                       @Value("${dropfile.manifest.chunk-max-size}") int manifestChunkMaxSize) {
        this.applicationDirectory = applicationDirectory;
        this.applicationDownloadDirectory = applicationDownloadDirectory;
        this.tunnelClientHttpRequestTimeoutMillis = tunnelClientHttpRequestTimeoutMillis;
        this.tunnelClientStreamDeadlineTimeoutMillis = tunnelClientStreamDeadlineTimeoutMillis;
        this.tunnelServerPayloadLifeTime = tunnelServerPayloadLifeTime;
        this.daemonPort = daemonPort;
        this.downloadOrchestratorMaxQueueSize = downloadOrchestratorMaxQueueSize;
        this.downloadOrchestratorActiveQueueSize = downloadOrchestratorActiveQueueSize;
        this.downloadProcedureThreadSize = downloadProcedureThreadSize;
        this.handshakeClientHttpRequestTimeoutMillis = handshakeClientHttpRequestTimeoutMillis;
        this.handshakeServerPayloadLiveTime = handshakeServerPayloadLiveTime;
        this.tunnelClientCompressEnabled = tunnelClientCompressEnabled;
        this.tunnelServerCompressLevel = tunnelServerCompressLevel;
        this.manifestChunkMaxSize = manifestChunkMaxSize;
        this.tunnelClientStreamMaxSize = tunnelClientStreamMaxSize;
    }
}
