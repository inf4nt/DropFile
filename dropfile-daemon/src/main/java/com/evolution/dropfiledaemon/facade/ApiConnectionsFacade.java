package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.TunnelTrafficResponseDTO;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClientGateway;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
public class ApiConnectionsFacade {

    private final TunnelClientGateway tunnelClientGateway;

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

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

    public void tunnelPing(@Nullable CriteriaEnvelope fingerprintCriteria) {
        String fingerprint = fingerprintCriteria != null
                ? handshakeTrustedOutStore.getRequiredByCriteria(fingerprintCriteria).getKey()
                : handshakeTrustedOutStore.getRequiredLastUpdated().getKey();
        tunnelClientGateway.ping(fingerprint);
    }
}
