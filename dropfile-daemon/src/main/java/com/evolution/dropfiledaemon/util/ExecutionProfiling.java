package com.evolution.dropfiledaemon.util;

import com.evolution.dropfiledaemon.util.function.IORunnable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ExecutionProfiling {

    public static void run(String operation, IORunnable runnable) {
        run(
                operation,
                () -> {
                    runnable.run();
                    return null;
                }
        );
    }

    @SneakyThrows
    public static <T> T run(String operation, Callable<T> call) {
        long start = System.currentTimeMillis();
        log.info("Operation {} starting", operation);
        try {
            T result = call.call();
            long end = System.currentTimeMillis();
            long duration = end - start;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
            log.info("Operation {} finished. Millis {} seconds {}", operation, duration, seconds);
            return result;
        } catch (Exception e) {
            long end = System.currentTimeMillis();
            long duration = end - start;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
            log.info("[{}] finished with error. Millis {} seconds {}", operation, duration, seconds);
            throw e;
        }
    }
}
