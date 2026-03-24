package com.evolution.dropfilecli;

import lombok.SneakyThrows;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public class LivePrinter {

    private static final int MAX_LIVE_ITERATION = 1_200;

    private static final AtomicReference<String> PROGRESS_INDICATOR = new AtomicReference<>("/");

    private static String PREV_LIVE;

    private static String NEXT_LIVE;

    public static void live(Runnable runnable) {
        boolean first = true;

        int iteration = 0;
        while (!Thread.currentThread().isInterrupted() && iteration <= MAX_LIVE_ITERATION) {
            iteration++;

            runnable.run();
            Spinner.stop();
            if (first) {
                System.out.print("\033[H\033[2J");
                first = false;
            } else {
                System.out.print("\033[H");
            }
            if (PREV_LIVE != null && NEXT_LIVE != null && (NEXT_LIVE.length() < PREV_LIVE.length())) {
                System.out.print("\033[H\033[2J");
            }

            String progressIndicator = getProgressIndicator();
            System.out.print("\r" + progressIndicator);
            System.out.println();

            System.out.println(NEXT_LIVE);
            System.out.println("Press CTR+C to stop the process");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @SneakyThrows
    public static void printLive(Callable<String> callable) {
        PREV_LIVE = NEXT_LIVE;
        NEXT_LIVE = callable.call();
    }

    private static String getProgressIndicator() {
        if (PROGRESS_INDICATOR.get().equals("/")) {
            PROGRESS_INDICATOR.set("\\");
        } else {
            PROGRESS_INDICATOR.set("/");
        }
        return "Live " + PROGRESS_INDICATOR.get();
    }
}
