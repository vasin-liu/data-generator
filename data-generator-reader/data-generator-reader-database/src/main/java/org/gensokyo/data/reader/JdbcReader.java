/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataSet;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.script.ScriptFactory;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.collect.MapKit;
import org.gensokyo.kit.json.JsonKit;
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
public class JdbcReader<S extends ReadStageVO, T extends JdbcReaderVO> implements Reader<S, T> {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ScriptFactory scriptFactory;

    @SuppressWarnings("SqlSourceToSinkFlow")
    @Override
    public Value read(final StageContext<S> ctx, final T rvo, final Value input) {
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(rvo.getDataSourceId()));
            var params = toSqlParams(ctx, input);
            var re = namedParameterJdbcTemplate.queryForList(Objects.requireNonNull(rvo.getContent()), params);
            Assert.notEmpty(re, "字段 %s 的数据读取器查询数据源编号 %s 的数据库查询结果为空，查询语句为：%s，查询参数为：%s",
                    ctx.field().getName(), rvo.getDataSourceId(), rvo.getContent(), JsonKit.write(params));
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
    private Map<String, Object> toSqlParams(final StageContext<S> ctx, final Value input) {
        var stage = ctx.stage();
        Map<String, Object> params = MapKit.newHashMap();
        if (CollectKit.isNotEmpty(stage.getParams())) {
            stage.getParams().forEach(p -> {
                var sspo = new ScriptStageVO();
                sspo.setType(Const.StageType.SCRIPT);
                sspo.setLanguage(p.getLanguage());
                var sctx = new StageContext<>(ctx.template(), ctx.field(), sspo);
                var spo = sctx.stage().getLanguage();
                var script = scriptFactory.newInstance(spo);
                if (Objects.nonNull(script)) {
                    var v = script.eval(sctx, spo, input, DataSet.getOrCreate(ctx.template().getId())).get();
                    params.put(p.getName(), v);
                }

            });
        }
        return params;
    }
}
