/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.writer;

import org.gensokyo.data.generator.domain.WriterPO;

import java.util.Objects;

/**
 * 数据写入器抽象类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/13 , Version 1.0.0
 */
public abstract class AbstractWriter implements Writer {

    protected final WriterPO wpo;

    protected AbstractWriter(final WriterPO wpo) {
        this.wpo = Objects.requireNonNull(wpo);
    }
}
