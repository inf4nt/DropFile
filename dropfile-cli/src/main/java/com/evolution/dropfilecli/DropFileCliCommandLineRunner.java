package com.evolution.dropfilecli;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.spring.PicocliSpringFactory;

@RequiredArgsConstructor
@Component
public class DropFileCliCommandLineRunner implements CommandLineRunner {

    private final ApplicationContext applicationContext;

    private final RootCommand root;

    @Override
    public void run(String... args) {
        CommandLine commandLine = new CommandLine(root, new PicocliSpringFactory(applicationContext));
        commandLine.setUnmatchedArgumentsAllowed(true);
//        commandLine.setExecutionExceptionHandler(new CommandLine.IExecutionExceptionHandler() {
//            @Override
//            public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult fullParseResult) throws Exception {
//                System.out.println("HANDLER " + ex.getMessage());
//                return 0;
//            }
//        });
        commandLine.execute(args);
        System.exit(0);
    }
}
