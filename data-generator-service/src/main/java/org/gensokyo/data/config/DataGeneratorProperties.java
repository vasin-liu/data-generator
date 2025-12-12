/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 元数据信息
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
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

    /**
     * 忽略文件前缀集合
     */
    private String[] ignorePrefix = new String[]{"___", "!"};

    /**
     * 核心线程数
     */
    private Integer corePoolSize = 50;
    /**
     * 最大线程数
     */
    private Integer maxPoolSize = 100;
    /**
     * 队列任务数
     */
    private Integer queueCapacity = 100;
    /**
     * 元数据最大缓存数量
     */
    private Integer metaCacheMaximumSize = 100;

}

