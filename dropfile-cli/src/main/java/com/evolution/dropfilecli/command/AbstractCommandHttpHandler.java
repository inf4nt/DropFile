package com.evolution.dropfilecli.command;

import com.evolution.dropfile.common.dto.ApiErrorDTO;
import com.evolution.dropfilecli.client.DaemonClient;
import com.evolution.dropfilecli.util.Spinner;
import com.evolution.dropfilecli.util.TablePrinter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;

import java.lang.reflect.ParameterizedType;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.StreamSupport;

public abstract class AbstractCommandHttpHandler<TR> implements SimpleCommandHandler {

    @CommandLine.Option(names = {"-table", "--table"}, description = "Print table", defaultValue = "false")
    protected boolean table;

    @CommandLine.Option(names = {"-list", "--list"}, description = "Print list", defaultValue = "false")
    protected boolean list;

    @CommandLine.Option(names = {"-verbose", "--verbose"}, description = "Show detailed error information", defaultValue = "false")
    protected boolean verbose;

    protected DaemonClient daemonClient;

    protected ObjectMapper objectMapper;

    @Autowired
    public void setDaemonClient(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
        System.out.println("Unaccepted. HTTP response code: " + response.statusCode());
        byte[] body = response.body();
        if (ObjectUtils.isEmpty(body)) {
            return;
        }
        ApiErrorDTO apiErrorDTO = objectMapper.readValue(body, ApiErrorDTO.class);
        System.out.println("Class: " + apiErrorDTO.clazz());
        System.out.println("Message: " + apiErrorDTO.message());
        if (verbose) {
            System.out.println("Stacktrace: " + apiErrorDTO.stacktrace());
        }
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
        if (table) {
            printTable(object);
        } else if (list) {
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
            throw new UnsupportedOperationException("Print table supports only Iterable.class");
        }
        List<?> data = StreamSupport.stream(iterable.spliterator(), false).toList();
        String print = TablePrinter.get(data);
        System.out.println(print);
    }

    protected enum PrintModeEnum {
        TABLE,
        LIST
    }
}
