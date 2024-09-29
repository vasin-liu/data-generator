/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.scripter;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;

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
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME
        , include = JsonTypeInfo.As.EXISTING_PROPERTY
        , property = "type"
        , visible = true
        // 反序列化时，如果没有匹配到子类，则使用默认实现类
        , defaultImpl = PlainScriptVO.class
)
public class ScriptVO implements Serializable {

    /**
     * 脚本类型
     */
    private String type = Const.ScriptType.PLAIN;

    /**
     * 脚本内容
     */
    private String content;
}
