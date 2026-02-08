package com.evolution.dropfilecli.util;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneOffset.UTC);
}
