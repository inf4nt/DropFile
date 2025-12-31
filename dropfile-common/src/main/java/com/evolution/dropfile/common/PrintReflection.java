package com.evolution.dropfile.common;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.List;

public class PrintReflection {

    private static final String SEPARATOR = "---------------------------";

    @SneakyThrows
    public static void print(Object object) {
        if (object instanceof Iterable) {
            print((Iterable<?>) object, SEPARATOR);
        } else {
            print(List.of(object), SEPARATOR);
        }
    }

    @SneakyThrows
    public static void print(Iterable<?> objects, String separator) {
        if (!objects.iterator().hasNext()) {
            return;
        }

        for (Object object : objects) {
            Field[] declaredFields = object.getClass().getDeclaredFields();

            if (declaredFields.length == 0) {
                return;
            }

            System.out.println(separator);
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                String fieldName = declaredField.getName();
                fieldName = capitalize(fieldName);
                Object value = declaredField.get(object);
                System.out.println(String.format("%s: %s", fieldName, value));
            }
        }
        System.out.println(separator);
    }

    private static String capitalize(String string) {
        String first = String.valueOf(string.charAt(0)).toUpperCase();
        String rest = string.substring(1);
        return first + rest;
    }
}
