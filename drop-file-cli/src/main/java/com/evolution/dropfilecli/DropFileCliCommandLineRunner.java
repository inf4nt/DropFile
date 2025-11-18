package com.evolution.dropfilecli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.spring.PicocliSpringFactory;

@Component
public class DropFileCliCommandLineRunner implements CommandLineRunner {

    private final ApplicationContext applicationContext;

    private final RootCommand root;

    @Autowired
    public DropFileCliCommandLineRunner(ApplicationContext applicationContext, RootCommand root) {
        this.applicationContext = applicationContext;
        this.root = root;
    }

    @Override
    public void run(String... args) {
        new CommandLine(root, new PicocliSpringFactory(applicationContext)).execute(args);
        System.exit(0);
    }
}
