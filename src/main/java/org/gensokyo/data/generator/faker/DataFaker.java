/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.faker;

import net.datafaker.Faker;
import net.datafaker.service.FakeValuesService;
import net.datafaker.service.FakerContext;
import net.datafaker.service.RandomService;

import java.util.Locale;
import java.util.Random;

/**
 * 自定义假数据生成器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/1 , Version 1.0.0
 */
public class DataFaker extends Faker {

    public DataFaker() {
        super();
    }

    public DataFaker(Locale locale) {
        super(locale);
    }

    public DataFaker(Random random) {
        super(random);
    }

    public DataFaker(Locale locale, Random random) {
        super(locale, random);
    }

    public DataFaker(Locale locale, RandomService randomService) {
        super(locale, randomService);
    }

    public DataFaker(FakeValuesService fakeValuesService, FakerContext context) {
        super(fakeValuesService, context);
    }

    /**
     * 中国车辆规则对象生成
     *
     * @return Vehicle
     */
    public VehicleProvider vehicleCN() {
        return getProvider(VehicleProvider.class, VehicleProvider::new, this);
    }

    public SnowflakeProvider snowflake() {
        return getProvider(SnowflakeProvider.class, SnowflakeProvider::new, this);
    }
}
