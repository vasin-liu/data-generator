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

/**
 * JSON写入器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/9/19 , Version 1.0.0
 */
@Configuration
public class JsonWriterConfig {

    @Bean
    @ConditionalOnMissingBean(JsonWriter.class)
    public <S extends WriteStageVO, T extends JsonWriterVO> JsonWriter<S, T> csvWriter() {
        return new JsonWriter<>();
    }
}
