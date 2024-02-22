/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.generator.dataset.Dataset;
import org.gensokyo.data.generator.dataset.ReadableDataset;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.FieldPO;
import org.gensokyo.data.generator.domain.TemplatePO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.factory.*;
import org.gensokyo.data.generator.listener.DefaultGeneratorListener;
import org.gensokyo.data.generator.listener.GeneratorListener;
import org.gensokyo.data.generator.processor.FieldProcessor;
import org.gensokyo.data.generator.processor.TableProcessor;
import org.gensokyo.data.generator.util.DatetimeKit;
import org.gensokyo.data.generator.util.PageKit;
import org.gensokyo.kit.collect.CollectKit;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * 核心数据生成器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Slf4j
public class DefaultDataGenerator implements Generator<Boolean>, Destroyable {
    private ReaderFactory readerFactory;
    private WriterFactory writerFactory;
    private ScriptFactory scriptFactory;
    private TemplatePO template;
    private ThreadPoolTaskExecutor executor;
    //FieldName,DataSet
    private ConcurrentHashMap<String, Dataset> fieldDataset = new ConcurrentHashMap<>(16);
    private Context ctx;
    private List<FieldPO> orderedFields;
    private CountDownLatch dataReady = new CountDownLatch(2);
    private FieldProcessor fieldProcessor;
    private TableProcessor tableProcessor;
    private GeneratorListener listener = new DefaultGeneratorListener();

    public DefaultDataGenerator(ExecutorFactory executorFactory,
                                ReaderFactory readerFactory,
                                WriterFactory writerFactory,
                                ConverterFactory converterFactory,
                                ScriptFactory scriptFactory,
                                TemplatePO template) {
        //线程池工厂类
        //数据读取器工厂类
        this.readerFactory = Objects.requireNonNull(readerFactory);
        //数据写入器工厂类
        this.writerFactory = Objects.requireNonNull(writerFactory);
        //脚本工厂
        this.scriptFactory = Objects.requireNonNull(scriptFactory);
        //数据生成模板对象
        this.template = Objects.requireNonNull(template);
        //当前数据生成器的上下文对象
        this.ctx = new Context(template);
        //线程池
        this.executor = Objects.requireNonNull(executorFactory).newInstance(template);
        //根据依赖关系重排序
        orderedFields = sortFields();
        //字段处理器
        this.fieldProcessor = new FieldProcessor(executor, readerFactory, scriptFactory, ctx);
        //表处理器
        this.tableProcessor = new TableProcessor(Objects.requireNonNull(converterFactory), scriptFactory, ctx, fieldDataset);
    }

    private List<FieldPO> sortFields() {
        var fields = template.getTable().getFields();
        var dag = new DirectedAcyclicGraph<FieldPO, DefaultEdge>(DefaultEdge.class);
        var fieldMap = fields.stream().collect(Collectors.toMap(FieldPO::getName, field -> field));
        for (var field : fields) {
            dag.addVertex(field);
            if (CollectKit.isNotEmpty(field.getDependsOn())) {
                for (var fn : field.getDependsOn()) {
                    var df = fieldMap.get(fn);
                    if (Objects.nonNull(df)) {
                        dag.addVertex(df);
                        dag.addEdge(df, field);
                    } else {
                        log.error("当前字段 [{}] 依赖的字段 [{}] 未在当前模板 [{}] 的配置表中找到，请检查配置是否正确",
                                field.getName(), fn, template.getName());
                    }
                }
            }
        }
        var it = new TopologicalOrderIterator<>(dag);
        var ordered = new ArrayList<FieldPO>();
        it.forEachRemaining(ordered::add);
        return ordered;
    }

    private void loadGlobalDataset() {
        log.info("开始加载任务全局数据");
        if (CollectKit.isEmpty(template.getGlobal().getReaders())) {
            //全局数据准备就绪
            dataReady.countDown();
            return;
        }
        var stopWatch = new StopWatch();
        stopWatch.start();
        for (var rpo : template.getGlobal().getReaders()) {
            var dataset = readerFactory.newInstance(rpo, scriptFactory).read(ctx);
            ctx.global(rpo.getDataSetId(), dataset.fetch());
        }
        dataReady.countDown();
        stopWatch.stop();
        log.info("当前任务全局数据加载完成，总计耗时：{} ", DatetimeKit.humanized(stopWatch.getTotalTimeMillis()));
    }

