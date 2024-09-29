/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.util;

import org.gensokyo.kit.character.StrKit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 时间工具
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/1 , Version 1.0.0
 */
public final class DatetimeKit {

    private DatetimeKit() {
        throw new UnsupportedOperationException();
    }

    public static String humanized(long duration) {
        return humanized(Duration.of(duration, ChronoUnit.MILLIS));
    }

    public static String humanized(Duration d) {
        return Stream.of(format(d.toDaysPart(), Unit.DAY),
                        format(d.toHoursPart(), Unit.HOUR),
                        format(d.toMinutesPart(), Unit.MINUTE),
                        format(d.toSecondsPart(), Unit.SECOND),
                        format(d.toMillisPart(), Unit.MILLIS))
                .filter(StrKit::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    private static String format(long n, Unit unit) {
        if (n > 0) {
            return n + unit.desc();
        }
        return StrKit.EMPTY;
    }

    enum Unit {
        YEAR("年"), MONTH("月"), DAY("天"), HOUR("小时"), MINUTE("分钟"), SECOND("秒"), MILLIS("毫秒");

        final String name;

        Unit(String name) {
            this.name = name;
        }

        String desc() {
            return this.name;
        }
    }
}
