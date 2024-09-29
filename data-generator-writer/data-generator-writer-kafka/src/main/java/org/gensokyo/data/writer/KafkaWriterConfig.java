/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.writer;

import org.gensokyo.boot.kafka.support.MultipleKafkaTemplate;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据库写入器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/25 , Version 1.0.0
 */
@Configuration
public class KafkaWriterConfig {

    @Bean
    @ConditionalOnMissingBean(KafkaWriter.class)
    public <S extends WriteStageVO, T extends KafkaWriterVO> KafkaWriter<S, T> multipleKafkaWriter(MultipleKafkaTemplate multipleKafkaTemplate) {
        return new KafkaWriter<>(multipleKafkaTemplate);
    }

}
