/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po.writer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.WriterType;

import java.io.Serializable;

/**
 * 数据写入器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        // 反序列化时，如果没有匹配到子类，则使用默认实现类
        defaultImpl = ConsoleWriterPO.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClickHouseWriterPO.class, names = {"CLICKHOUSE", "clickhouse"}),
        @JsonSubTypes.Type(value = ConsoleWriterPO.class, names = {"CONSOLE", "console"}),
        @JsonSubTypes.Type(value = ElasticsearchWriterPO.class, names = {"ELASTICSEARCH", "elasticsearch"}),
        @JsonSubTypes.Type(value = JdbcWriterPO.class, names = {"JDBC", "jdbc"}),
        @JsonSubTypes.Type(value = KafkaWriterPO.class, names = {"KAFKA", "kafka"}),
        @JsonSubTypes.Type(value = MySQLWriterPO.class, names = {"MYSQL", "mysql"}),
        @JsonSubTypes.Type(value = PostgresWriterPO.class, names = {"POSTGRES", "postgres"}),
})
public class WriterPO implements Serializable {
    /**
     * 写入器类型
     */
    private WriterType type;

    /**
     * 数据源编号
     */
    private String dataSourceId;
    /**
     * 写入目标对象
     */
    private String target;
    /**
     * 写入模板
     */
    private String template;
}
