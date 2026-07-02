package com.evolution.dropfilecli;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class IExecutionStrategyImpl implements CommandLine.IExecutionStrategy {

    private static final int MAX_LIVE_ITERATION = 1_200;

    private String lastOutput = "";

    private String progressIndicator;

    @SneakyThrows
    @Override
    public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException, CommandLine.ParameterException {
        Spinner.stop();

        boolean isLive = isLive(parseResult);
        if (!isLive) {
            return delegateCall(parseResult);
        }

        System.out.print("\033[H\033[2J");

        PrintStream originalOut = System.out;

        int iteration = 0;
        int latestStatusCode = 0;

        while (iteration <= MAX_LIVE_ITERATION) {
            iteration++;

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                 PrintStream interceptorOut = new PrintStream(outputStream)) {

                System.setOut(interceptorOut);

                latestStatusCode = delegateCall(parseResult);

                String currentOutput = outputStream.toString();
                renderScreen(originalOut, currentOutput);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

        return latestStatusCode;
    }

    private void renderScreen(PrintStream originalOut, String currentOutput) {
        String nextOutput = new StringBuilder()
                .append("\r" + getProgressIndicator())
                .append("\n")
                .append("\n")
                .append(currentOutput)
                .append("\n")
                .append("Press CTR+C to stop the process")
                .toString();

        boolean cleanAll = isCleanAll(nextOutput);

        if (cleanAll) {
            originalOut.print("\033[H\033[2J");
        } else {
            originalOut.print("\033[H");
        }

        originalOut.println(nextOutput);
        lastOutput = nextOutput;
    }

    private String getProgressIndicator() {
        if ("Live /".equals(progressIndicator)) {
            progressIndicator = "Live \\";
            return progressIndicator;
        }
        progressIndicator = "Live /";
        return progressIndicator;
    }

    private boolean isCleanAll(String nextOutput) {
        List<String> nextOutputLines = Arrays.stream(nextOutput.split("\n")).toList();
        List<String> lastOutputLines = Arrays.stream(lastOutput.split("\n")).toList();

        if (nextOutputLines.size() < lastOutputLines.size()) {
            return true;
        }

        for (int i = 0; i < nextOutputLines.size(); i++) {
            String nextLine = nextOutputLines.get(i);
            String lastLine = "";
            try {
                lastLine = lastOutputLines.get(i);
            } catch (IndexOutOfBoundsException e) {
            }
            if (nextLine.length() < lastLine.length()) {
                return true;
            }
        }

        return false;
    }

    private int delegateCall(CommandLine.ParseResult parseResult) {
        Random random = new Random();
        int randomInt = random.nextInt(1, 5);

        for (int i = 0; i < randomInt; i++) {
            System.out.println("a");
        }

        new CommandLine.RunLast().execute(parseResult);

        return 0;
    }

    private boolean isLive(CommandLine.ParseResult parseResult) {
        return parseResult.hasMatchedOption("live") ||
                parseResult.subcommands().stream().anyMatch(sub -> sub.hasMatchedOption("live"));
    }

    @SneakyThrows
    static void main() {
        PrintStream originalOut = System.out;

        PrintStream err = System.err;

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             PrintStream interceptorOut = new PrintStream(outputStream)) {

            System.setOut(interceptorOut);
            System.setErr(interceptorOut);

            try {
                call();
            } catch (Exception e) {
                e.printStackTrace();
            }

            String currentOutput = outputStream.toString();

            originalOut.println(currentOutput);
        }
    }

    public static void call() {
        System.out.println("call");

        if (true) {
            throw new RuntimeException();
        }
    }
}
