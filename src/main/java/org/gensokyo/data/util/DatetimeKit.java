/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.util;

import com.google.common.base.Joiner;
import org.gensokyo.kit.character.StrKit;
import org.joda.time.Duration;
import org.joda.time.Period;

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
        return humanized(Duration.millis(duration));
    }

    public static String humanized(Duration duration) {
        Period p = duration.toPeriod();
        var list = Stream.of(format(p.getYears(), Unit.YEAR), format(p.getMonths(), Unit.MONTH),
                        format(p.getDays(), Unit.DAY), format(p.getHours(), Unit.HOUR),
                        format(p.getMinutes(), Unit.MINUTE), format(p.getSeconds(), Unit.SECOND),
                        format(p.getMillis(), Unit.MILLIS))
                .filter(StrKit::isNotBlank)
                .toList();
        return Joiner.on(", ").skipNulls().join(list);
    }

    private static String format(int n, Unit unit) {
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
