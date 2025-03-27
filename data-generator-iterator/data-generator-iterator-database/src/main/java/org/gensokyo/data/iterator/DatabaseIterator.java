/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataSet;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.database.dialect.DialectFactory;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.script.ScriptFactory;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.collect.MapKit;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据库迭代器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/2 , Version 1.0.0
 */
@Slf4j
public class DatabaseIterator<T extends DatabaseIteratorVO> extends AbstractIterator<T> {
    private final String dsId;
    private final String sql;
    private final AtomicLong pageIndex;
    private final int pageSize;
    private final AtomicLong pages = new AtomicLong(0L);
    private final long maxRows;
    private final BlockingQueue<Value> queue;
    private final AtomicLong total = new AtomicLong(-1L);

    private DynamicRoutingDataSource dynamicRoutingDataSource;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private ScriptFactory scriptFactory;

    @Autowired
    public void setDynamicRoutingDataSource(DynamicRoutingDataSource dynamicRoutingDataSource) {
        this.dynamicRoutingDataSource = dynamicRoutingDataSource;
    }

    @Autowired
    public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Autowired
    public void setScriptFactory(ScriptFactory scriptFactory) {
        this.scriptFactory = scriptFactory;
    }

    protected DatabaseIterator(IteratorContext<T> ctx) {
        super(ctx);
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.iterator(), "迭代器配置不能为空");
        var it = ctx.iterator();
        Assert.isTrue(it.getPageIndex() > 0 && it.getPageIndex() < Integer.MAX_VALUE,
                "数据库迭代器配置的当前页码数值必须大于0");
        Assert.isTrue(it.getPageSize() > 0 && it.getPageSize() < Integer.MAX_VALUE,
                "数据库迭代器配置的每页记录数值必须大于0");
        Assert.hasLength(it.getDataSourceId(), "数据库迭代器配置的数据源ID不能为空");
        Assert.hasLength(it.getSql(), "数据库迭代器配置的分页查询SQL不能为空");
        Assert.isTrue((it.getMaxRows() > 0 && it.getMaxRows() < Integer.MAX_VALUE) || -1 == it.getMaxRows(),
                "数字迭代器配置的最大记录数值必须大于0或者等于-1");
        this.dsId = it.getDataSourceId();
        this.sql = it.getSql();
        this.pageIndex = new AtomicLong(it.getPageIndex());
        this.pageSize = it.getPageSize();
        this.maxRows = it.getMaxRows();
        this.queue = new LinkedBlockingQueue<>(pageSize);
    }

    @Override
    public boolean hasNext() {
        if (0 == total.get()) {
            return false;
        }
        if (-1 == total.get()
                || (queue.isEmpty() && pageIndex.get() <= pages.get())) {
            log.info("当前页码数：{}", pageIndex.get());
            fetch();
        }
        return !queue.isEmpty();
    }

    @Override
    public Value next() {
        if (hasNext()) {
            return queue.poll();
        }

        throw new IllegalStateException("迭代器已经到达最大值");
    }

    private void fetch() {
        try {
            // 设置当前数据源，如果需要的话
            DynamicDataSourceContextHolder.push(dsId);

            var params = toSqlParams(ctx);

            //第一次查询总数
            if (-1 == total.get()) {
                initTotal(params);
                initPages();
            }

            if (0 == total.get()) {
                return;
            }

            // 执行原生 SQL 查询
            var ds = dynamicRoutingDataSource.determineDataSource();
            var re = namedParameterJdbcTemplate.queryForList(SqlKit.toPageSql(ds, sql, getLimit(), getOffset()), params);
            Assert.notEmpty(re, "迭代器查询数据源编号 %s 的数据库查询结果为空，查询语句为：%s，查询参数为：%s",
                    dsId, sql, JsonKit.write(params));

            var r = re.stream().map(DatasetKit::toValue).toList();
            if (CollectKit.isNotEmpty(r)) {
                queue.addAll(r);
            }
            pageIndex.incrementAndGet();
        } catch (Exception e) {
            throw new DataGeneratorException("读取数据库数据出现异常", e);
        } finally {
            DynamicDataSourceContextHolder.clear();
            DialectFactory.clearDbType();
        }
    }

    private void initTotal(Map<String, Object> params) {
        var num = namedParameterJdbcTemplate.queryForObject(SqlKit.toCountSql(sql), params, Long.class);
        if (Objects.isNull(num) || num <= 0) {
            total.set(0L);
        } else {
            if (maxRows > 0) {
                total.set(Math.min(num, maxRows));
            } else {
                total.set(num);
            }
        }
    }

    private void initPages() {
        long p = total.get() / pageSize;
        if (total.get() % pageSize != 0) {
            p++;
        }
        pages.set(p);
    }

    /**
     * 解析SQL参数
     *
     * @param ctx 迭代器器上下文对象
     * @return SQL参数
     */
    private Map<String, Object> toSqlParams(final IteratorContext<T> ctx) {
        var it = ctx.iterator();
        Map<String, Object> params = MapKit.newHashMap();
        if (CollectKit.isNotEmpty(it.getParams())) {
            it.getParams().forEach(p -> {
                var sspo = new ScriptStageVO();
                sspo.setType(Const.StageType.SCRIPT);
                sspo.setLanguage(p.getLanguage());
                var sctx = new StageContext<>(ctx.template(), null, sspo);
                var spo = sctx.stage().getLanguage();
                var script = scriptFactory.newInstance(spo);
                if (Objects.nonNull(script)) {
                    var input = SingleValue.of(DatasetKit.toObject(ctx.dataset()));
                    var dataset = DataSet.getOrCreate(ctx.template().getId(), ctx.template().getInstanceId());
                    var v = script.eval(sctx, spo, input, dataset).get();
                    params.put(p.getName(), v);
                }

            });
        }
        return params;
    }


    private Long getOffset() {
        long current = pageIndex.get();
        if (current <= 1L) {
            return 0L;
        }
        return Math.max((current - 1) * pageSize, 0L);
    }

    private Long getLimit() {
        if (pageSize * pageIndex.get() <= total.get()) {
            return (long) pageSize;
        } else {
            return total.get() - (pageSize * (pageIndex.get() - 1));
        }
    }
}
