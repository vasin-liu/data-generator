/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.faker;

import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.kit.time.DateTime;

import java.util.List;

/**
 * 时间数据提供者
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/2 , Version 1.0.0
 */
public class DateTimeProvider extends AbstractProvider<BaseProviders> {

    public DateTimeProvider(BaseProviders faker) {
        super(faker);
    }

    public String text(int min, int max) {
        return RandomKit.text(min, max);
    }

    public List<Integer> seq(int end) {
        return seq(1, end);
    }

    public List<Integer> seq(int start, int end) {
        return seq(start, end, 1);
    }

    public List<Integer> seq(int start, int end, int step) {
        return RandomKit.seq(start, end, step);
    }

    public String beforeHours(String date, int min, int max) {
        return DateTime.of(date).minusHours(RandomKit.nextInt(min, max)).toString();
    }

    public String beforeMinutes(String date, int min, int max) {
        return DateTime.of(date).minusMinutes(RandomKit.nextInt(min, max)).toString();
    }

    public String beforeSeconds(String date, int min, int max) {
        return DateTime.of(date).minusSeconds(RandomKit.nextInt(min, max)).toString();
    }

    public String afterHours(String date, int min, int max) {
        return DateTime.of(date).plusHours(RandomKit.nextInt(min, max)).toString();
    }

    public String afterMinutes(String date, int min, int max) {
        return DateTime.of(date).plusMinutes(RandomKit.nextInt(min, max)).toString();
    }

    public String afterSeconds(String date, int min, int max) {
        return DateTime.of(date).plusSeconds(RandomKit.nextInt(min, max)).toString();
    }
}
