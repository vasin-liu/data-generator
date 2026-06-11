/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.selector.reader;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.json.JsonSubType;

/**
 * 权重读取器选择策略配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/11 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(ReaderSelectStrategyVO.class)
@JsonSubType(value = Const.ReaderSelectStrategyType.WEIGHT)
public class WeightReaderSelectStrategyVO extends ReaderSelectStrategyVO {
}
