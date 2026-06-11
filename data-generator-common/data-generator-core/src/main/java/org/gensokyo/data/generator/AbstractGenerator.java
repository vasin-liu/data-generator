/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataSet;
import org.gensokyo.data.context.GeneratorContext;
import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.exception.NotEnoughElementException;
import org.gensokyo.data.iterator.IteratorFactory;
import org.gensokyo.data.model.vo.condition.OtherwiseVO;
import org.gensokyo.data.model.vo.condition.WhenVO;
import org.gensokyo.data.model.vo.generator.GeneratorVO;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.pipeline.DefaultRowPipeline;
import org.gensokyo.data.pipeline.DefaultRowPipelineFactory;
import org.gensokyo.data.pipeline.DefaultWritePipelineFactory;
import org.gensokyo.data.script.Script;
import org.gensokyo.data.script.ScriptFactory;
import org.gensokyo.data.stage.StageFactory;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.json.JsonKit;
import org.gensokyo.kit.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 生成器抽象类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/19 , Version 1.0.0
 */
@Slf4j
public abstract class AbstractGenerator<G extends GeneratorVO> implements Generator<G> {
    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    protected final CountDownLatch latch = new CountDownLatch(1);
    protected final GeneratorContext<G> ctx;
    protected final BlockingQueue<Value> queue;
    protected final ThreadPoolTaskExecutor producerExecutor;
    protected final ThreadPoolTaskExecutor consumerExecutor;
    @Setter(onMethod_ = @Autowired)
    protected IteratorFactory iteratorFactory;
    @Setter(onMethod_ = @Autowired)
    protected StageFactory stageFactory;
    @Setter(onMethod_ = @Autowired)
    protected ScriptFactory scriptFactory;
    @Setter(onMethod_ = @Autowired)
    protected DefaultRowPipelineFactory defaultRowPipelineFactory;
    @Setter(onMethod_ = @Autowired)
    protected DefaultWritePipelineFactory defaultWritePipelineFactory;

    public AbstractGenerator(final GeneratorContext<G> ctx) {
        this.ctx = Objects.requireNonNull(ctx);
        var batchSize = ctx.generator().getBatchSize();
        Assert.isTrue(batchSize > 0 && batchSize < Integer.MAX_VALUE, "迭代器配置的批次大小必须大于0");
        this.queue = new LinkedBlockingQueue<>(batchSize);
        this.producerExecutor = producerExecutor();
        this.consumerExecutor = consumerExecutor();
    }

    @SneakyThrows
    @Override
    public void startup() {
        var ivo = ctx.template().getIterator();
        var batchSize = ctx.generator().getBatchSize();

        //启动消费者
        consumerExecutor.execute(() -> consume(batchSize));

        //执行遍历任务
        doIteration(ivo);

        //等待生产线程结束
        producerExecutor.shutdown();
        //终止值
        queue.put(new EndValue());

        //等待消费线程结束
        latch.await();
        consumerExecutor.shutdown();
    }

