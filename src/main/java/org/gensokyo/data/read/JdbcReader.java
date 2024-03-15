/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataCache;
import org.gensokyo.data.constant.ScriptType;
import org.gensokyo.data.constant.StageType;
import org.gensokyo.data.context.ReaderContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.reader.JdbcReaderPO;
import org.gensokyo.data.po.stage.ScriptStagePO;
import org.gensokyo.data.script.ScriptFactory;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.collect.MapKit;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;
import java.util.Objects;

/**
 * Jdbc数据读取类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcReader<T extends JdbcReaderPO> implements Reader<T> {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ScriptFactory scriptFactory;

    @SuppressWarnings("SqlSourceToSinkFlow")
    @Override
    public Value read(final ReaderContext<T> ctx, final Value input) {
        try {
            var rpo = ctx.reader();
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(rpo.getDataSourceId()));
            var re = namedParameterJdbcTemplate.queryForList(Objects.requireNonNull(rpo.getContent()),
                    toSqlParams(ctx, input));
            Assert.notEmpty(re, "字段 %s 的数据读取器查询数据源编号 %s 的数据库查询结果为空，查询语句为：%s",
                    ctx.field().getName(), ctx.reader().getDataSourceId(), ctx.reader().getContent());
            return DatasetKit.extractCollection(re);
        } catch (Exception e) {
            throw new DataGeneratorException("读取数据库数据出现异常", e);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    /**
     * 解析SQL参数
     *
     * @param ctx   读取器上下文对象
     * @param input 输入值
     * @return SQL参数
     */
    private Map<String, Object> toSqlParams(final ReaderContext<T> ctx, final Value input) {
        Map<String, Object> params = MapKit.newHashMap();
        var rpo = ctx.stage();
        if (CollectKit.isNotEmpty(rpo.getParams())) {
            rpo.getParams().forEach(p -> {
                switch (p.getType()) {
                    case CONSTANT -> params.put(p.getName(), p.getContent());
                    case SPEL -> params.put(p.getName(), eval(ctx, createSpo(ScriptType.SPEL, p.getContent()), input));
                    case JAVASCRIPT ->
                            params.put(p.getName(), eval(ctx, createSpo(ScriptType.JAVASCRIPT, p.getContent()), input));
                }
            });
        }
        return params;
    }

    private ScriptStagePO createSpo(ScriptType scriptType, Object content) {
        var spo = new ScriptStagePO();
        spo.setType(StageType.SCRIPT);
        spo.setScriptType(scriptType);
        spo.setContent((String) content);
        return spo;
    }

    private Object eval(final ReaderContext<T> ctx, ScriptStagePO spo, Value input) {
        var script = scriptFactory.newInstance(spo);
        if (Objects.nonNull(script)) {
            return script.eval(spo, input, DataCache.getOrCreate(ctx.template().getName())).get();
        }
        return null;
    }
}
