/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.faker;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.script.vars.Variable;

import java.util.Objects;

/**
 * DataFaker变量
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/11 , Version 1.0.0
 */
public class DataFakerVariable implements Variable {

    private final DataFaker dataFaker;

    private DataFakerVariable(DataFaker dataFaker) {
        this.dataFaker = Objects.requireNonNull(dataFaker);
    }

    public static DataFakerVariable of(DataFaker dataFaker) {
        return new DataFakerVariable(dataFaker);
    }

    @Override
    public String name() {
        return Const.SCRIPT_VAR_FAKER;
    }

    @Override
    public Object value() {
        return dataFaker;
    }
}
