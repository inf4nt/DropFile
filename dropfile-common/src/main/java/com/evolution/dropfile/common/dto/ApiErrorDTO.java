package com.evolution.dropfile.common.dto;

public record ApiErrorDTO(String clazz,
                          String message,
                          String stacktrace) {
}
