package com.evolution.dropfilecli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DropFileCliApplication {

    public static void main(String[] args) {
        Spinner.start();
        try {
            SpringApplication.run(DropFileCliApplication.class, args);
        } finally {
            Spinner.stop();
        }
    }
}
