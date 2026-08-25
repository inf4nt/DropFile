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

            hideInheritedOptionsFromHelp(commandLine);

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
                    return 1;
                }

                PrintWriter err = commandLine.getErr();
                err.println(ex.getMessage());
                return 1;
            }

            private boolean isStacktraceEnabled(CommandLine.ParseResult parseResult) {
                if (parseResult == null) {
                    return false;
                }
                return parseResult.asCommandLineList().stream()
                        .anyMatch(cmd -> cmd.getParseResult().hasMatchedOption("stacktrace"));
            }
        });
    }

    private void addGlobalOptions(CommandLine commandLine) {
        CommandLine.Model.CommandSpec rootSpec = commandLine.getCommandSpec();

        rootSpec.addOption(CommandLine.Model.OptionSpec.builder("--live")
                .type(boolean.class)
                .description("Run this command in live update mode")
                .scopeType(CommandLine.ScopeType.INHERIT)
                .build());

        rootSpec.addOption(CommandLine.Model.OptionSpec.builder("--ignore-error")
                .type(boolean.class)
                .description("Continue --live polling even if the command encounters an error")
                .scopeType(CommandLine.ScopeType.INHERIT)
                .build());

        rootSpec.addOption(CommandLine.Model.OptionSpec.builder("--stacktrace")
                .type(boolean.class)
                .description("Show detailed information about error")
                .scopeType(CommandLine.ScopeType.INHERIT)
                .build());
    }

    public static void hideInheritedOptionsFromHelp(CommandLine commandLine) {
//        commandLine.getHelpSectionMap().put(
//                CommandLine.Model.UsageMessageSpec.SECTION_KEY_OPTION_LIST,
//                help -> {
//                    CommandLine.Help.Layout layout = help.createDefaultLayout();
//
//                    for (CommandLine.Model.OptionSpec opt : help.commandSpec().options()) {
//                        if (!opt.inherited() && !opt.hidden()) {
//                            layout.addOption(opt, help.parameterLabelRenderer());
//                        }
//                    }
//
//                    return layout.toString();
//                }
//        );
//
//        for (CommandLine sub : commandLine.getSubcommands().values()) {
//            hideInheritedOptionsFromHelp(sub);
//        }
    }
}
