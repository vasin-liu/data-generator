/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.context;

import org.gensokyo.data.po.*;

/**
 * 写入器上下文
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public record WriterContext(TemplatePO template,
                            FieldPO field,
                            WriteStagePO writer) {

    public static WriterContext from(StageContext<WriteStagePO> ctx, WriteStagePO writer) {
        return new WriterContext(ctx.template(), ctx.field(), writer);
    }
}
