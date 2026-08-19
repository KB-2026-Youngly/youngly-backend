package com.kb.youngly.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 시연 환경에서 서비스가 바라보는 현재 시각을 제어한다.
 * JWT와 운영체제 로그는 실제 시간을 유지하고 비즈니스 로직과 DB 세션만 이 시각을 사용한다.
 */
public final class YounglyTime {

    public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private static final AtomicReference<Duration> OFFSET =
            new AtomicReference<>(Duration.ZERO);
    private static final AtomicBoolean OVERRIDDEN = new AtomicBoolean(false);

    private YounglyTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.ofInstant(instant(), SERVICE_ZONE);
    }

    public static LocalDate today() {
        return now().toLocalDate();
    }

    public static Instant instant() {
        return Instant.now().plus(OFFSET.get());
    }

    public static long epochSecond() {
        return instant().getEpochSecond();
    }

    public static boolean isOverridden() {
        return OVERRIDDEN.get();
    }

    /** 선택한 날짜로 이동하되 현재 서비스 시각의 시·분·초는 유지한다. */
    public static synchronized LocalDateTime setDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("적용할 날짜가 필요합니다.");
        }

        LocalDateTime virtualNow = now();
        ZonedDateTime realNow = ZonedDateTime.now(SERVICE_ZONE);
        ZonedDateTime target = ZonedDateTime.of(date, virtualNow.toLocalTime(), SERVICE_ZONE);
        OFFSET.set(Duration.between(realNow.toInstant(), target.toInstant()));
        OVERRIDDEN.set(true);
        return now();
    }

    public static synchronized LocalDateTime advanceDays(long days) {
        if (days < 1) {
            throw new IllegalArgumentException("이동 일수는 1일 이상이어야 합니다.");
        }
        return setDate(today().plusDays(days));
    }

    public static synchronized LocalDateTime reset() {
        OFFSET.set(Duration.ZERO);
        OVERRIDDEN.set(false);
        return now();
    }

    /** 기존 Clock 기반 코드와 테스트를 유지하면서 동적으로 가상 시간을 제공한다. */
    public static Clock clock() {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return SERVICE_ZONE;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return zone.equals(SERVICE_ZONE) ? this : Clock.fixed(instant(), zone);
            }

            @Override
            public Instant instant() {
                return YounglyTime.instant();
            }
        };
    }
}
