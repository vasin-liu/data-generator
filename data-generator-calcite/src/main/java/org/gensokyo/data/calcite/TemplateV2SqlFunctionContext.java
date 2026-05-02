package org.gensokyo.data.calcite;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public record TemplateV2SqlFunctionContext(List<Object> arguments) {
    public Object argument(int index) {
        return arguments.get(index);
    }

    public String stringArgument(int index) {
        return Objects.toString(argument(index), "");
    }

    public int intArgument(int index) {
        return asBigDecimal(argument(index)).intValue();
    }

    public LocalDate dateArgument(int index) {
        Object value = argument(index);
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (value instanceof String stringValue) {
            return LocalDate.parse(stringValue);
        }
        throw new IllegalArgumentException("Expected date value but got: " + value);
    }

    public BigDecimal decimalArgument(int index) {
        return asBigDecimal(argument(index));
    }

    public static String toJavaDatePattern(String pattern) {
        return pattern.replace("%Y", "yyyy")
                .replace("%m", "MM")
                .replace("%d", "dd");
    }

    static BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String stringValue) {
            return new BigDecimal(stringValue);
        }
        throw new IllegalArgumentException("Expected numeric value but got: " + value);
    }
}
