package com.evolution.dropfilecli;

import org.springframework.util.ObjectUtils;

import java.net.http.HttpResponse;

public interface CommandHttpHandler<T> extends Runnable {

    HttpResponse<T> execute() throws Exception;

    void handleSuccessful(HttpResponse<T> response) throws Exception;

    default void handleUnsuccessful(HttpResponse<T> response) throws Exception {
        System.out.println("Unaccepted. HTTP response code: " + response.statusCode());
        T body = response.body();
        if (!ObjectUtils.isEmpty(body)) {
            if (body instanceof byte[]) {
                System.out.println("HTTP response body: " + new String((byte[]) body));
            } else {
                System.out.println("HTTP response body: " + body);
            }
        }
    }

    default void handleError(Exception exception) {
        exception.printStackTrace();
    }

    default boolean isSuccessful(HttpResponse<T> response) {
        return response.statusCode() == 200;
    }

    @Override
    default void run() {
        try {
            HttpResponse<T> httpResponse = execute();
            if (isSuccessful(httpResponse)) {
                handleSuccessful(httpResponse);
            } else {
                handleUnsuccessful(httpResponse);
            }
        } catch (Exception exception) {
            handleError(exception);
        }
    }
}
