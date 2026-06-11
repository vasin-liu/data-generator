/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.writer;

import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * 数据库写入器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/25 , Version 1.0.0
 */
@Configuration
public class DbWriterConfig {

    @Bean
    @ConditionalOnMissingBean(JdbcWriter.class)
    public <S extends WriteStageVO, T extends JdbcWriterVO> JdbcWriter<S, T> jdbcWriter(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new JdbcWriter<>(namedParameterJdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(ClickHouseWriter.class)
    public <S extends WriteStageVO, T extends ClickHouseWriterVO> ClickHouseWriter<S, T> clickHouseWriter(JdbcTemplate jdbcTemplate) {
        return new ClickHouseWriter<>(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(MySQLWriter.class)
    public <S extends WriteStageVO, T extends MySQLWriterVO> MySQLWriter<S, T> mysqlWriter(JdbcTemplate jdbcTemplate) {
        return new MySQLWriter<>(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(PostgresWriter.class)
    public <S extends WriteStageVO, T extends PostgresWriterVO> PostgresWriter<S, T> postgresWriter(JdbcTemplate jdbcTemplate) {
        return new PostgresWriter<>(jdbcTemplate);
    }
}
