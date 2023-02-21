/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.reader;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.ReaderPO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.factory.ScriptFactory;
import org.gensokyo.data.generator.util.DatasetKit;

import java.util.List;
import java.util.Objects;

/**
 * 数据读取器抽象类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/13 , Version 1.0.0
 */
@Slf4j
public abstract class AbstractReader implements Reader {

    protected final ReaderPO rpo;
    protected final ScriptFactory scriptFactory;

    protected AbstractReader(final ReaderPO rpo, final ScriptFactory scriptFactory) {
        this.rpo = Objects.requireNonNull(rpo);
        this.scriptFactory = Objects.requireNonNull(scriptFactory);
    }

    protected List<Object> evalScript(Context ctx, List<Object> data) {
        try (var script = scriptFactory.newInstance(rpo.getPostScript(), ctx)) {
            if (Objects.nonNull(script)) {
                var re = script.eval(data);
                return DatasetKit.toList(re);
            }
        } catch (Exception e) {
            log.error("Reader [{}] 执行脚本 [{}] 出现异常", rpo.getDataSetId(), rpo.getPostScript().getContent());
            throw new DataGeneratorException(e);
        }

        return data;
    }
}
