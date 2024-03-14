/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.context;

import org.gensokyo.data.po.*;
import org.gensokyo.data.po.reader.ReaderPO;
import org.gensokyo.data.po.stage.ReadStagePO;

/**
 * 读取器上下文
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public record ReaderContext<T extends ReaderPO>(TemplatePO template,
                                                FieldPO field,
                                                ReadStagePO stage,
                                                T reader) {

    public static <T extends ReaderPO> ReaderContext<T> from(StageContext<ReadStagePO> ctx, T reader) {
        return new ReaderContext<>(ctx.template(), ctx.field(), ctx.stage(), reader);
    }

}
