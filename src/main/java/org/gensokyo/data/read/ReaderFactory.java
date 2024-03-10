/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.po.ReadStagePO;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * 数据读取器工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@RequiredArgsConstructor
public class ReaderFactory {

    private final AutowireCapableBeanFactory beanFactory;

    public @NonNull Reader newInstance(final ReadStagePO.ReaderPO rpo) {
        var reader = switch (rpo.getType()) {
            case JDBC -> beanFactory.getBean(JdbcReader.class);
            case SPEL -> beanFactory.getBean(SpelReader.class);
            case DIRECT_SPEL -> beanFactory.getBean(DirectSpelReader.class);
            case CONSTANT -> beanFactory.getBean(ConstantReader.class);
            default -> null;
        };
        Assert.notNull(reader, "未找到类型为 " + rpo.getType() + " 的数据读取器类");
        return reader;
    }
}
