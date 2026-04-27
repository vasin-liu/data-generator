/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import org.gensokyo.boot.elasticsearch.support.MultipleElasticsearchRestClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * ES读取器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/25 , Version 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(MultipleElasticsearchRestClient.class)
public class EsReaderConfig {
}
