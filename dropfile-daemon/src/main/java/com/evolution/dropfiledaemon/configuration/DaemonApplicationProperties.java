package com.evolution.dropfiledaemon.configuration;

import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

// TODO
// @ConfigurationProperties(prefix = "dropfile.daemon")
// @Validated
// and record instead of class
// @EnableConfigurationProperties(DaemonApplicationProperties.class)


@Component
public class DaemonApplicationProperties {

    public final Path userDir;

    public final int serverPort;

    public final int serverTomcatMaxConnections;

    public final int serverTomcatAcceptCount;

    public final String springMvcAsyncRequestTimeout;

    @Nullable
    public final String daemonExternalHost;

    public final Path daemonApplicationHomeDirectory;

    public final Path daemonSecretsDirectory;

    public final Path daemonInstallationSeedDirectory;

    public final Path daemonDownloadsDirectory;

    public final int daemonDownloadOrchestratorMaxQueueSize;

    public final int daemonDownloadOrchestratorActiveQueueSize;

    public final int daemonDownloadProcedureThreadSize;

    public final boolean daemonTunnelClientCompressEnabled;

    public final int daemonTunnelClientStreamMaxSize;

    public final Duration daemonTunnelClientStreamDeadlineTimeout;

    public final Duration daemonTunnelClientHttpRequestTimeout;

    public final int daemonTunnelServerCompressLevel;

    public final int daemonTunnelServerChunkLimitMax;

    public final int daemonTunnelServerChunkLimitMin;

    public final int daemonTunnelClientManifestChunkSize;

    public final Duration daemonHandshakeClientHttpRequestTimeout;

    public final Duration daemonQuickShareSecureAsyncRequestTimeout;

    public final int daemonQuickShareSecureCompressLevel;

    public final boolean daemonQuickShareInsecureCompressEnabled;

    public final int daemonQuickShareInsecureCompressLevel;

    public final Duration daemonGcRateInterval;

    public final long daemonIdleTimeoutMillis;

    public final Duration daemonIdleRateInterval;

    public final Duration daemonShareAddHashExecutionTimeout;

    public final int daemonServerServletInputStreamTimeoutMillis;

    public final int daemonServerServletInputStreamLimitMax;

    public final int daemonServerServletOutputStreamTimeoutMillis;

    public final int daemonServerServletRateRequestHandshakeLimitMax;

    public final int daemonServerServletRateRequestTunnelLimitMax;

    public final int daemonServerServletRateRequestQuickshareLimitMax;

    public final int daemonSecurityReplyTtlSeconds;

    public final int daemonSecurityReplyMaxFutureDriftSeconds;

