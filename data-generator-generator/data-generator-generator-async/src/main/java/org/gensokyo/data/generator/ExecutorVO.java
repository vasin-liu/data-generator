/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 线程池配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
@Setter
@Getter
public class ExecutorVO implements Serializable {

    private int coreSize = 8;
    private int maxSize = 16;
    private int queueCapacity = 8;
    private int keepAliveSeconds = 180;
    private boolean waitForJobsToCompleteOnShutdown = true;
    private int awaitTerminationSeconds = Integer.MAX_VALUE;
}
