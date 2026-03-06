package com.evolution.dropfilecli;

import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Spinner {

    private static final AtomicReference<String> PROGRESS_INDICATOR = new AtomicReference<>("/");

    private static final AtomicReference<Long> START = new AtomicReference<>(0L);

    private static final AtomicReference<Boolean> EXECUTING = new AtomicReference<>(false);

    public static void start() {
        EXECUTING.set(true);
        Thread.ofVirtual().start(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                START.set(System.currentTimeMillis());
                while (EXECUTING.get() && !Thread.currentThread().isInterrupted()) {
                    String progressIndicator = getProgressIndicator();
                    if (EXECUTING.get()) {
                        System.out.print("\r" + progressIndicator);
                    }
                    Thread.sleep(500);
                }
            }
        });
    }

    public static void stop() {
        if (!EXECUTING.get()) {
            return;
        }
        EXECUTING.set(false);
        System.out.print("\r");
        long time = System.currentTimeMillis() - START.get();
        System.out.println("Finished processing " + TimeUnit.MILLISECONDS.toSeconds(time) + " seconds");
    }

    public static String getProgressIndicator() {
        if (PROGRESS_INDICATOR.get().equals("/")) {
            PROGRESS_INDICATOR.set("\\");
        } else {
            PROGRESS_INDICATOR.set("/");
        }
        return "Processing " + PROGRESS_INDICATOR.get();
    }
}
