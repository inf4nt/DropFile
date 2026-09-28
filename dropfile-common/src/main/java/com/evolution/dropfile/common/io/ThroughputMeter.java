package com.evolution.dropfile.common.io;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public class ThroughputMeter {

    private static final int WINDOW_MILLIS = 5_000;

    private final ConcurrentLinkedQueue<ChunkSample> samples = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean cleaning = new AtomicBoolean();

    private final LongAdder downloaded = new LongAdder();

    private final LongAdder totalBytesInWindow = new LongAdder();

    public void add(long size) {
        if (size <= 0) {
            return;
        }

        samples.add(new ChunkSample(System.currentTimeMillis(), size));
        downloaded.add(size);
        totalBytesInWindow.add(size);

        cleanup();
    }

    public long getSpeedBytesPerSec() {
        cleanup();
        return totalBytesInWindow.sum() / (WINDOW_MILLIS / 1_000);
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
                ChunkSample polled = samples.poll();
                if (polled != null) {
                    totalBytesInWindow.add(-polled.size());
                }
            }
        } finally {
            cleaning.set(false);
        }
    }

    private record ChunkSample(long time, long size) {
    }
}
