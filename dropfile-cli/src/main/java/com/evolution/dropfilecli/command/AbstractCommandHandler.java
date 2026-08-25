package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.util.Spinner;
import lombok.SneakyThrows;
import picocli.CommandLine;

public abstract class AbstractCommandHandler implements Runnable {

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @SneakyThrows
    @Override
    public void run() {
        Spinner.stop();
        handle();
    }

    public void handle() throws Exception {
        spec.commandLine().usage(System.out);
    }
}
