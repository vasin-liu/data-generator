package org.gensokyo.data.calcite;

import net.datafaker.Faker;
import org.gensokyo.data.util.RandomKit;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

final class TemplateV2FakerFunctions {
    private static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Faker ZH_CN_FAKER = new Faker(Locale.CHINA);
    private static final String GUANGDONG_SPECIAL_PREFIX = "\u7ca4Z";
    private static final Set<String> VIID_BASE_TYPES = Set.of("01", "02", "03", "99");
    private static final Set<String> VIID_SEMANTIC_TYPES = Set.of("01", "02", "03", "04", "05", "06", "07", "99");
    private static final Map<String, String> PLATE_PROVINCE = Map.ofEntries(
            Map.entry("\u7696", "1"),
            Map.entry("\u4eac", "2"),
            Map.entry("\u95fd", "3"),
            Map.entry("\u7518", "4"),
            Map.entry("\u7ca4", "5"),
            Map.entry("\u6842", "6"),
            Map.entry("\u8d35", "7"),
            Map.entry("\u743c", "8"),
            Map.entry("\u5180", "9"),
            Map.entry("\u8c6b", "10"),
            Map.entry("\u9ed1", "11"),
            Map.entry("\u9102", "12"),
            Map.entry("\u6e58", "13"),
            Map.entry("\u5409", "14"),
            Map.entry("\u82cf", "15"),
            Map.entry("\u8d63", "16"),
            Map.entry("\u8fbd", "17"),
            Map.entry("\u8499", "18"),
            Map.entry("\u5b81", "19"),
            Map.entry("\u9752", "20"),
            Map.entry("\u9c81", "21"),
            Map.entry("\u664b", "22"),
            Map.entry("\u9655", "23"),
            Map.entry("\u6caa", "24"),
            Map.entry("\u5ddd", "25"),
            Map.entry("\u6d25", "26"),
            Map.entry("\u85cf", "27"),
            Map.entry("\u65b0", "28"),
            Map.entry("\u4e91", "29"),
            Map.entry("\u6d59", "30"),
            Map.entry("\u6e1d", "31"),
            Map.entry("\u6fb3", "88"),
            Map.entry("\u6e2f", "89")
    );

    private TemplateV2FakerFunctions() {
    }

    static long snowflake() {
        return RandomKit.id();
    }

    static String text(int min, int max) {
        return RandomKit.text(min, max);
    }

