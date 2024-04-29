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

    public String plusDays(String date,int amount) {
        return DateTime.of(date).plusDays(amount).toString();
    }

    public String plusHours(String date,int amount) {
        return DateTime.of(date).plusHours(amount).toString();
    }

    public String plusMinutes(String date,int amount) {
        return DateTime.of(date).plusMinutes(amount).toString();
    }

    public String plusSeconds(String date,int amount) {
        return DateTime.of(date).plusSeconds(amount).toString();
    }

    public String minusDays(String date,int amount) {
        return DateTime.of(date).minusDays(amount).toString();
    }

    public String minusHours(String date,int amount) {
        return DateTime.of(date).minusHours(amount).toString();
    }

    public String minusMinutes(String date,int amount) {
        return DateTime.of(date).minusMinutes(amount).toString();
    }

    public String minusSeconds(String date,int amount) {
        return DateTime.of(date).minusSeconds(amount).toString();
    }
}
