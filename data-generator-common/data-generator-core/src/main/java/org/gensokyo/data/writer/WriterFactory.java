/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.writer;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.gensokyo.data.util.TypeKit;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * 写入器工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@RequiredArgsConstructor
public class WriterFactory {
    private final ApplicationContext ctx;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public @NonNull <S extends WriteStageVO, T extends WriterVO> Writer<S, T> newInstance(T wvo) {
        Writer<S, T> writer = null;
        Map<String, Writer> services = ctx.getBeansOfType(Writer.class);

        for (Writer<?, ?> service : services.values()) {
            if (TypeKit.isMatchingType(Writer.class, service, WriteStageVO.class, wvo.getClass())) {
                writer = (Writer<S, T>) service;
            }
        }

        Assert.notNull(writer, "未找到类型为 " + wvo.getType() + " 的数据写入器类");
        return writer;
    }

}
