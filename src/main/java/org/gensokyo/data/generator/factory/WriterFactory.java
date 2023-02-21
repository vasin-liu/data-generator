/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.factory;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.generator.domain.WriterPO;
import org.gensokyo.data.generator.writer.*;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.NonNull;

import java.util.Objects;

/**
 * 数据写入器工厂
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@RequiredArgsConstructor
public class WriterFactory implements Factory {
    private final AutowireCapableBeanFactory beanFactory;

    public @NonNull Writer newInstance(WriterPO wpo) {
        var writer = switch (Objects.requireNonNull(wpo).getType()) {
            case JDBC -> new JdbcWriter(wpo);
            case MYSQL -> new MySQLWriter(wpo);
            case POSTGRES -> new PostgresWriter(wpo);
            case CLICKHOUSE -> new ClickHouseWriter(wpo);
            case KAFKA -> new KafkaWriter(wpo);
            case ELASTICSEARCH -> new ElasticsearchWriter(wpo);
        };
        beanFactory.autowireBean(writer);
        return writer;
    }
}
