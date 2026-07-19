package com.evolution.dropfiledaemon.tunnel.framework.monitor;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.util.ThroughputMeter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class TunnelTrafficMonitor {

    private final Map<String, ThroughputMeter> inputStreams = new ConcurrentHashMap<>();

    private final Map<String, ThroughputMeter> outputStreams = new ConcurrentHashMap<>();

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final HandshakeSessionOutStore handshakeSessionOutStore;

    public Traffic getTraffic() {
        cleanup();
        return new Traffic(
                getTraffic(inputStreams),
                getTraffic(outputStreams),
                getTotalTraffic(inputStreams),
                getTotalTraffic(outputStreams)
        );
    }

    public OutputStream outputStreamWrapper(String fingerprint, OutputStream outputStream) {
        ThroughputMeter throughputMeter = outputStreams.computeIfAbsent(fingerprint, value -> new ThroughputMeter());
        return new MonitoringOutputStream(outputStream, throughputMeter);
    }

    public InputStream inputStreamWrapper(String fingerprint, InputStream inputStream) {
        ThroughputMeter throughputMeter = inputStreams.computeIfAbsent(fingerprint, value -> new ThroughputMeter());
        return new MonitoringInputStream(inputStream, throughputMeter);
    }

    private Map<String, String> getTraffic(Map<String, ThroughputMeter> traffic) {
        Map<String, String> map = traffic.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> CommonUtils.toDisplaySize(entry.getValue().getSpeedBytesPerSec()),
                        (existing, __) -> existing,
                        TreeMap::new
                ));
        return Collections.unmodifiableMap(map);
    }

    private Map<String, String> getTotalTraffic(Map<String, ThroughputMeter> traffic) {
        Map<String, String> map = traffic.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> CommonUtils.toDisplaySize(entry.getValue().getTotalThroughput()),
                        (existing, __) -> existing,
                        TreeMap::new
                ));
        return Collections.unmodifiableMap(map);
    }

    private void cleanup() {
        outputStreams.keySet().removeIf(fingerprint ->
                handshakeTrustedInStore.get(fingerprint).isEmpty()
        );

        inputStreams.keySet().removeIf(fingerprint ->
                handshakeSessionOutStore.get(fingerprint).isEmpty()
        );
    }

    public record Traffic(Map<String, String> download,
                          Map<String, String> upload,
                          Map<String, String> totalDownload,
                          Map<String, String> totalUpload) {
    }
}
