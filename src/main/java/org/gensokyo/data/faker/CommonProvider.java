/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.faker;

import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import org.gensokyo.data.util.RandomKit;

import java.util.List;

/**
 * 常用数据提供者
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/2 , Version 1.0.0
 */
public class CommonProvider extends AbstractProvider<BaseProviders> {

    public CommonProvider(BaseProviders faker) {
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
}
