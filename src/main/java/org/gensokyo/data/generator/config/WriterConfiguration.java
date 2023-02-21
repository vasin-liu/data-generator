/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.config;

import com.alibaba.druid.wall.WallConfig;
import org.gensokyo.data.generator.factory.WriterFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 写入器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/12 , Version 1.0.0
 */
@Configuration
public class WriterConfiguration {

    @Bean
    @ConditionalOnMissingBean(WriterFactory.class)
    public WriterFactory writerFactory(AutowireCapableBeanFactory beanFactory) {
        return new WriterFactory(beanFactory);
    }

    @Bean
    //@ConfigurationProperties("spring.datasource.druid.wall.config")
    public WallConfig wallConfig() {
        WallConfig config = new WallConfig();
        // 运行一次执行多条SQL
        config.setMultiStatementAllow(true);
        // 防火墙拦截loadfile
        config.setDir("META-INF/druid/wall/mysql");
        //允许非基本语句的其他语句
        config.setNoneBaseStatementAllow(true);
        return config;
    }
}
