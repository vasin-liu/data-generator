/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po.condition;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.po.ScriptPO;

import java.io.Serializable;

/**
 * 其他配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/27 , Version 1.0.0
 */
@Getter
@Setter
public class Otherwise implements Serializable {

    /**
     * 执行脚本配置
     */
    private ScriptPO then;
}
