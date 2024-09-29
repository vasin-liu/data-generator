/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.scripter.ScriptVO;

/**
 * SPEL脚本配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/10 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(ScriptVO.class)
@JsonSubType(value = "SPEL")
public class SpelScriptVO extends ScriptVO {
}
