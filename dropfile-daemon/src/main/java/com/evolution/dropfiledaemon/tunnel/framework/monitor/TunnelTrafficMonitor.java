package com.evolution.dropfiledaemon.tunnel.framework.monitor;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.util.DownloadSpeedMeter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class TunnelTrafficMonitor {

    private final ApplicationConfigStore applicationConfigStore;

    private final Map<String, DownloadSpeedMeter> inputStreams = new ConcurrentHashMap<>();

    private final Map<String, DownloadSpeedMeter> outputStreams = new ConcurrentHashMap<>();

    public Traffic getTraffic() {
        cleanup();
        return new Traffic(
                getTraffic(inputStreams),
                getTraffic(outputStreams)
        );
    }

    public OutputStream outputStreamWrapper(String fingerprint, OutputStream outputStream) {
        DownloadSpeedMeter downloadSpeedMeter = outputStreams.computeIfAbsent(fingerprint, value -> new DownloadSpeedMeter());
        return new MonitoringOutputStream(outputStream, downloadSpeedMeter);
    }

    public InputStream inputStreamWrapper(String fingerprint, InputStream inputStream) {
        DownloadSpeedMeter downloadSpeedMeter = inputStreams.computeIfAbsent(fingerprint, value -> new DownloadSpeedMeter());
        return new MonitoringInputStream(inputStream, downloadSpeedMeter);
    }

    private Map<String, String> getTraffic(Map<String, DownloadSpeedMeter> traffic) {
        cleanup();
        Map<String, String> map = traffic.entrySet().stream()
                .map(entry -> {
                    String fingerprint = entry.getKey();
                    long speedBytesPerSec = entry.getValue().getSpeedBytesPerSec();
                    String displaySize = CommonUtils.toDisplaySize(speedBytesPerSec);
                    return new AbstractMap.SimpleEntry<>(fingerprint, displaySize);
                })
                .collect(Collectors.toMap(
                        x -> x.getKey(),
                        x -> x.getValue(),
                        (s, s2) -> s2,
                        () -> new TreeMap<>(String::compareTo))
                );
        return Collections.unmodifiableMap(map);
    }

    private void cleanup() {
        new HashSet<>(outputStreams.keySet()).forEach(fingerprint -> {
            boolean empty = applicationConfigStore.getHandshakeTrustedInStore().get(fingerprint).isEmpty();
            if (empty) {
                outputStreams.remove(fingerprint);
            }
        });
        new HashSet<>(inputStreams.keySet()).forEach(fingerprint -> {
            boolean empty = applicationConfigStore.getHandshakeSessionOutStore().get(fingerprint).isEmpty();
            if (empty) {
                inputStreams.remove(fingerprint);
            }
        });
    }

    public record Traffic(Map<String, String> download,
                          Map<String, String> upload) {

    }
}
