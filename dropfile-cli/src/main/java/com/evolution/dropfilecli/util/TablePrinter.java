package com.evolution.dropfilecli.util;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_FixedWidth;
import de.vandermeer.asciithemes.a8.A8_Grids;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class TablePrinter {

    private static final int MAX_TABLE_WIDTH = 190;

    private static final int MAX_LINES = 5;

    private static final int PADDING = 0;

    @SneakyThrows
    public static <T> String get(List<T> list) {
        if (list == null || list.isEmpty()) {
            return "No values present";
        }

        Class<?> clazz = list.getFirst().getClass();
        Field[] fields = clazz.getDeclaredFields();
        Arrays.stream(fields).forEach(it -> it.setAccessible(true));

        int colCount = fields.length;

        String[] headers = Arrays.stream(fields)
                .map(f -> capitalize(f.getName()))
                .toArray(String[]::new);

        int[] intrinsicWidths = new int[colCount];
        for (int i = 0; i < colCount; i++) {
            intrinsicWidths[i] = headers[i].length() + PADDING;
        }

        List<String[]> rows = new ArrayList<>();
        for (T item : list) {
            String[] row = new String[colCount];
            for (int i = 0; i < colCount; i++) {
                Object val = fields[i].get(item);
                if (val instanceof Instant instant) {
                    val = DateUtils.FORMATTER.format(instant);
                }
                String strVal = (val == null) ? "" : val.toString();
                row[i] = strVal;
                intrinsicWidths[i] = Math.max(intrinsicWidths[i], strVal.length() + PADDING);
            }
            rows.add(row);
        }

        int usableWidth = MAX_TABLE_WIDTH - (colCount + 1);
        int[] finalWidths = calculateSmartWidths(intrinsicWidths, usableWidth);

        List<String[]> processedRows = new ArrayList<>();
        for (String[] row : rows) {
            String[] processedRow = new String[colCount];
            for (int i = 0; i < colCount; i++) {
                processedRow[i] = truncateToMaxLines(row[i], finalWidths[i]);
            }
            processedRows.add(processedRow);
        }

        AsciiTable at = new AsciiTable();
        at.addRule();
        at.addRow((Object[]) headers);
        at.addRule();
        for (String[] row : processedRows) {
            at.addRow((Object[]) row);
            at.addRule();
        }

        CWC_FixedWidth cwc = new CWC_FixedWidth();
        for (int w : finalWidths) {
            cwc.add(w);
        }
        at.getRenderer().setCWC(cwc);

        at.getContext().setGrid(A8_Grids.lineDobuleTripple());
        return at.render();
    }

    private static int[] calculateSmartWidths(int[] widths, int targetTotal) {
        int[] current = Arrays.copyOf(widths, widths.length);
        int currentTotal = IntStream.of(current).sum();

        if (currentTotal <= targetTotal) {
            return current;
        }

        while (IntStream.of(current).sum() > targetTotal) {
            int maxIdx = -1;
            int maxVal = -1;

            for (int i = 0; i < current.length; i++) {
                if (current[i] > maxVal) {
                    maxVal = current[i];
                    maxIdx = i;
                }
            }

            if (maxIdx == -1 || current[maxIdx] <= 3) {
                break;
            }

            current[maxIdx]--;
        }

        return current;
    }

    private static String truncateToMaxLines(String text, int width) {
        if (text == null) {
            return "";
        }
        int effectiveWidth = width - PADDING;
        if (effectiveWidth <= 0) {
            effectiveWidth = 1;
        }

        int maxChars = effectiveWidth * MAX_LINES;
        if (text.length() <= maxChars) {
            return text;
        }

        return text.substring(0, maxChars - 3) + "...";
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}