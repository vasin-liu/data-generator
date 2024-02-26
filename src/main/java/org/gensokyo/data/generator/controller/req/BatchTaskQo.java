/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.controller.req;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量任务请求实体
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/5/8 , Version 1.0.0
 */
@Data
public class BatchTaskQo implements Serializable {

    private List<String> templateNames;
}
