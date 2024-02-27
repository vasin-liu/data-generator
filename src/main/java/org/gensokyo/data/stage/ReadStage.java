/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.Context;
import org.gensokyo.data.read.Reader;
import org.gensokyo.data.value.Value;

/**
 * 数据读取阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@RequiredArgsConstructor
public class ReadStage implements Stage {
    private final Context ctx;
    private final Reader reader;

    @Override
    public Value execute(Value input) {
        return reader.read(ctx);
    }
}
