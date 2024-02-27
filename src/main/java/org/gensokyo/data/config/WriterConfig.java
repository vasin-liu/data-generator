/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.boot.elasticsearch.support.MultipleElasticsearchRestClient;
import org.gensokyo.boot.kafka.support.MultipleKafkaTemplate;
import org.gensokyo.data.write.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * 写入器器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Configuration
public class WriterConfig {

    @Bean
    @ConditionalOnMissingBean(ConsoleWriter.class)
    public ConsoleWriter consoleWriter() {
        return new ConsoleWriter();
    }

    @Bean
    @ConditionalOnMissingBean(JdbcWriter.class)
    public JdbcWriter jdbcWriter(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new JdbcWriter(namedParameterJdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(ClickHouseWriter.class)
    public ClickHouseWriter clickHouseWriter(JdbcTemplate jdbcTemplate) {
        return new ClickHouseWriter(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(ElasticsearchWriter.class)
    public ElasticsearchWriter elasticsearchWriter(MultipleElasticsearchRestClient multipleElasticsearchRestClient) {
        return new ElasticsearchWriter(multipleElasticsearchRestClient);
    }

    @Bean
    @ConditionalOnMissingBean(KafkaWriter.class)
    public KafkaWriter multipleKafkaWriter(MultipleKafkaTemplate multipleKafkaTemplate) {
        return new KafkaWriter(multipleKafkaTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(MySQLWriter.class)
    public MySQLWriter mysqlWriter(JdbcTemplate jdbcTemplate) {
        return new MySQLWriter(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(PostgresWriter.class)
    public PostgresWriter postgresWriter(JdbcTemplate jdbcTemplate) {
        return new PostgresWriter(jdbcTemplate);
    }
}
