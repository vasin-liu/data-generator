/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.context;

import org.gensokyo.data.po.FieldPO;
import org.gensokyo.data.po.TablePO;
import org.gensokyo.data.po.TemplatePO;

import java.io.Serializable;

/**
 * 字段生成上下文对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public record FieldContext(TemplatePO template,
                           FieldPO field) implements Serializable {

    public static FieldContext from(TemplateContext ctx, FieldPO field) {
        return new FieldContext(ctx.template(), field);
    }
}
