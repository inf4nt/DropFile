package com.evolution.dropfiledaemon.configuration;

import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class DaemonApplicationProperties {

    public final Path userDir;

    public final int serverPort;

    @Nullable
    public final String daemonExternalHost;

    public final Path daemonConfigDirectory;

    public final Path daemonSecretsDirectory;

    public final Path daemonInstallationSeedDirectory;

    public final Path daemonDownloadsDirectory;

    public final int daemonPort;

    public final int daemonDownloadOrchestratorMaxQueueSize;

    public final int daemonDownloadOrchestratorActiveQueueSize;

    public final int daemonDownloadProcedureThreadSize;

    public final boolean daemonTunnelClientCompressEnabled;

    public final int daemonTunnelClientStreamMaxSize;

    public final int daemonTunnelClientStreamDeadlineTimeoutMillis;

    public final int daemonTunnelClientHttpRequestTimeoutMillis;

    public final int daemonTunnelServerCompressLevel;

    public final int daemonTunnelServerPayloadLifeTime;

    public final int daemonManifestChunkMaxSize;

    public final int daemonHandshakeClientHttpRequestTimeoutMillis;

    public final int daemonHandshakeServerPayloadLiveTime;

    public final int daemonQuickShareSecureCompressLevel;

    public DaemonApplicationProperties(
            @Value("${user.dir}") Path userDir,
            @Value("${server.port}") int serverPort,
            @Value("${dropfile.daemon.external-host:#{null}}") String daemonExternalHost,
            @Value("${dropfile.daemon.config.directory}") Path daemonConfigDirectory,
            @Value("${dropfile.daemon.daemon-secrets.directory}") Path daemonSecretsDirectory,
            @Value("${dropfile.daemon.installation-seed.directory}") Path daemonInstallationSeedDirectory,
            @Value("${dropfile.daemon.downloads.directory}") Path daemonDownloadsDirectory,
            @Value("${dropfile.daemon.port}") int daemonPort,
            @Value("${dropfile.daemon.download.orchestrator.max-queue-size}") int daemonDownloadOrchestratorMaxQueueSize,
            @Value("${dropfile.daemon.download.orchestrator.active-queue-size}") int daemonDownloadOrchestratorActiveQueueSize,
            @Value("${dropfile.daemon.download.procedure.thread-size}") int daemonDownloadProcedureThreadSize,
            @Value("${dropfile.daemon.handshake.client.http.request-timeout-millis}") int daemonHandshakeClientHttpRequestTimeoutMillis,
            @Value("${dropfile.daemon.handshake.server.payload.life-time}") int daemonHandshakeServerPayloadLiveTime,
            @Value("${dropfile.daemon.tunnel.client.compress.enabled}") boolean daemonTunnelClientCompressEnabled,
            @Value("${dropfile.daemon.tunnel.client.stream.max-size}") int daemonTunnelClientStreamMaxSize,
            @Value("${dropfile.daemon.tunnel.client.stream.deadline-timeout-millis}") int daemonTunnelClientStreamDeadlineTimeoutMillis,
            @Value("${dropfile.daemon.tunnel.client.http.request-timeout-millis}") int daemonTunnelClientHttpRequestTimeoutMillis,
            @Value("${dropfile.daemon.tunnel.server.compress.level}") int daemonTunnelServerCompressLevel,
            @Value("${dropfile.daemon.tunnel.server.payload.life-time}") int daemonTunnelServerPayloadLifeTime,
            @Value("${dropfile.daemon.manifest.chunk-max-size}") int daemonManifestChunkMaxSize,
            @Value("${dropfile.daemon.quick-share.secure.compress.level}") int daemonQuickShareSecureCompressLevel) {
        this.userDir = userDir;
        this.serverPort = serverPort;
        this.daemonExternalHost = daemonExternalHost;
        this.daemonConfigDirectory = daemonConfigDirectory;
        this.daemonSecretsDirectory = daemonSecretsDirectory;
        this.daemonInstallationSeedDirectory = daemonInstallationSeedDirectory;
        this.daemonDownloadsDirectory = daemonDownloadsDirectory;
        this.daemonTunnelClientHttpRequestTimeoutMillis = daemonTunnelClientHttpRequestTimeoutMillis;
        this.daemonTunnelClientStreamDeadlineTimeoutMillis = daemonTunnelClientStreamDeadlineTimeoutMillis;
        this.daemonTunnelServerPayloadLifeTime = daemonTunnelServerPayloadLifeTime;
        this.daemonPort = daemonPort;
        this.daemonDownloadOrchestratorMaxQueueSize = daemonDownloadOrchestratorMaxQueueSize;
        this.daemonDownloadOrchestratorActiveQueueSize = daemonDownloadOrchestratorActiveQueueSize;
        this.daemonDownloadProcedureThreadSize = daemonDownloadProcedureThreadSize;
        this.daemonHandshakeClientHttpRequestTimeoutMillis = daemonHandshakeClientHttpRequestTimeoutMillis;
        this.daemonHandshakeServerPayloadLiveTime = daemonHandshakeServerPayloadLiveTime;
        this.daemonTunnelClientCompressEnabled = daemonTunnelClientCompressEnabled;
        this.daemonTunnelServerCompressLevel = daemonTunnelServerCompressLevel;
        this.daemonManifestChunkMaxSize = daemonManifestChunkMaxSize;
        this.daemonTunnelClientStreamMaxSize = daemonTunnelClientStreamMaxSize;
        this.daemonQuickShareSecureCompressLevel = daemonQuickShareSecureCompressLevel;
    }
}
