/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.context;

import org.gensokyo.data.po.*;
import org.gensokyo.data.po.stage.WriteStagePO;
import org.gensokyo.data.po.writer.WriterPO;

/**
 * 写入器上下文
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public record WriterContext<T extends WriterPO>(TemplatePO template,
                                                FieldPO field,
                                                T writer) {

    public static <T extends WriterPO> WriterContext<T> from(StageContext<WriteStagePO> ctx, T writer) {
        return new WriterContext<>(ctx.template(), ctx.field(), writer);
    }
}
