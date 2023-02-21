/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.domain;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 结果映射配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Setter
@Getter
public class ResultMapperPO implements Serializable {
    private List<Object> defaultDataset;
    private List<MapperPO> mappers;
}
