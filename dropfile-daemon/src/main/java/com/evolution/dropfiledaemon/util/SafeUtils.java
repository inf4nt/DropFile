package com.evolution.dropfiledaemon.util;

import com.evolution.dropfiledaemon.util.function.IORunnable;

public class SafeUtils {

    public static void execute(IORunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