    private void loadTableDataset() {
        log.info("开始加载任务表字段数据集");
        var stopWatch = new StopWatch();
        stopWatch.start();
        for (var field : orderedFields) {
            if (CollectKit.isNotEmpty(field.getDependsOn())) {
                //依赖类型字段不需要进行数据读取，直接使用其依赖字段的选取结果
                fieldDataset.put(field.getName(), ReadableDataset.lazy());
            } else {
                //其他非依赖型字段，通过读取器读取数据
                fieldDataset.put(field.getName(), fieldProcessor.handle(field));
            }
        }
        dataReady.countDown();
        stopWatch.stop();
        log.info("当前任务表字段数据集加载完成，总计耗时：{} ", DatetimeKit.humanized(stopWatch.getTotalTimeMillis()));
    }

    private List<Map<String, Object>> batch(int batchSize) {
        return IntStream.range(0, batchSize).parallel()
                .mapToObj(i -> tableProcessor.handle(orderedFields))
                .toList();
    }

    @Override
    public Boolean call() throws Exception {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        final var writeType = template.getTable().getWriter().getType();
        final var writeTarget = template.getTable().getWriter().getTarget();
        final var writeTemplate = template.getTable().getWriter().getTemplate();
        var tn = template.getName();
        var summary = """
                开始执行模板 [{}] 的数据生成任务：
                     模板名称：{}
                     生成数量：{}
                     批次数量：{}
                目标数据源类型：{}
                目标数据表名称：{}
                目标数据表字段：{}
                """;
        log.info(summary, tn, tn, template.getAmount(), template.getBatchSize(), writeType, writeTarget, writeTemplate);
        //等待数据就绪
        log.info("等待任务全局和表字段数据就绪……");
        //加载全局数据集
        loadGlobalDataset();
        //加载表字段数据集
        loadTableDataset();
        //发布启动完成事件
        dataReady.await();
        listener.onReady();
        log.info("数据准备完毕，开始执行数据生成任务");
        final var counter = new LongAdder();
        final var stopWatch = new StopWatch();
        stopWatch.start();
        try {
            //分页分批生成任务
            final List<CompletableFuture<?>> futures = PageKit.of(template.getBatchSize(), template.getAmount())
                    .supplier(this::batch)
                    .consumer((data, size, index) -> supplyAsync(() ->
                            writerFactory.newInstance(template.getTable().getWriter()).write(data), executor)
                            .whenComplete((r, ex) -> {
                                counter.add(r);
                                listener.onProcessing(r);
                                if (Objects.nonNull(ex)) {
                                    log.error(String.format("目标源 [%s] 数据生成的第 [%s] 批任务执行异常：", writeTarget, index), ex);
                                } else {
                                    log.info("目标源 [{}] 数据生成的第 [{}] 批任务执行完成, 总计写入 [{}] 条数据", writeTarget, index, r);
                                }
                            }))
                    .collect();
            allOf(futures.toArray(new CompletableFuture[]{})).join();
            stopWatch.stop();
            log.info("当前目标源 [{}] 数据生成任务完成，共计生成 [{}] 条数据，总计耗时：{}", writeTarget,
                    counter.sum(), DatetimeKit.humanized(stopWatch.getTotalTimeMillis()));
        } catch (Exception ex) {
            //发布失败事件
            listener.onError(ex);
            throw new DataGeneratorException(ex);
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            listener.onComplete(counter.sum());
            destroy();
        }
        log.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        return true;
    }

    @Override
    public void registerListener(GeneratorListener listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    @Override
    public void destroy() {
        this.executor.shutdown();
        this.fieldDataset.clear();
        this.orderedFields.clear();
        this.ctx.global().clear();
        this.fieldProcessor.destroy();
        this.tableProcessor.destroy();
        //set null
        this.readerFactory = null;
        this.writerFactory = null;
        this.scriptFactory = null;
        this.template = null;
        this.executor = null;
        this.fieldDataset = null;
        this.ctx = null;
        this.orderedFields = null;
        this.dataReady = null;
        this.fieldProcessor = null;
        this.tableProcessor = null;
        this.listener = null;
    }
}
