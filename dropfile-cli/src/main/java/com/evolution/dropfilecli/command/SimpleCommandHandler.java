package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.util.Spinner;
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
