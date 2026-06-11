/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.context.GeneratorContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.generator.GeneratorVO;
import org.gensokyo.data.util.ClassKit;
import org.gensokyo.data.util.TypeKit;
import org.gensokyo.kit.security.Md5Kit;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生成器工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@RequiredArgsConstructor
public class GeneratorFactory {

    private final ApplicationContext ctx;
    @SuppressWarnings("rawtypes")
    private final Map<String, Class<? extends Generator>> cache = new ConcurrentHashMap<>(8);

    @SuppressWarnings("rawtypes")
    public @NonNull <T extends GeneratorVO> Generator<T> newInstance(final GeneratorContext<T> ctx) {
        var g = ctx.template().getGenerator();
        var i = ctx.template().getIterator();
        var kg = g.getClass().getName();
        var ki = i.getClass().getName();
        var key = Md5Kit.encrypt(kg + ki);
        if (cache.containsKey(key)) {
            return create(cache.get(key), ctx);
        }

        try {
            Set<Class<? extends Generator>> services = ClassKit.getImplementations(Generator.class);
            for (Class<? extends Generator> service : services) {
                if (TypeKit.isMatchingType(Generator.class, service, ctx.generator().getClass())) {
                    cache.put(key, service);
                    return create(service, ctx);
                }
            }
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
        throw new DataGeneratorException("未找到类型为 " + ctx.template().getGenerator().getType() + " 的生成器子类");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private @NonNull <T extends GeneratorVO> Generator<T> create(Class<? extends Generator> service,
                                                                 final GeneratorContext<T> ctx) {
        Generator<T> generator;
        try {
            generator = (Generator<T>) service.getDeclaredConstructor(ctx.getClass()).newInstance(ctx);
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
        var beanFactory = this.ctx.getAutowireCapableBeanFactory();
        beanFactory.autowireBean(generator);
        beanFactory.initializeBean(generator, generator.getClass().getSimpleName());
        return generator;
    }
}
