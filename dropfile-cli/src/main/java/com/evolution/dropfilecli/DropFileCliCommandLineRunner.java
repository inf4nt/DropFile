package com.evolution.dropfilecli;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.spring.PicocliSpringFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@Component
public class DropFileCliCommandLineRunner implements CommandLineRunner {

    private final ApplicationContext applicationContext;

    private final RootCommand root;

    private final IExecutionStrategyImpl executionStrategy;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @PostConstruct
    public void postConstruct() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdownNow();
        }));
    }

    @Override
    public void run(String... args) {
        CompletableFuture.runAsync(() -> {
            CommandLine commandLine = new CommandLine(root, new PicocliSpringFactory(applicationContext));
            commandLine.setUnmatchedArgumentsAllowed(true);

            CommandLine.Model.OptionSpec liveOption = CommandLine.Model.OptionSpec.builder("--live")
                    .type(boolean.class)
                    .description("Run this command in live update mode")
                    .scopeType(CommandLine.ScopeType.INHERIT)
                    .build();
            commandLine.getCommandSpec().addOption(liveOption);

            CommandLine.Model.OptionSpec ignoreErrorOption = CommandLine.Model.OptionSpec.builder("--ignore-error")
                    .type(boolean.class)
                    .description("Continue polling even if the command encounters an error")
                    .scopeType(CommandLine.ScopeType.INHERIT)
                    .build();
            commandLine.getCommandSpec().addOption(ignoreErrorOption);

            commandLine.setExecutionStrategy(executionStrategy);

//        commandLine.setExecutionExceptionHandler(new CommandLine.IExecutionExceptionHandler() {
//            @Override
//            public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult fullParseResult) throws Exception {
//                System.out.println("HANDLER " + ex.getMessage());
//                return 0;
//            }
//        });
            commandLine.execute(args);
            System.exit(0);
        }, executorService).join();
    }
}
