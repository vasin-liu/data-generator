/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.factory;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.generator.domain.ReaderPO;
import org.gensokyo.data.generator.faker.DataFaker;
import org.gensokyo.data.generator.reader.*;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.NonNull;

/**
 * 数据读取器工厂
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@RequiredArgsConstructor
public class ReaderFactory implements Factory {
    private final AutowireCapableBeanFactory beanFactory;
    private final DataFaker dataFaker;

    public @NonNull Reader newInstance(final ReaderPO rpo, final ScriptFactory scriptFactory) {
        var reader = switch (rpo.getType()) {
            case JDBC -> new JdbcReader(rpo, scriptFactory);
            case KAFKA -> new KafkaReader(rpo, scriptFactory);
            case ELASTICSEARCH -> new ElasticsearchReader(rpo, scriptFactory);
            case CONSTANT -> new ConstantReader(rpo, scriptFactory);
            case SPEL -> new SpelReader(rpo, scriptFactory, dataFaker);
            case DIRECT_SPEL -> new DirectSpelReader(rpo, scriptFactory, dataFaker);
        };
        beanFactory.autowireBean(reader);
        return reader;
    }
}
