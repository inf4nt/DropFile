package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.DropFileCliApplication;
import com.evolution.dropfilecli.util.Spinner;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.spring.PicocliSpringFactory;

import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@Component
public class DropFileCliCommandLineRunner implements CommandLineRunner {

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private final ApplicationContext applicationContext;

    private final RootCommand root;

    private final LiveExecutionStrategy liveExecutionStrategy;

    @Override
    public void run(String... args) {
        CompletableFuture.runAsync(() -> {
            CommandLine commandLine = new CommandLine(root, new PicocliSpringFactory(applicationContext));
            commandLine.setCommandName("dropfile");

            addParameterExceptionHandler(commandLine);
            addExecutionExceptionHandler(commandLine);

            addGlobalOptions(commandLine);

            commandLine.setExecutionStrategy(liveExecutionStrategy);

            int execute = commandLine.execute(args);
            DropFileCliApplication.exit(execute);
        }, executorService).join();
    }

    private void addParameterExceptionHandler(CommandLine commandLine) {
        CommandLine.IParameterExceptionHandler defaultHandler = commandLine.getParameterExceptionHandler();
        commandLine.setParameterExceptionHandler((ex, arr) -> {
            Spinner.stop();
            return defaultHandler.handleParseException(ex, arr);
        });
    }

    private void addExecutionExceptionHandler(CommandLine commandLine) {
        commandLine.setExecutionExceptionHandler(new CommandLine.IExecutionExceptionHandler() {
            @Override
            public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult fullParseResult) throws Exception {
                Spinner.stop();
                if (isStacktraceEnabled(fullParseResult)) {
                    ex.printStackTrace(commandLine.getErr());
                } else {
                    PrintWriter err = commandLine.getErr();
                    err.println(ex.getMessage());
                }

                return 1;
            }

            private boolean isStacktraceEnabled(CommandLine.ParseResult parseResult) {
                return parseResult != null && parseResult.asCommandLineList().stream()
                        .anyMatch(cmd -> cmd.getParseResult().hasMatchedOption("stacktrace"));
            }
        });
    }

    private void addGlobalOptions(CommandLine commandLine) {
        addOptionsToCommand(commandLine.getCommandSpec(), false);
        addOptionsToSubcommandsRecursively(commandLine, true);
    }

    private void addOptionsToSubcommandsRecursively(CommandLine commandLine, boolean hidden) {
        for (CommandLine subCmd : commandLine.getSubcommands().values()) {
            addOptionsToCommand(subCmd.getCommandSpec(), hidden);
            addOptionsToSubcommandsRecursively(subCmd, hidden);
        }
    }

    private void addOptionsToCommand(CommandLine.Model.CommandSpec spec, boolean hidden) {
        if (spec.findOption("--live") == null) {
            spec.addOption(CommandLine.Model.OptionSpec.builder("--live")
                    .type(boolean.class)
                    .description("Run this command in live update mode")
                    .hidden(hidden)
                    .build());
        }

        if (spec.findOption("--ignore-error") == null) {
            spec.addOption(CommandLine.Model.OptionSpec.builder("--ignore-error")
                    .type(boolean.class)
                    .description("Continue --live polling even if the command encounters an error")
                    .hidden(hidden)
                    .build());
        }

        if (spec.findOption("--stacktrace") == null) {
            spec.addOption(CommandLine.Model.OptionSpec.builder("--stacktrace")
                    .type(boolean.class)
                    .description("Show detailed information about error")
                    .hidden(hidden)
                    .build());
        }

        if (spec.findOption("--list") == null) {
            spec.addOption(CommandLine.Model.OptionSpec.builder("--list")
                    .type(boolean.class)
                    .description("Print result as a list")
                    .hidden(hidden)
                    .build());
        }

        if (spec.findOption("--table") == null) {
            spec.addOption(CommandLine.Model.OptionSpec.builder("--table")
                    .type(boolean.class)
                    .description("Print result as a table")
                    .hidden(hidden)
                    .build());
        }
    }
}