    static int numberBetween(int min, int max) {
        if (max <= min) {
            throw new IllegalArgumentException("faker_number_between requires max > min");
        }
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    static String phoneCell() {
        synchronized (ZH_CN_FAKER) {
            return ZH_CN_FAKER.phoneNumber().cellPhone();
        }
    }

    static String datePast(int days, String pattern) {
        LocalDateTime value = LocalDateTime.now()
                .minusDays(ThreadLocalRandom.current().nextInt(0, Math.max(days, 1) + 1));
        return format(value, pattern);
    }

    static String datetimeNow() {
        return format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss");
    }

    static long datetimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    static String datetimeMinusDays(int amount) {
        return formatDefault(LocalDateTime.now().minusDays(amount));
    }

    static String datetimeMinusDays(Object value, int amount) {
        return formatDefault(toDateTime(value).minusDays(amount));
    }

    static String datetimeMinusHours(int amount) {
        return formatDefault(LocalDateTime.now().minusHours(amount));
    }

    static String datetimeMinusHours(Object value, int amount) {
        return formatDefault(toDateTime(value).minusHours(amount));
    }

    static String datetimeMinusMinutes(int amount) {
        return formatDefault(LocalDateTime.now().minusMinutes(amount));
    }

    static String datetimeMinusMinutes(Object value, int amount) {
        return formatDefault(toDateTime(value).minusMinutes(amount));
    }

    static String datetimeMinusSeconds(int amount) {
        return formatDefault(LocalDateTime.now().minusSeconds(amount));
    }

    static String datetimeMinusSeconds(Object value, int amount) {
        return formatDefault(toDateTime(value).minusSeconds(amount));
    }

    static String datetimePlusDays(int amount) {
        return formatDefault(LocalDateTime.now().plusDays(amount));
    }

    static String datetimePlusDays(Object value, int amount) {
        return formatDefault(toDateTime(value).plusDays(amount));
    }

    static String datetimePlusHours(int amount) {
        return formatDefault(LocalDateTime.now().plusHours(amount));
    }

    static String datetimePlusHours(Object value, int amount) {
        return formatDefault(toDateTime(value).plusHours(amount));
    }

    static String datetimePlusMinutes(int amount) {
        return formatDefault(LocalDateTime.now().plusMinutes(amount));
    }

    static String datetimePlusMinutes(Object value, int amount) {
        return formatDefault(toDateTime(value).plusMinutes(amount));
    }

    static String datetimePlusSeconds(int amount) {
        return formatDefault(LocalDateTime.now().plusSeconds(amount));
    }

    static String datetimePlusSeconds(Object value, int amount) {
        return formatDefault(toDateTime(value).plusSeconds(amount));
    }

    static String datetimeFormat(Object value) {
        return formatDefault(toDateTime(value));
    }

    static String datetimeFormat(Object value, String pattern) {
        return format(toDateTime(value), pattern);
    }

    static String datetimeParse(Object value) {
        return formatDefault(toDateTime(value));
    }

    static String datetimeBeforeDays(int min, int max) {
        return formatDefault(LocalDateTime.now().minusDays(randomBetween(min, max)));
    }

    static String datetimeBeforeDays(Object value, int min, int max) {
        return formatDefault(toDateTime(value).minusDays(randomBetween(min, max)));
    }

    static String datetimeBeforeHours(int min, int max) {
        return formatDefault(LocalDateTime.now().minusHours(randomBetween(min, max)));
    }

    static String datetimeBeforeHours(Object value, int min, int max) {
        return formatDefault(toDateTime(value).minusHours(randomBetween(min, max)));
    }

    static String datetimeBeforeMinutes(int min, int max) {
        return formatDefault(LocalDateTime.now().minusMinutes(randomBetween(min, max)));
    }

    static String datetimeBeforeMinutes(Object value, int min, int max) {
        return formatDefault(toDateTime(value).minusMinutes(randomBetween(min, max)));
    }

    static String datetimeBeforeSeconds(int min, int max) {
        return formatDefault(LocalDateTime.now().minusSeconds(randomBetween(min, max)));
    }

    static String datetimeBeforeSeconds(Object value, int min, int max) {
        return formatDefault(toDateTime(value).minusSeconds(randomBetween(min, max)));
    }

    static String datetimeAfterDays(int min, int max) {
        return formatDefault(LocalDateTime.now().plusDays(randomBetween(min, max)));
    }

    static String datetimeAfterDays(Object value, int min, int max) {
        return formatDefault(toDateTime(value).plusDays(randomBetween(min, max)));
    }

    static String datetimeAfterHours(int min, int max) {
        return formatDefault(LocalDateTime.now().plusHours(randomBetween(min, max)));
    }

    static String datetimeAfterHours(Object value, int min, int max) {
        return formatDefault(toDateTime(value).plusHours(randomBetween(min, max)));
    }

    static String datetimeAfterMinutes(int min, int max) {
        return formatDefault(LocalDateTime.now().plusMinutes(randomBetween(min, max)));
    }

    static String datetimeAfterMinutes(Object value, int min, int max) {
        return formatDefault(toDateTime(value).plusMinutes(randomBetween(min, max)));
    }

    static String datetimeAfterSeconds(int min, int max) {
        return formatDefault(LocalDateTime.now().plusSeconds(randomBetween(min, max)));
    }

    static String datetimeAfterSeconds(Object value, int min, int max) {
        return formatDefault(toDateTime(value).plusSeconds(randomBetween(min, max)));
    }

    static String vehicleCnPlateProvince(String plate) {
        if (plate == null || plate.isBlank()) {
            return "";
        }
        if (plate.startsWith(GUANGDONG_SPECIAL_PREFIX)) {
            return PLATE_PROVINCE.getOrDefault(plate.substring(plate.length() - 1), "");
        }
        return PLATE_PROVINCE.getOrDefault(plate.substring(0, 1), "");
    }

    static String snowflakeViid(String deviceId, String baseType, Object passTime, String semanticType) {
        return Objects.requireNonNull(deviceId, "deviceId")
                + requireSupported(baseType, VIID_BASE_TYPES, "baseType")
                + Objects.toString(passTime, "")
                + randomNumericString(1, 99999)
                + requireSupported(semanticType, VIID_SEMANTIC_TYPES, "semanticType")
                + randomNumericString(1, 99999);
    }

    private static String format(LocalDateTime value, String pattern) {
        return value.format(DateTimeFormatter.ofPattern(pattern, Locale.ROOT));
    }

    private static String formatDefault(LocalDateTime value) {
        return value.format(DEFAULT_DATETIME_FORMATTER);
    }

    private static int randomBetween(int min, int max) {
        if (max <= min) {
            throw new IllegalArgumentException("faker datetime range requires max > min");
        }
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    private static String randomNumericString(int min, int max) {
        return String.valueOf(randomBetween(min, max));
    }

    private static String requireSupported(String value, Set<String> supportedValues, String name) {
        if (supportedValues.contains(value)) {
            return value;
        }
        throw new UnsupportedOperationException("Unsupported " + name + ": " + value);
    }

    private static LocalDateTime toDateTime(Object value) {
        if (value instanceof java.time.LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().atStartOfDay();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        if (value instanceof CharSequence text) {
            return parseDateTime(text.toString());
        }
        throw new IllegalArgumentException("Unsupported datetime argument: " + value);
    }

    private static LocalDateTime parseDateTime(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Datetime text must not be blank");
        }
        if (text.length() == 12 && text.chars().allMatch(Character::isDigit)) {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyMMddHHmmss", Locale.ROOT));
        }
        if (text.length() == 6 && text.chars().allMatch(Character::isDigit)) {
            return LocalDateTime.of(LocalDateTime.now().toLocalDate(),
                    java.time.LocalTime.parse(text, DateTimeFormatter.ofPattern("HHmmss", Locale.ROOT)));
        }
        if (text.length() == 8 && text.chars().allMatch(Character::isDigit)) {
            return java.time.LocalDate.parse(text, DateTimeFormatter.ofPattern("yyMMdd", Locale.ROOT)).atStartOfDay();
        }
        if (text.contains("T")) {
            return LocalDateTime.parse(text);
        }
        if (text.length() == 19) {
            return LocalDateTime.parse(text, DEFAULT_DATETIME_FORMATTER);
        }
        if (text.length() == 10) {
            return java.time.LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)).atStartOfDay();
        }
        throw new IllegalArgumentException("Unsupported datetime text: " + text);
    }
}
