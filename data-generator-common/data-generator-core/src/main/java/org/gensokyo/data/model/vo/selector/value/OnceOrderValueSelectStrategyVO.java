/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.selector.value;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.json.JsonSubType;

/**
 * 一次性顺序选择策略配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/10 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(ValueSelectStrategyVO.class)
@JsonSubType(value = Const.ValueSelectStrategyType.ONCE_ORDER)
public class OnceOrderValueSelectStrategyVO extends ValueSelectStrategyVO {
}
