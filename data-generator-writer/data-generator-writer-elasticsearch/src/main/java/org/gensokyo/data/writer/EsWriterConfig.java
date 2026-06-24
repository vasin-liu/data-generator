/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.writer;

import org.elasticsearch.client.RestClient;
import org.gensokyo.data.datasource.elasticsearch.DynamicElasticsearchClientRegistry;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 数据库写入器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/25 , Version 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass({DynamicElasticsearchClientRegistry.class, RestClient.class})
public class EsWriterConfig {

    @Bean
    @ConditionalOnMissingBean(ElasticsearchWriter.class)
    public <S extends WriteStageVO, T extends ElasticsearchWriterVO> ElasticsearchWriter<S, T> elasticsearchWriter(
            DynamicElasticsearchClientRegistry elasticsearchClientRegistry) {
        return new ElasticsearchWriter<>(elasticsearchClientRegistry);
    }

}
