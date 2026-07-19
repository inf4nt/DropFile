package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.TunnelTrafficResponseDTO;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
public class ApiConnectionsFacade {

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final HandshakeSessionInStore handshakeSessionInStore;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    private final HandshakeSessionOutStore handshakeSessionOutStore;

    public synchronized void revoke(String fingerprint) {
        String key = handshakeTrustedInStore.getRequiredByKeyStartWith(fingerprint)
                .getKey();
        handshakeSessionInStore.remove(key);
        handshakeTrustedInStore.remove(key);
    }

    public synchronized void revokeAll() {
        handshakeSessionInStore.removeAll();
        handshakeTrustedInStore.removeAll();
    }

    public synchronized void disconnect(String fingerprint) {
        String key = handshakeTrustedOutStore.getRequiredByKeyStartWith(fingerprint).getKey();
        handshakeSessionOutStore.remove(key);
        handshakeTrustedOutStore.remove(key);
    }

    public synchronized void disconnectCurrent() {
        String key = handshakeSessionOutStore.getRequiredLatestCreated().getKey();
        handshakeSessionOutStore.remove(key);
        handshakeTrustedOutStore.remove(key);
    }

    public synchronized void disconnectAll() {
        handshakeSessionOutStore.removeAll();
        handshakeTrustedOutStore.removeAll();
    }

    public List<TunnelTrafficResponseDTO> getTraffic() {
        TunnelTrafficMonitor.Traffic traffic = tunnelTrafficMonitor.getTraffic();
        return Stream.concat(traffic.download().keySet().stream(), traffic.upload().keySet().stream())
                .map(fingerprint -> {
                    String download = traffic.download().get(fingerprint);
                    String upload = traffic.upload().get(fingerprint);
                    String totalDownload = traffic.totalDownload().get(fingerprint);
                    String totalUpload = traffic.totalUpload().get(fingerprint);
                    return new TunnelTrafficResponseDTO(fingerprint, download, upload, totalDownload, totalUpload);
                })
                .toList();
    }
}
