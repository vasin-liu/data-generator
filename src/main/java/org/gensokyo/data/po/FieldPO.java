/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.FieldType;
import org.springframework.core.convert.converter.Converter;

import java.io.Serializable;
import java.util.List;

/**
 * 字段配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Setter
@Getter
public class FieldPO implements Serializable {
    private String name;
    private FieldType type;

    private Class<? extends Converter<Object, ?>> converter;
    private ScriptPO preScript;
    private ScriptPO postScript;
    private List<ReaderPO> readers;
    private List<String> dependsOn;
    private ResultMapperPO resultMapper;

    public FieldPO() {
    }

    public FieldPO(String name, List<String> dependsOn) {
        this.name = name;
        this.dependsOn = dependsOn;
    }
}
