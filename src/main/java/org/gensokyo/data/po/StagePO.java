/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.StageType;

import java.io.Serializable;

/**
 * 步骤配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        // 反序列化时，如果没有匹配到子类，则使用默认实现类，即WriteStageP，
        // 除了WriteStagePO，其他子类都需要在配置时指定type属性
        defaultImpl = WriteStagePO.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReadStagePO.class, names = {"READ", "read"}),
        @JsonSubTypes.Type(value = SelectStagePO.class, names = {"SELECT", "select"}),
        @JsonSubTypes.Type(value = ScriptStagePO.class, names = {"SCRIPT", "script"}),
        @JsonSubTypes.Type(value = ConvertStagePO.class, names = {"CONVERT", "convert"}),
        @JsonSubTypes.Type(value = WriteStagePO.class, names = {"WRITE", "write"})
})
public class StagePO implements Serializable {

    private StageType type;
}
