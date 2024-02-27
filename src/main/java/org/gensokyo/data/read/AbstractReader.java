/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read;

import org.gensokyo.data.Context;
import org.gensokyo.data.po.ReaderPO;
import org.gensokyo.data.value.Value;

import java.util.Objects;

/**
 * 数据读取抽象类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public class AbstractReader implements Reader {

    protected final ReaderPO rpo;

    protected AbstractReader(final ReaderPO rpo) {
        this.rpo = Objects.requireNonNull(rpo);
    }

    @Override
    public Value read(Context ctx) {
        return null;
    }
}
