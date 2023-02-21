/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 元数据信息
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Component
@ConfigurationProperties(prefix = DataGeneratorProperties.PREFIX)
@Setter
@Getter
public class DataGeneratorProperties {

    /**
     * 组件配置的前缀
     */
    public static final String PREFIX = "pci.data.generator";

    private String[] metaFolders = new String[]{"classpath*:/template/**/*.yaml"};

    private Integer corePoolSize = 3;
    private Integer maxPoolSize = 8;
    private Integer queueCapacity = 8;

    private Integer metaCacheMaximumSize = 100;

}
