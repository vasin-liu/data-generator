/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.write;

import org.gensokyo.data.po.WriteStagePO;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 写入器抽象类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public class AbstractWriter implements Writer {
    protected final WriteStagePO wpo;

    protected AbstractWriter(final WriteStagePO wpo) {
        this.wpo = Objects.requireNonNull(wpo);
    }

    @Override
    public long write(List<Map<String, Object>> dataset) {
        return 0L;
    }
}
