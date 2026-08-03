package com.kb.youngly.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class IdGenerator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generateAccountId() {
        return FORMATTER.format(LocalDateTime.now())
                + "-"
                + UUID.randomUUID().toString().substring(0, 8);
    }

}