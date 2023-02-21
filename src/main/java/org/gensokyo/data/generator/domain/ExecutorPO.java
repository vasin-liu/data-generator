/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 线程池执行器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Setter
@Getter
public class ExecutorPO implements Serializable {

    @Builder.Default
    private int coreSize = 8;
    @Builder.Default
    private int maxSize = 16;
    @Builder.Default
    private int queueCapacity = 16;
}
