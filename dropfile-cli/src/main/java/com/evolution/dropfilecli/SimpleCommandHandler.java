package com.evolution.dropfilecli;

import lombok.SneakyThrows;

public interface SimpleCommandHandler extends Runnable {

    @SneakyThrows
    @Override
    default void run() {
        Spinner.stop();
        handle();
    }

    void handle() throws Exception;
}
