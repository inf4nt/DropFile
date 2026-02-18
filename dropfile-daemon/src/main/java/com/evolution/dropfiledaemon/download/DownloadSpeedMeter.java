package com.evolution.dropfiledaemon.download;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

public class DownloadSpeedMeter {

    private final int windowMs = 5000;

    private final ConcurrentLinkedQueue<ChunkSample> samples = new ConcurrentLinkedQueue<>();

    private final LongAdder downloaded = new LongAdder();

    public void addChunk(long bytes) {
        samples.add(new ChunkSample(System.currentTimeMillis(), bytes));
        downloaded.add(bytes);
        cleanup();
    }

    public long getSpeedBytesPerSec() {
        cleanup();
        long totalBytes = samples.stream()
                .mapToLong(ChunkSample::size)
                .sum();

        return totalBytes / (windowMs / 1000);
    }

    public long getTotalDownloaded() {
        return downloaded.sum();
    }

    private void cleanup() {
        long horizon = System.currentTimeMillis() - windowMs;
        while (!samples.isEmpty() && samples.peek().time < horizon) {
            samples.poll();
        }
    }

    private record ChunkSample(long time, long size) {
    }
}