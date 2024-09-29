/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.condition;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.model.vo.scripter.ScriptVO;

import java.io.Serializable;

/**
 * 条件配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/27 , Version 1.0.0
 */
@Getter
@Setter
public class WhenVO implements Serializable {

    /**
     * 判断条件脚本配置
     */
    private ScriptVO when;

    /**
     * 执行脚本配置
     */
    private ScriptVO then;
}
