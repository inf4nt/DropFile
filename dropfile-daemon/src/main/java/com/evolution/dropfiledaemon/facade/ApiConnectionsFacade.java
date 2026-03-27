package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.TunnelTrafficResponseDTO;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ApiConnectionsFacade {

    private final ApplicationConfigStore applicationConfigStore;

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    public synchronized void revoke(String fingerprint) {
        String key = applicationConfigStore.getHandshakeTrustedInStore().getRequiredByKeyStartWith(fingerprint)
                .getKey();
        applicationConfigStore.getHandshakeSessionInStore().remove(key);
        applicationConfigStore.getHandshakeTrustedInStore().remove(key);
    }

    public synchronized void revokeAll() {
        applicationConfigStore.getHandshakeSessionInStore().removeAll();
        applicationConfigStore.getHandshakeTrustedInStore().removeAll();
    }

    public synchronized void disconnect(String fingerprint) {
        String key = applicationConfigStore.getHandshakeTrustedOutStore().getRequiredByKeyStartWith(fingerprint).getKey();
        applicationConfigStore.getHandshakeSessionOutStore().remove(key);
        applicationConfigStore.getHandshakeTrustedOutStore().remove(key);
    }

    public synchronized void disconnectCurrent() {
        String key = applicationConfigStore.getHandshakeSessionOutStore().getRequiredLatestUpdated().getKey();
        applicationConfigStore.getHandshakeSessionOutStore().remove(key);
        applicationConfigStore.getHandshakeTrustedOutStore().remove(key);
    }

    public synchronized void disconnectAll() {
        applicationConfigStore.getHandshakeSessionOutStore().removeAll();
        applicationConfigStore.getHandshakeTrustedOutStore().removeAll();
    }

    public TunnelTrafficResponseDTO getTraffic() {
        TunnelTrafficMonitor.Traffic traffic = tunnelTrafficMonitor.getTraffic();
        return new TunnelTrafficResponseDTO(
                traffic.download(),
                traffic.upload()
        );
    }
}