    public DaemonApplicationProperties(
            @Value("${user.dir}") Path userDir,
            @Value("${server.port}") int serverPort,
            @Value("${server.tomcat.max-connections}") int serverTomcatMaxConnections,
            @Value("${server.tomcat.accept-count}") int serverTomcatAcceptCount,
            @Value("${spring.mvc.async.request-timeout}") String springMvcAsyncRequestTimeout,
            @Value("${dropfile.daemon.external-host:#{null}}") String daemonExternalHost,
            @Value("${dropfile.daemon.application-home.directory}") Path daemonApplicationHomeDirectory,
            @Value("${dropfile.daemon.daemon-secrets.directory}") Path daemonSecretsDirectory,
            @Value("${dropfile.daemon.installation-seed.directory}") Path daemonInstallationSeedDirectory,
            @Value("${dropfile.daemon.downloads.directory}") Path daemonDownloadsDirectory,
            @DurationUnit(ChronoUnit.MILLIS) @Value("${dropfile.daemon.share.add.hash.execution-timeout}") Duration daemonShareAddHashExecutionTimeout,
            @Value("${dropfile.daemon.download.orchestrator.max-queue-size}") int daemonDownloadOrchestratorMaxQueueSize,
            @Value("${dropfile.daemon.download.orchestrator.active-queue-size}") int daemonDownloadOrchestratorActiveQueueSize,
            @Value("${dropfile.daemon.download.procedure.thread-size}") int daemonDownloadProcedureThreadSize,
            @DurationUnit(ChronoUnit.MILLIS) @Value("${dropfile.daemon.handshake.client.http.request-timeout}") Duration daemonHandshakeClientHttpRequestTimeout,
            @Value("${dropfile.daemon.tunnel.client.compress.enabled}") boolean daemonTunnelClientCompressEnabled,
            @Value("${dropfile.daemon.tunnel.client.stream.max-size}") int daemonTunnelClientStreamMaxSize,
            @DurationUnit(ChronoUnit.MILLIS) @Value("${dropfile.daemon.tunnel.client.stream.deadline-timeout}") Duration daemonTunnelClientStreamDeadlineTimeout,
            @DurationUnit(ChronoUnit.MILLIS) @Value("${dropfile.daemon.tunnel.client.http.request-timeout}") Duration daemonTunnelClientHttpRequestTimeout,
            @Value("${dropfile.daemon.tunnel.server.compress.level}") int daemonTunnelServerCompressLevel,
            @Value("${dropfile.daemon.tunnel.server.chunk.limit.max}") int daemonTunnelServerChunkLimitMax,
            @Value("${dropfile.daemon.tunnel.server.chunk.limit.min}") int daemonTunnelServerChunkLimitMin,
            @Value("${dropfile.daemon.tunnel.client.manifest.chunk-size}") int daemonTunnelClientManifestChunkSize,
            @DurationUnit(ChronoUnit.MILLIS) @Value("${dropfile.daemon.quickshare.async.request-timeout}") Duration daemonQuickShareSecureAsyncRequestTimeout,
            @Value("${dropfile.daemon.quickshare.secure.compress.level}") int daemonQuickShareSecureCompressLevel,
            @Value("${dropfile.daemon.quickshare.insecure.compress.enabled}") boolean daemonQuickShareInsecureCompressEnabled,
            @Value("${dropfile.daemon.quickshare.insecure.compress.level}") int daemonQuickShareInsecureCompressLevel,
            @DurationUnit(ChronoUnit.MILLIS) @Value("${dropfile.daemon.gc.rate.interval}") Duration daemonGcRateInterval,
            @Value("${dropfile.daemon.idle.timeout-millis}") long daemonIdleTimeoutMillis,
            @DurationUnit(ChronoUnit.MILLIS) @Value("${dropfile.daemon.idle.rate.interval}") Duration daemonIdleRateInterval,
            @Value("${dropfile.daemon.server.servlet.input.stream.timeout-millis}") int daemonServerServletInputStreamTimeoutMillis,
            @Value("${dropfile.daemon.server.servlet.input.stream.limit.max}") int daemonServerServletInputStreamLimitMax,
            @Value("${dropfile.daemon.server.servlet.output.stream.timeout-millis}") int daemonServerServletOutputStreamTimeoutMillis,
            @Value("${dropfile.daemon.server.servlet.rate-request.handshake.limit.max}") int daemonServerServletRateRequestHandshakeLimitMax,
            @Value("${dropfile.daemon.server.servlet.rate-request.tunnel.limit.max}") int daemonServerServletRateRequestTunnelLimitMax,
            @Value("${dropfile.daemon.server.servlet.rate-request.quickshare.limit.max}") int daemonServerServletRateRequestQuickshareLimitMax,
            @Value("${dropfile.daemon.security.replay.ttl-seconds}") int daemonSecurityReplyTtlSeconds,
            @Value("${dropfile.daemon.security.replay.max-future-drift-seconds}") int daemonSecurityReplyMaxFutureDriftSeconds) {
        this.userDir = userDir;
        this.serverPort = serverPort;
        this.serverTomcatMaxConnections = serverTomcatMaxConnections;
        this.serverTomcatAcceptCount = serverTomcatAcceptCount;
        this.springMvcAsyncRequestTimeout = springMvcAsyncRequestTimeout;
        this.daemonExternalHost = daemonExternalHost;
        this.daemonApplicationHomeDirectory = daemonApplicationHomeDirectory;
        this.daemonSecretsDirectory = daemonSecretsDirectory;
        this.daemonInstallationSeedDirectory = daemonInstallationSeedDirectory;
        this.daemonDownloadsDirectory = daemonDownloadsDirectory;
        this.daemonTunnelClientHttpRequestTimeout = daemonTunnelClientHttpRequestTimeout;
        this.daemonTunnelClientStreamDeadlineTimeout = daemonTunnelClientStreamDeadlineTimeout;
        this.daemonShareAddHashExecutionTimeout = daemonShareAddHashExecutionTimeout;
        this.daemonDownloadOrchestratorMaxQueueSize = daemonDownloadOrchestratorMaxQueueSize;
        this.daemonDownloadOrchestratorActiveQueueSize = daemonDownloadOrchestratorActiveQueueSize;
        this.daemonDownloadProcedureThreadSize = daemonDownloadProcedureThreadSize;
        this.daemonHandshakeClientHttpRequestTimeout = daemonHandshakeClientHttpRequestTimeout;
        this.daemonTunnelClientCompressEnabled = daemonTunnelClientCompressEnabled;
        this.daemonTunnelServerCompressLevel = daemonTunnelServerCompressLevel;
        this.daemonTunnelServerChunkLimitMax = daemonTunnelServerChunkLimitMax;
        this.daemonTunnelServerChunkLimitMin = daemonTunnelServerChunkLimitMin;
        this.daemonTunnelClientManifestChunkSize = validateDaemonTunnelClientManifestChunkSize(daemonTunnelClientManifestChunkSize);
        this.daemonTunnelClientStreamMaxSize = daemonTunnelClientStreamMaxSize;
        this.daemonQuickShareSecureAsyncRequestTimeout = daemonQuickShareSecureAsyncRequestTimeout;
        this.daemonQuickShareSecureCompressLevel = daemonQuickShareSecureCompressLevel;
        this.daemonQuickShareInsecureCompressEnabled = daemonQuickShareInsecureCompressEnabled;
        this.daemonQuickShareInsecureCompressLevel = daemonQuickShareInsecureCompressLevel;
        this.daemonGcRateInterval = daemonGcRateInterval;
        this.daemonIdleTimeoutMillis = daemonIdleTimeoutMillis;
        this.daemonIdleRateInterval = daemonIdleRateInterval;
        this.daemonServerServletInputStreamTimeoutMillis = daemonServerServletInputStreamTimeoutMillis;
        this.daemonServerServletInputStreamLimitMax = daemonServerServletInputStreamLimitMax;
        this.daemonServerServletOutputStreamTimeoutMillis = daemonServerServletOutputStreamTimeoutMillis;
        this.daemonServerServletRateRequestHandshakeLimitMax = daemonServerServletRateRequestHandshakeLimitMax;
        this.daemonServerServletRateRequestTunnelLimitMax = daemonServerServletRateRequestTunnelLimitMax;
        this.daemonServerServletRateRequestQuickshareLimitMax = daemonServerServletRateRequestQuickshareLimitMax;
        this.daemonSecurityReplyTtlSeconds = daemonSecurityReplyTtlSeconds;
        this.daemonSecurityReplyMaxFutureDriftSeconds = daemonSecurityReplyMaxFutureDriftSeconds;
    }

    private int validateDaemonTunnelClientManifestChunkSize(int daemonTunnelClientManifestChunkSize) {
        if (daemonTunnelClientManifestChunkSize <= 0) {
            throw new IllegalArgumentException("daemonTunnelClientManifestChunkSize '%d' must be greater than 0".formatted(
                    daemonTunnelClientManifestChunkSize
            ));
        }
        return daemonTunnelClientManifestChunkSize;
    }
}
