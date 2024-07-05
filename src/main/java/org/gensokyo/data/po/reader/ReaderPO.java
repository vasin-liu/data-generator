/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po.reader;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.ReaderType;

import java.io.Serializable;

/**
 * 数据读取器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        // 反序列化时，如果没有匹配到子类，则使用默认实现类
        defaultImpl = ConstantReaderPO.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConstantReaderPO.class, names = {"CONSTANT", "constant"}),
        @JsonSubTypes.Type(value = SpelReaderPO.class, names = {"SPEL", "spel"}),
        @JsonSubTypes.Type(value = JdbcReaderPO.class, names = {"JDBC", "jdbc"}),
        @JsonSubTypes.Type(value = AiReaderPO.class, names = {"AI", "ai"}),
})
public class ReaderPO implements Serializable {

    /**
     * 读取器权重，用于权重选择策略
     */
    private int weight = 100;

    /**
     * 数据集读取类型
     */
    private ReaderType type;
}
