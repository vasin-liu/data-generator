/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.write;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.po.writer.WriterPO;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * 写入器工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@RequiredArgsConstructor
public class WriterFactory {
    private final AutowireCapableBeanFactory beanFactory;

    public @NonNull <T extends WriterPO> Writer<T> newInstance(T wpo) {
        Writer<T> writer = switch (Objects.requireNonNull(wpo).getWriterType()) {
            case CONSOLE -> beanFactory.getBean(ConsoleWriter.class);
            case JDBC -> beanFactory.getBean(JdbcWriter.class);
            case KAFKA -> beanFactory.getBean(KafkaWriter.class);
            case MYSQL -> beanFactory.getBean(MySQLWriter.class);
            case POSTGRES -> beanFactory.getBean(PostgresWriter.class);
            case ELASTICSEARCH -> beanFactory.getBean(ElasticsearchWriter.class);
            case CLICKHOUSE -> beanFactory.getBean(ClickHouseWriter.class);
        };
        Assert.notNull(writer, "未找到类型为 " + wpo.getWriterType() + " 的数据写入器类");
        return writer;
    }
}
