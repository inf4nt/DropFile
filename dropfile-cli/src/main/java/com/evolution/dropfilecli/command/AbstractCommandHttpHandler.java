package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.client.DaemonClient;
import com.evolution.dropfilecli.config.CliApplicationProperties;
import com.evolution.dropfilecli.util.Spinner;
import com.evolution.dropfilecli.util.TablePrinter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.lang.reflect.ParameterizedType;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.StreamSupport;

public abstract class AbstractCommandHttpHandler<TR> extends AbstractCommandHandler {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    protected DaemonClient daemonClient;

    protected ObjectMapper objectMapper;

    protected CliApplicationProperties applicationProperties;

    @Autowired
    public void setDaemonClient(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Autowired
    public void setApplicationProperties(CliApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public abstract HttpResponse<byte[]> execute() throws Exception;

    protected TypeReference<TR> getTypeReference() {
        return null;
    }

    protected void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        TypeReference<TR> typeReference = getTypeReference();
        if (typeReference == null) {
            System.out.println("Completed");
            return;
        }
        TR object = objectMapper.readValue(response.body(), typeReference);
        print(object);
    }

    protected void handleUnsuccessful(HttpResponse<byte[]> response) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        HttpRequest request = response.request();
        stringBuilder.append("Daemon request failed %s %s".formatted(request.method(), request.uri()));
        if (!ObjectUtils.isEmpty(response.body())) {
            stringBuilder.append("\n").append(new String(response.body()));
        }
        throw new IllegalStateException(stringBuilder.toString());
    }

    protected boolean isSuccessful(HttpResponse<byte[]> response) {
        return response.statusCode() == 200;
    }

    @SneakyThrows
    @Override
    public void run() {
        handle();
    }

    @Override
    public void handle() throws Exception {
        try {
            Spinner.start();
            HttpResponse<byte[]> httpResponse = execute();
            Spinner.stop();
            if (isSuccessful(httpResponse)) {
                handleSuccessful(httpResponse);
            } else {
                handleUnsuccessful(httpResponse);
            }
        } finally {
            Spinner.stop();
        }
    }

    protected PrintModeEnum getPrintMode() {
        PrintModeEnum printModeDefault = applicationProperties.printModeDefault;
        if (printModeDefault != null) {
            return printModeDefault;
        }

        TypeReference<?> typeReference = getTypeReference();
        if (typeReference == null) {
            return PrintModeEnum.LIST;
        }
        if (typeReference.getType() instanceof ParameterizedType parameterizedType) {
            if (Iterable.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
                return PrintModeEnum.TABLE;
            }
        }
        return PrintModeEnum.LIST;
    }

    protected void print(TR object) {
        if (isTable()) {
            printTable(object);
        } else if (isList()) {
            printList(object);
        } else {
            PrintModeEnum printMode = getPrintMode();
            if (printMode == PrintModeEnum.TABLE) {
                printTable(object);
            } else {
                printList(object);
            }
        }
    }

    @SneakyThrows
    protected void printList(Object object) {
        if (ObjectUtils.isEmpty(object)) {
            System.out.println("No values present");
            return;
        }
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        System.out.println(json);
    }

    protected void printTable(Object object) {
        if (!(object instanceof Iterable<?> iterable)) {
            throw new IllegalArgumentException("Is not an array. Print table supports only arrays");
        }
        List<?> data = StreamSupport.stream(iterable.spliterator(), false).toList();
        String print = TablePrinter.get(data);
        System.out.println(StringUtils.hasText(print) ? print : "No values present");
    }

    private boolean isTable() {
        CommandLine.ParseResult parseResult = spec.commandLine().getParseResult();
        return parseResult != null && parseResult.asCommandLineList().stream()
                .anyMatch(cmd -> cmd.getParseResult().hasMatchedOption("table"));
    }

    private boolean isList() {
        CommandLine.ParseResult parseResult = spec.commandLine().getParseResult();
        return parseResult != null && parseResult.asCommandLineList().stream()
                .anyMatch(cmd -> cmd.getParseResult().hasMatchedOption("list"));
    }
}
