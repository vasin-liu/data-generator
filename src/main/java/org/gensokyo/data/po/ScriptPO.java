/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.ScriptType;

import java.io.Serializable;

/**
 * 脚本配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/27 , Version 1.0.0
 */
@Getter
@Setter
public class ScriptPO implements Serializable {

    /**
     * 脚本类型
     */
    private ScriptType scriptType = ScriptType.SPEL;

    /**
     * 脚本内容
     */
    private String content;
}
