/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.script.ScriptFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * 数据库读取器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/1 , Version 1.0.0
 */
@Configuration
public class DbReaderConfig {

    @Bean
    @ConditionalOnMissingBean(JdbcReader.class)
    public JdbcReader<ReadStageVO, JdbcReaderVO> jdbcReader(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                                            ScriptFactory scriptFactory) {
        return new JdbcReader<>(namedParameterJdbcTemplate, scriptFactory);
    }
}
