package com.evolution.dropfilecli.util;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TablePrinter {

    private static final int MAX_COL_WIDTH = 40;

    private static final int MAX_VALUE_LENGTH = 30;

    public static void print(List<?> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("No values present");
            return;
        }

        Class<?> type = rows.getFirst().getClass();
        List<Field> fields = getPrintableFields(type);

        if (fields.isEmpty()) {
            throw new UnsupportedOperationException("No printable fields");
        }

        List<String> headers = new ArrayList<>();
        for (Field f : fields) {
            headers.add(f.getName());
        }

        List<List<String>> data = extractData(rows, fields);
        List<Integer> widths = computeWidths(headers, data);

        printHeader(headers, widths);
        printSeparator(widths);
        printRows(data, widths);
    }

    private static List<Field> getPrintableFields(Class<?> type) {
        List<Field> result = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            result.add(field);
        }
        return result;
    }

    @SneakyThrows
    private static List<List<String>> extractData(List<?> rows, List<Field> fields) {
        List<List<String>> result = new ArrayList<>();

        for (Object row : rows) {
            List<String> line = new ArrayList<>();
            for (Field field : fields) {
                Object value = field.get(row);
                if (value instanceof Instant) {
                    value = DateUtils.FORMATTER.format((Instant) value);
                }
                String s = value == null ? "" : value.toString();
                line.add(truncateValue(s));

            }
            result.add(line);
        }
        return result;
    }

    private static List<Integer> computeWidths(List<String> headers, List<List<String>> rows) {
        List<Integer> widths = new ArrayList<>();

        for (int col = 0; col < headers.size(); col++) {
            int max = headers.get(col).length();
            for (List<String> row : rows) {
                max = Math.max(max, row.get(col).length());
            }
            widths.add(Math.min(max, MAX_COL_WIDTH));
        }
        return widths;
    }

    private static void printHeader(List<String> headers, List<Integer> widths) {
        for (int i = 0; i < headers.size(); i++) {
            System.out.print(pad(headers.get(i), widths.get(i)));
            if (i < headers.size() - 1) {
                System.out.print(" ");
            }
        }
        System.out.println();
    }

    private static void printSeparator(List<Integer> widths) {
        int total = widths.stream().mapToInt(Integer::intValue).sum()
                + (widths.size() - 1) * 2;
        System.out.println("-".repeat(total));
    }

    private static void printRows(List<List<String>> rows, List<Integer> widths) {
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                System.out.print(pad(row.get(i), widths.get(i)));
                if (i < row.size() - 1) {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }

    private static String truncateValue(String s) {
        if (s.length() > MAX_VALUE_LENGTH) {
            return s.substring(0, MAX_VALUE_LENGTH - 1) + "..";
        }
        return s;
    }

    private static String pad(String s, int width) {
        if (s.length() > width) {
            return s.substring(0, width - 1) + "..";
        }
        return String.format("%-" + width + "s", s);
    }
}
