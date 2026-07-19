package com.evolution.dropfiledaemon.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public class ThroughputMeter {

    private static final int WINDOW_MILLIS = 5_000;

    private final ConcurrentLinkedQueue<ChunkSample> samples = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean cleaning = new AtomicBoolean();

    private final LongAdder downloaded = new LongAdder();

    public void add(long size) {
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

    public long getTotalThroughput() {
        return downloaded.sum();
    }

    private void cleanup() {
        if (!cleaning.compareAndSet(false, true)) {
            return;
        }

        try {
            long horizon = System.currentTimeMillis() - WINDOW_MILLIS;
            while (!samples.isEmpty() && samples.peek().time < horizon) {
                samples.poll();
            }
        } finally {
            cleaning.set(false);
        }
    }

    private record ChunkSample(long time, long size) {
    }
}
