package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.TunnelTrafficResponseDTO;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClientGateway;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ApiConnectionsFacade {

    private final TunnelClientGateway tunnelClientGateway;

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    public List<TunnelTrafficResponseDTO> getTraffic() {
        return tunnelTrafficMonitor.getTraffic().stream()
                .map(it -> toResponseDTO(it))
                .sorted(Comparator.comparing(TunnelTrafficResponseDTO::fingerprint))
                .toList();
    }

    private TunnelTrafficResponseDTO toResponseDTO(TunnelTrafficMonitor.PeerTraffic traffic) {
        return new TunnelTrafficResponseDTO(
                traffic.fingerprint(),
                CommonUtils.toDisplaySize(traffic.downloadSpeed()),
                CommonUtils.toDisplaySize(traffic.uploadSpeed()),
                CommonUtils.toDisplaySize(traffic.totalDownloaded()),
                CommonUtils.toDisplaySize(traffic.totalUploaded())
        );
    }

    public void tunnelPing(@Nullable CriteriaEnvelope fingerprintCriteria) {
        String fingerprint = fingerprintCriteria != null
                ? handshakeTrustedOutStore.getRequiredByCriteriaKey(fingerprintCriteria).getKey()
                : handshakeTrustedOutStore.getRequiredLastUpdated().getKey();
        tunnelClientGateway.ping(fingerprint);
    }
}
