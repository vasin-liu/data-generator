/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.processor;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.generator.Destroyable;
import org.gensokyo.data.generator.dataset.Dataset;
import org.gensokyo.data.generator.dataset.ReadableDataset;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.FieldPO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.factory.ReaderFactory;
import org.gensokyo.data.generator.factory.ScriptFactory;
import org.gensokyo.data.generator.util.DatasetKit;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * 字段处理器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/9 , Version 1.0.0
 */
@Slf4j
public class FieldProcessor implements Processor<FieldPO, Dataset>, Destroyable {
    private ThreadPoolTaskExecutor executor;
    private ReaderFactory readerFactory;
    private ScriptFactory scriptFactory;
    private Context ctx;

    public FieldProcessor(ThreadPoolTaskExecutor executor, ReaderFactory readerFactory,
                          ScriptFactory scriptFactory, Context ctx) {
        this.executor = executor;
        this.readerFactory = readerFactory;
        this.scriptFactory = scriptFactory;
        this.ctx = ctx;
    }

    @Override
    public Dataset handle(FieldPO fpo) {
        var readerDataset = new ConcurrentHashMap<String, Dataset>();
        var futures = new ArrayList<CompletableFuture<Dataset>>();
        for (var rpo : fpo.getReaders()) {
            var cf = supplyAsync(() -> readerFactory.newInstance(rpo, scriptFactory).read(ctx), executor)
                    .whenComplete((r, ex) -> readerDataset.put(rpo.getDataSetId(), r))
                    .exceptionally(ex -> {
                        log.error("字段 [{}] 的Reader [{}] 处理字段数据集出现异常：", fpo.getName(), rpo.getDataSetId());
                        throw new DataGeneratorException("处理字段数据集出现异常", ex);
                    });
            futures.add(cf);
        }
        allOf(futures.toArray(new CompletableFuture[]{})).join();
        return combine(ctx, fpo, readerDataset);
    }

    private Dataset combine(Context ctx, FieldPO field, Map<String, Dataset> dataset) {
        if (CollectionUtils.isEmpty(dataset)) {
            return ReadableDataset.empty();
        }
        try (var script = scriptFactory.newInstance(field.getPreScript(), ctx)) {
            //字段中所有Reader的数据集当做参数传入脚本
            if (Objects.nonNull(script)) {
                //转换数据集
                Object evalResult = script.eval(Maps.transformValues(dataset, Dataset::fetch));
                return ReadableDataset.of(DatasetKit.toList(evalResult));
            }
        } catch (Exception e) {
            log.error("字段 [{}] 执行前置脚本 [{}] 出现异常", field.getName(), field.getPreScript().getContent());
            throw new DataGeneratorException(e);
        }

        //无脚本处理且有多个数据集时，默认选取第一个不为空的数据集
        return dataset.values().stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ReadableDataset.empty());
    }

    @Override
    public void destroy() {
        this.ctx.global().clear();
        //set null
        this.executor = null;
        this.readerFactory = null;
        this.scriptFactory = null;
        this.ctx = null;
    }
}
