package com.evolution.dropfilecli;

import com.evolution.dropfilecli.util.ProgressIndicator;
import com.evolution.dropfilecli.util.Spinner;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

@Component
public class LiveExecutionStrategy implements CommandLine.IExecutionStrategy {

    private static final int MAX_LIVE_ITERATION = 1_200;

    private static final ProgressIndicator PROGRESS_INDICATOR = ProgressIndicator.live();

    private String lastOutput = "";

    @SneakyThrows
    @Override
    public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException, CommandLine.ParameterException {
        Spinner.stop();

        boolean isLive = isLive(parseResult);
        if (!isLive) {
            return delegateCall(parseResult);
        }

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        int iteration = 0;
        int latestStatusCode = 0;

        System.out.print("\033[H\033[2J");
        while (iteration <= MAX_LIVE_ITERATION) {
            iteration++;

            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                 PrintStream outInterceptor = new PrintStream(outStream);
                 ByteArrayOutputStream errStream = new ByteArrayOutputStream();
                 PrintStream errInterceptor = new PrintStream(errStream)) {

                System.setOut(outInterceptor);
                System.setErr(errInterceptor);

                try {
                    latestStatusCode = delegateCall(parseResult);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!isIgnoreError(parseResult)) {
                        renderScreen(originalOut, outStream.toString(), originalErr, errStream.toString());
                        throw e;
                    }
                }
                renderScreen(originalOut, outStream.toString(), originalErr, errStream.toString());
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

        return latestStatusCode;
    }

    private void renderScreen(PrintStream originalOut, String currentOutput,
                              PrintStream originalErr, String currentError) {

        String progressIndicator = PROGRESS_INDICATOR.getProgressIndicator();

        StringBuilder fullFrame = new StringBuilder()
                .append("\r")
                .append(progressIndicator)
                .append("\n\n");

        if (!currentOutput.isBlank()) {
            fullFrame
                    .append(currentOutput)
                    .append("\n");
        }

        if (!currentError.isBlank()) {
            fullFrame
                    .append(currentError)
                    .append("\n");
        }

        fullFrame.append("Press CTR+C to stop the process");
        String nextOutput = fullFrame.toString();

        if (isCleanAll(nextOutput)) {
            originalOut.print("\033[H\033[2J");
        } else {
            originalOut.print("\033[H");
        }

        originalOut.println("\r" + progressIndicator + "\n");

        if (!currentOutput.isBlank()) {
            originalOut.print(currentOutput + "\n");
        }

        if (!currentError.isBlank()) {
            originalErr.print(currentError + "\n");
        }

        originalOut.println("Press CTR+C to stop the process");

        originalOut.flush();
        originalErr.flush();

        lastOutput = nextOutput;
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
            } catch (IndexOutOfBoundsException _) {
            }
            if (nextLine.length() < lastLine.length()) {
                return true;
            }
        }

        return false;
    }

    private int delegateCall(CommandLine.ParseResult parseResult) {
        return new CommandLine.RunLast().execute(parseResult);
    }

    private boolean isLive(CommandLine.ParseResult parseResult) {
        if (parseResult == null) {
            return false;
        }

        return parseResult.asCommandLineList().stream()
                .anyMatch(cmd -> cmd.getParseResult().hasMatchedOption("live"));
    }

    private boolean isIgnoreError(CommandLine.ParseResult parseResult) {
        if (parseResult == null) {
            return false;
        }
        return parseResult.asCommandLineList().stream()
                .anyMatch(cmd -> cmd.getParseResult().hasMatchedOption("ignore-error"));
    }
}
