/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.stage;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.model.vo.scripter.ScriptVO;

import java.io.Serializable;

/**
 * 参数配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Getter
@Setter
public class ParamVO implements Serializable {

    /**
     * 参数名称
     */
    private String name;

    /**
     * 脚本配置
     */
    private ScriptVO language;
}
