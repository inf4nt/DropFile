package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.HandshakeApiTrustOutResponseDTO;
import com.evolution.dropfile.common.dto.TunnelTrafficResponseDTO;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClientGateway;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
public class ApiConnectionsFacade {

    private final TunnelClientGateway tunnelClientGateway;

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    private final ApiHandshakeFacade apiHandshakeFacade;

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

    public void tunnelPing() {
        HandshakeApiTrustOutResponseDTO latestTrustOut = apiHandshakeFacade.getLatestTrustOut();
        tunnelClientGateway.ping(latestTrustOut.remoteFingerprint());
    }
}
