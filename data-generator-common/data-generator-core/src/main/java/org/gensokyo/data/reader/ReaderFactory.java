/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.util.TypeKit;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * 数据读取器工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ReaderFactory {

    private final ApplicationContext ctx;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public @NonNull <S extends ReadStageVO, T extends ReaderVO> Reader<S, T> newInstance(final T rvo) {
        Reader<S, T> reader = null;
        Map<String, Reader> services = ctx.getBeansOfType(Reader.class);

        for (Reader<?, ?> service : services.values()) {
            if (TypeKit.isMatchingType(Reader.class, service, ReadStageVO.class, rvo.getClass())) {
                reader = (Reader<S, T>) service;
            }
        }

        Assert.notNull(reader, "未找到类型为 " + rvo.getType() + " 的数据读取器类");
        return reader;
    }
}
