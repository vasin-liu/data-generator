/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.model.vo.reader.ConstantReaderVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.reader.ConstantReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 读取器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Configuration
public class ReaderConfig {

    @Bean
    @ConditionalOnMissingBean(ConstantReader.class)
    public ConstantReader<ReadStageVO, ConstantReaderVO> constantReader() {
        return new ConstantReader<>();
    }
}