    /**
     * 迭代器核心处理方法，如果有多个子迭代，递归处理迭代，并且每个迭代中会处理条件表达式，部分迭代的值可能会被条件表达式跳过
     * 因此生成的结果最终可能会比迭代预期的总数量少
     *
     * @param ivo          迭代配置对象
     * @param parentValues 迭代输入值
     */
    @SuppressWarnings("t")
    protected void doIteration(IteratorVO ivo, Value... parentValues) {
        try (var it = iteratorFactory.newInstance(new IteratorContext<>(ctx.template(), ivo, parentValues))) {
            while (it.hasNext()) {
                var currentValue = it.next();
                //条件判断脚本
                var p = condition(ivo, currentValue);
                if (Boolean.TRUE.equals(p.getLeft())) {
                    continue;
                } else {
                    currentValue = p.getRight();
                }

                //迭代值流水线处理
                var currentNewValue = createPipelineAndExecute(ctx, ivo, currentValue);
                var finalValues = new Value[parentValues.length + 1];
                System.arraycopy(parentValues, 0, finalValues, 0, parentValues.length);
                //当前所有嵌套迭代的值
                finalValues[parentValues.length] = currentNewValue;
                if (Objects.nonNull(ivo.getIterator())) {
                    //递归循环
                    doIteration(ivo.getIterator(), finalValues);
                } else {
                    //var input = ListValue.fromValueArray(finalValues);
                    var input = SingleValue.of(finalValues);
                    if (!initialized.get()) {
                        if (log.isDebugEnabled()) {
                            log.debug("预加载内存驻留数据集");
                        }
                        preloading(input);
                    } else {
                        doJob(input);
                    }
                }
                //是否需要暂停
                if (Objects.nonNull(ivo.getPause()) && ivo.getPause() > 0) {
                    Thread.sleep(ivo.getPause() * 1000);
                }
            }
        } catch (NotEnoughElementException e) {
            log.error("===> 当前已无足够的数据可供选取，无法继续生产数据，终止任务 <===");
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
    }

    /**
     * 预加载数据，对于部分有配置 {@code inMemory } 且值为 {@code true } 的字段，需要先执行一行记录生成缓存数据，
     * 以便后续依赖字段或者适配字段的值选择策略，部分值选择策略需要 {@code inMemory } 配置的值为 {@code true }
     * 才能正确的工作
     *
     * @param input 输入值
     */
    protected void preloading(final Value input) throws InterruptedException {
        //先执行一行记录生成缓存数据
        var tdc = DataSet.getOrCreate(ctx.template().getId(), ctx.template().getInstanceId());
        if (tdc.isEmpty()) {
            produce(input);
            //初始化完成
            initialized.compareAndSet(false, true);
            log.info("模板 {}-{} 所需的缓存数据已加载完毕", ctx.template().getId(), ctx.template().getName());
        }
    }

    protected abstract void doJob(final Value input);

    /**
     * 处理迭代条件判断，返回结果为 {@code Triple<Boolean, Boolean, Value> }, 其中第一个参数 {@code Boolean}
     * 是否跳过当前迭代值，第二个参数 {@code Boolean} 当前表达式的判断结果是否满足，第三个参数 {@code Value}
     * 条件计算结果值，假如条件计算结果值的类型与原迭代产生的值的类型不一致，原则上需要使用的时候进行区分，
     * 迭代器和生成器并不会校验值的类型是否一致，最终给到行数据处理器时需要小心判断结果值类型
     *
     * @param ivo   迭代配置
     * @param input 输入值
     * @return Triple<Boolean, Boolean, Value> , 其中第一个参数 {@code Boolean} 是否跳过当前迭代值，
     * 第二个参数 {@code Boolean} 当前表达式的判断结果是否满足，第三个参数 {@code Value} 条件计算结果值
     */
    protected Triple<Boolean, Boolean, Value> condition(final IteratorVO ivo, final Value input) {
        var choose = choose(ivo.getChoose(), input);
        if (Boolean.TRUE.equals(choose.getMiddle())) {
            return choose;
        }

        //其他
        var otherwise = otherwise(ivo.getOtherwise(), input);
        if (Boolean.TRUE.equals(otherwise.getMiddle())) {
            return otherwise;
        }

        // 无任何条件匹配
        return Triple.of(false, false, input);
    }

    protected Triple<Boolean, Boolean, Value> choose(final List<WhenVO> whens, final Value input) {
        if (CollectKit.isEmpty(whens)) {
            return Triple.of(false, false, input);
        }
        for (var when : whens) {
            var result = when(when, input);
            if (Boolean.TRUE.equals(result.getMiddle())) {
                return result;
            }
        }
        return Triple.of(false, false, input);
    }

    protected Triple<Boolean, Boolean, Value> when(final WhenVO when, final Value input) {
        var wssp = new ScriptStageVO(when.getWhen());
        var whenScript = scriptFactory.newInstance(wssp.getLanguage());
        if (Objects.isNull(whenScript)) {
            return Triple.of(false, false, input);
        }

        var wctx = new StageContext<>(ctx.template(), null, wssp);
        var val = whenScript.eval(wctx, wssp.getLanguage(), input);
        if (!(val.get() instanceof Boolean flag)) {
            var msg = String.format("执行条件判断阶段失败，条件表达式 %s 的执行结果不为 Boolean 值，输入值为：%s，上下文信息为：%s",
                    when.getWhen().getContent(), input, JsonKit.write(ctx));
            throw new DataGeneratorException(msg);
        }

        if (Boolean.TRUE.equals(flag)) {
            var tssp = new ScriptStageVO(when.getThen());
            var thenScript = scriptFactory.newInstance(tssp.getLanguage());
            if (Objects.isNull(thenScript)) {
                return Triple.of(false, false, input);
            }
            return eval(thenScript, tssp, input);
        }

        return Triple.of(false, false, input);
    }

    protected Triple<Boolean, Boolean, Value> otherwise(final OtherwiseVO otherwise, final Value input) {
        if (Objects.nonNull(otherwise)) {
            var ossp = new ScriptStageVO(otherwise.getThen());
            var otherwiseScript = scriptFactory.newInstance(ossp.getLanguage());
            if (Objects.nonNull(otherwiseScript)) {
                return eval(otherwiseScript, ossp, input);
            }
        }
        return Triple.of(false, false, input);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Triple<Boolean, Boolean, Value> eval(Script script,
                                                 ScriptStageVO ssvo, Value input) {
        var sctx = new StageContext<>(ctx.template(), null, ssvo);
        var r = script.eval(sctx, ssvo.getLanguage(), input);
        // 特殊处理，如果值表达式执行结果为 Boolean 值类型
        // 如果值为 true 那么跳过当前迭代值
        // 如果值为 false 那么不跳过当前迭代值，直接使用迭代的输入值（即当前迭代的值，非脚本计算后的值）
        if (r.get() instanceof Boolean skip) {
            return Triple.of(skip, true, input);
        } else {
            return Triple.of(false, true, r);
        }
    }

    /**
     * 对迭代器中每一个元素结果进行流水线处理，假如迭代器有配置条件判断表达式，那么会先执行这条件判断表达式，
     * 如果条件判断表达式返回 {@code true}，则不会执行下面的流水线，如果返回 {@code false}，则此处的
     * {@code input} 值为条件判断表达式的执行结果值
     *
     * @param ctx   生成器上下文
     * @param ivo   迭代器配置对象
     * @param input 输入值
     * @return 输出值
     */
    protected Value createPipelineAndExecute(final GeneratorContext<G> ctx, final IteratorVO ivo, final Value input) {
        var pipeline = new DefaultRowPipeline();
        for (var svo : ivo.getStages()) {
            var stageCtx = new StageContext<>(ctx.template(), null, svo);
            var stage = stageFactory.newInstance(stageCtx);
            pipeline.next(stage);
        }
        return pipeline.execute(input);
    }

    /**
     * 生产者处理逻辑，负责生产数据，然后对数据进行流水线处理，最终放入队列
     *
     * @param input 输入值
     */
    protected void produce(final Value input) {
        try {
            //var nv = createPipelineAndExecute(ctx, input);
            var rowVal = defaultRowPipelineFactory.startup(new TemplateContext(ctx.template(), input));
            queue.put(rowVal);
        } catch (NotEnoughElementException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成数据发生异常，上下文信息为：{}", JsonKit.write(ctx), e);
        }
    }

    /**
     * 消费者处理逻辑，批量提交数据
     *
     * @param batchSize 每个批次提交数量
     */
    @SneakyThrows
    protected void consume(int batchSize) {
        try {
            var total = new LongAdder();
            var data = new ArrayList<Value>();

            while (!queue.isEmpty() || !Thread.currentThread().isInterrupted()) {
                var value = queue.take();
                if (value instanceof EndValue) {
                    break;
                }
                data.add(value);
                total.increment();
                if (data.size() == batchSize) {
                    commitBatch(new GeneratorContext<>(ctx.template(), ctx.generator()), data);
                    log.info("已生成 {} 条数据", total.sum());
                    data.clear();
                }
            }

            if (!data.isEmpty()) {
                commitBatch(new GeneratorContext<>(ctx.template(), ctx.generator()), data);
                data.clear();
            }
            log.info("数据生成完成，总计 {} 条", total.sum());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            latch.countDown();
        }
    }

    /**
     * 批量提交数据
     *
     * @param ctx  生成器上下文
     * @param data 待提交的数据
     */
    protected void commitBatch(final GeneratorContext<G> ctx, final List<Value> data) {
        //写入数据
        defaultWritePipelineFactory.startup(new TemplateContext(ctx.template(), ListValue.fromValueCollection(data)));
    }

    protected abstract ThreadPoolTaskExecutor producerExecutor();

    protected ThreadPoolTaskExecutor consumerExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        //核心线程池大小
        executor.setCorePoolSize(1);
        //最大线程数
        executor.setMaxPoolSize(1);
        //队列容量
        executor.setQueueCapacity(1);
        //活跃时间
        executor.setKeepAliveSeconds(240);
        //线程名字前缀
        executor.setThreadNamePrefix("DG-CONSUMER-" + ctx.template().getId() + "-" + ctx.template().getName() + "-");
        // 设置线程池关闭的时候等待所有任务都完成再继续销毁其他的Bean
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(Integer.MAX_VALUE);
        //队列满时阻塞主线程提交任务动作
        executor.setRejectedExecutionHandler(new BlockWhenQueueFullHandler());
        executor.initialize();
        return executor;
    }

    @Override
    public void cleanup() {
        DataSet.remove(ctx.template().getId(), ctx.template().getInstanceId());
        queue.clear();
        initialized.compareAndSet(true, false);
    }

    @Override
    public void shutdown() {
        this.cleanup();
        var tctx = new TemplateContext(ctx.template(), Value.EMPTY);
        defaultRowPipelineFactory.shutdown(tctx);
        defaultWritePipelineFactory.shutdown(tctx);
        if (!producerExecutor.getThreadPoolExecutor().isShutdown()) {
            producerExecutor.shutdown();
        }
        if (!consumerExecutor.getThreadPoolExecutor().isShutdown()) {
            consumerExecutor.shutdown();
        }
        log.info("===> 模板 {} 的数据生成器已关闭 <===", tctx.template().getName());
    }
}
