package com.evolution.dropfiledaemon.tunnel.framework.client.handler;

import com.evolution.dropfile.common.WatchdogInputStream;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelServerDispatcherStatus;
import com.evolution.dropfiledaemon.tunnel.framework.server.compress.CompressTunnelService;
import com.evolution.dropfiledaemon.tunnel.framework.client.exception.TunnelClientException;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@RequiredArgsConstructor
@Component
public class OkTunnelClientHandler implements TunnelClientHandler {

    private final CryptoTunnel cryptoTunnel;

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final CompressTunnelService compressTunnelService;

    @Override
    public int getStatusCode() {
        return TunnelServerDispatcherStatus.OK.getStatusCode();
    }

    @Override
    public InputStream handle(String fingerprint,
                              HandshakeTrustedOutStore.TrustedOut trustedOut,
                              SecretKey secretKey,
                              InputStream inputStream) throws TunnelClientException, IOException {
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(
                inputStream,
                daemonApplicationProperties.daemonTunnelClientStreamMaxSize,
                Duration.ofMillis(daemonApplicationProperties.daemonTunnelClientStreamDeadlineTimeoutMillis)
        );
        InputStream tunnelTrafficMonitorInputStream = tunnelTrafficMonitor.inputStreamWrapper(fingerprint, watchdogInputStream);
        InputStream decrypt = cryptoTunnel.decrypt(
                tunnelTrafficMonitorInputStream,
                secretKey
        );
        if (daemonApplicationProperties.daemonTunnelClientCompressEnabled) {
            return compressTunnelService.decompress(decrypt);
        }
        return decrypt;
    }
}
