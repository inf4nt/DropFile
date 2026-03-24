package com.evolution.dropfiledaemon.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

public class DownloadSpeedMeter {

    private static final int WINDOW_MILLIS = 5_000;

    private final ConcurrentLinkedQueue<ChunkSample> samples = new ConcurrentLinkedQueue<>();

    private final LongAdder downloaded = new LongAdder();

    public void addChunk(long size) {
        samples.add(new ChunkSample(System.currentTimeMillis(), size));
        downloaded.add(size);
        cleanup();
    }

    public long getSpeedBytesPerSec() {
        cleanup();
        long totalBytes = samples.stream()
                .mapToLong(ChunkSample::size)
                .sum();

        return totalBytes / (WINDOW_MILLIS / 1_000);
    }

    public long getTotalDownloaded() {
        return downloaded.sum();
    }

    private void cleanup() {
        long horizon = System.currentTimeMillis() - WINDOW_MILLIS;
        while (!samples.isEmpty() && samples.peek().time < horizon) {
            samples.poll();
        }
    }

    private record ChunkSample(long time, long size) {
    }
}
