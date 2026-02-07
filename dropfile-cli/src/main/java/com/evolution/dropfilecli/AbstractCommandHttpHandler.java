package com.evolution.dropfilecli;

import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

import java.net.http.HttpResponse;

public abstract class AbstractCommandHttpHandler implements Runnable {

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

    protected TypeReference<?> getTypeReference() {
        return null;
    }

    protected void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        TypeReference<?> typeReference = getTypeReference();
        if (typeReference == null) {
            System.out.println("Completed");
            return;
        }
        Object object = objectMapper.readValue(response.body(), typeReference);
        print(object);
    }

    protected void handleUnsuccessful(HttpResponse<byte[]> response) throws Exception {
        System.out.println("Unaccepted. HTTP response code: " + response.statusCode());
        byte[] body = response.body();
        if (ObjectUtils.isEmpty(body)) {
            return;
        }
        System.out.println("HTTP response body: " + new String(body));
    }

    protected void handleError(Exception exception) {
        exception.printStackTrace();
    }

    protected boolean isSuccessful(HttpResponse<byte[]> response) {
        return response.statusCode() == 200;
    }

    @Override
    public void run() {
        try {
            HttpResponse<byte[]> httpResponse = execute();
            if (isSuccessful(httpResponse)) {
                handleSuccessful(httpResponse);
            } else {
                handleUnsuccessful(httpResponse);
            }
        } catch (Exception exception) {
            handleError(exception);
        }
    }

    @SneakyThrows
    protected void print(Object object) {
        if (ObjectUtils.isEmpty(object)) {
            System.out.println("No values present");
            return;
        }

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        System.out.println(json);
    }
}
