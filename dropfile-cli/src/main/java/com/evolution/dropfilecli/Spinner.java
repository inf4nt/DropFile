package com.evolution.dropfilecli;

import java.util.concurrent.atomic.AtomicBoolean;

public class Spinner {

    private static final ProgressIndicator PROGRESS_INDICATOR = ProgressIndicator.processing();

    private static final AtomicBoolean EXECUTING = new AtomicBoolean(false);

    volatile private static Thread SPINNER_THREAD;

    public static void start() {
        if (!EXECUTING.compareAndSet(false, true)) {
            return;
        }

        SPINNER_THREAD = Thread.ofVirtual().start(() -> {
            try {
                while (EXECUTING.get() && !Thread.currentThread().isInterrupted()) {
                    String progressIndicator = PROGRESS_INDICATOR.getProgressIndicator();

                    System.out.print("\r" + progressIndicator);
                    System.out.flush();

                    Thread.sleep(150);
                }
            } catch (InterruptedException _) {

            } finally {
                System.out.print("\r\u001b[K");
                System.out.flush();
            }
        });
    }

    public static void stop() {
        if (!EXECUTING.compareAndSet(true, false)) {
            return;
        }
        if (SPINNER_THREAD != null) {
            SPINNER_THREAD.interrupt();
        }
    }
}
