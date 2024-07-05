/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.faker;

import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import org.gensokyo.data.cache.ShareCache;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.kit.security.Md5Kit;
import org.gensokyo.kit.time.DateTime;
import org.gensokyo.kit.time.Patterns;

import java.util.Date;
import java.util.Objects;

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

    public String format(String date, String pattern) {
        return DateTime.of(date, Patterns.DATETIME_STANDARD).toString(pattern);
    }

    public String now() {
        return DateTime.now().toString();
    }

    public String today() {
        return DateTime.now().toString(Patterns.DATE_STANDARD);
    }

    public String beforeDays(String date, int min, int max) {
        return DateTime.of(date).minusDays(RandomKit.nextInt(min, max)).toString();
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

    public String beforeDays(int min, int max) {
        return DateTime.now().minusDays(RandomKit.nextInt(min, max)).toString();
    }

    public String beforeHours(int min, int max) {
        return DateTime.now().minusHours(RandomKit.nextInt(min, max)).toString();
    }

    public String beforeMinutes(int min, int max) {
        return DateTime.now().minusMinutes(RandomKit.nextInt(min, max)).toString();
    }

    public String beforeSeconds(int min, int max) {
        return DateTime.now().minusSeconds(RandomKit.nextInt(min, max)).toString();
    }

    public String afterDays(String date, int min, int max) {
        return DateTime.of(date).plusDays(RandomKit.nextInt(min, max)).toString();
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

    public String afterDays(int min, int max) {
        return DateTime.now().plusDays(RandomKit.nextInt(min, max)).toString();
    }

    public String afterHours(int min, int max) {
        return DateTime.now().plusHours(RandomKit.nextInt(min, max)).toString();
    }

    public String afterMinutes(int min, int max) {
        return DateTime.now().plusMinutes(RandomKit.nextInt(min, max)).toString();
    }

    public String afterSeconds(int min, int max) {
        return DateTime.now().plusSeconds(RandomKit.nextInt(min, max)).toString();
    }

    public String plusDays(String date, int amount) {
        return DateTime.of(date).plusDays(amount).toString();
    }

    public String plusHours(String date, int amount) {
        return DateTime.of(date).plusHours(amount).toString();
    }

    public String plusMinutes(String date, int amount) {
        return DateTime.of(date).plusMinutes(amount).toString();
    }

    public String plusSeconds(String date, int amount) {
        return DateTime.of(date).plusSeconds(amount).toString();
    }

    public String plusDays(int amount) {
        return DateTime.now().plusDays(amount).toString();
    }

    public String plusHours(int amount) {
        return DateTime.now().plusHours(amount).toString();
    }

    public String plusMinutes(int amount) {
        return DateTime.now().plusMinutes(amount).toString();
    }

    public String plusSeconds(int amount) {
        return DateTime.now().plusSeconds(amount).toString();
    }

    public String minusDays(String date, int amount) {
        return DateTime.of(date).minusDays(amount).toString();
    }

    public String minusHours(String date, int amount) {
        return DateTime.of(date).minusHours(amount).toString();
    }

    public String minusMinutes(String date, int amount) {
        return DateTime.of(date).minusMinutes(amount).toString();
    }

    public String minusSeconds(String date, int amount) {
        return DateTime.of(date).minusSeconds(amount).toString();
    }

    public String minusDays(int amount) {
        return DateTime.now().minusDays(amount).toString();
    }

    public String minusHours(int amount) {
        return DateTime.now().minusHours(amount).toString();
    }

    public String minusMinutes(int amount) {
        return DateTime.now().minusMinutes(amount).toString();
    }

    public String minusSeconds(int amount) {
        return DateTime.now().minusSeconds(amount).toString();
    }

    public Date intervalMinutes(String start, int amount, String salt) {
        String key = Md5Kit.encrypt(start + amount + salt);
        Date d = ShareCache.getAsDate(key);
        if (Objects.isNull(d)) {
            d = DateTime.of(start).withSecond(0).withNano(0).toDate();
            ShareCache.put(key, d);
            return d;
        }
        d = DateTime.of(d).plusMinutes(amount).withSecond(0).withNano(0).toDate();
        ShareCache.put(key, d);
        return d;
    }

    public Date intervalMinutes(int amount, String salt) {
        return intervalMinutes(DateTime.now().startOfDay().toString(), amount, salt);
    }
}
