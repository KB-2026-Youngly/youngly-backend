package com.kb.youngly.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class IdGenerator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generateAccountId() {
        return FORMATTER.format(YounglyTime.now())
                + "-"
                + UUID.randomUUID().toString().substring(0, 8);
    }
    public static String generateMoimAccountId() {
        return FORMATTER.format(YounglyTime.now())
                + "-"
                + UUID.randomUUID().toString().substring(0, 8);
    }

}
