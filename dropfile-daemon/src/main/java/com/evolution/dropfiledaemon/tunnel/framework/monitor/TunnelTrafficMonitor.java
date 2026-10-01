package com.evolution.dropfiledaemon.tunnel.framework.monitor;

import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfile.common.io.MonitoringInputStream;
import com.evolution.dropfile.common.io.MonitoringOutputStream;
import com.evolution.dropfile.common.io.ThroughputMeter;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedOutStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
public class TunnelTrafficMonitor implements Purgeable {

    private final Map<String, ThroughputMeter> inputStreams = new ConcurrentHashMap<>();

    private final Map<String, ThroughputMeter> outputStreams = new ConcurrentHashMap<>();

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    public List<PeerTraffic> getTraffic() {
        return Stream.concat(inputStreams.keySet().stream(), outputStreams.keySet().stream())
                .map(this::buildPeerTraffic)
                .toList();
    }

    public OutputStream outputStreamWrapper(String fingerprint, OutputStream outputStream) {
        ThroughputMeter throughputMeter = outputStreams.computeIfAbsent(fingerprint, _ -> new ThroughputMeter());
        return new MonitoringOutputStream(outputStream, throughputMeter);
    }

    public InputStream inputStreamWrapper(String fingerprint, InputStream inputStream) {
        ThroughputMeter throughputMeter = inputStreams.computeIfAbsent(fingerprint, _ -> new ThroughputMeter());
        return new MonitoringInputStream(inputStream, throughputMeter);
    }

    private PeerTraffic buildPeerTraffic(String fingerprint) {
        ThroughputMeter inMeter = inputStreams.get(fingerprint);
        ThroughputMeter outMeter = outputStreams.get(fingerprint);

        long downloadSpeed = inMeter != null ? inMeter.getSpeedBytesPerSec() : 0;
        long totalDownloaded = inMeter != null ? inMeter.getTotalThroughput() : 0;

        long uploadSpeed = outMeter != null ? outMeter.getSpeedBytesPerSec() : 0;
        long totalUploaded = outMeter != null ? outMeter.getTotalThroughput() : 0;

        return new PeerTraffic(fingerprint, downloadSpeed, uploadSpeed, totalDownloaded, totalUploaded);
    }

    @Override
    public void purge() {
        outputStreams.keySet().removeIf(fingerprint -> handshakeTrustedInStore.get(fingerprint).isEmpty());

        inputStreams.keySet().removeIf(fingerprint -> handshakeTrustedOutStore.get(fingerprint).isEmpty());
    }

    public record PeerTraffic(
            String fingerprint,
            long downloadSpeed,
            long uploadSpeed,
            long totalDownloaded,
            long totalUploaded
    ) {
    }
}
