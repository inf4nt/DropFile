package com.evolution.dropfilecli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DropFileCliApplication {

    private static ConfigurableApplicationContext context;

    private static int EXIT_CODE = 0;

    public static void main(String[] args) {
        Spinner.start();
        try {
            context = SpringApplication.run(DropFileCliApplication.class, args);
        } finally {
            Spinner.stop();
        }
        System.exit(EXIT_CODE);
    }

    public static void exit(int code) {
        EXIT_CODE = code;
        if (context != null) {
            SpringApplication.exit(context, () -> code);
        }
    }
}