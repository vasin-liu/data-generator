/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.util.ClassKit;
import org.gensokyo.data.util.TypeKit;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 迭代器工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@RequiredArgsConstructor
public class IteratorFactory {

    private final ApplicationContext ctx;

    @SuppressWarnings("rawtypes")
    private final Map<String, Class<? extends Iterator>> cache = new ConcurrentHashMap<>(8);

    @SuppressWarnings({"rawtypes"})
    public @NonNull <T extends IteratorVO> Iterator<T> newInstance(final IteratorContext<T> ictx) {
        var key = ictx.iterator().getClass().getName();
        if (cache.containsKey(key)) {
            return create(cache.get(key), ictx);
        }

        try {
            Set<Class<? extends Iterator>> services = ClassKit.getImplementations(Iterator.class);
            for (Class<? extends Iterator> service : services) {
                if (TypeKit.isMatchingType(Iterator.class, service, ictx.iterator().getClass())) {
                    cache.put(key, service);
                    return create(service, ictx);
                }
            }
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
        throw new DataGeneratorException("未找到类型为 " + ictx.iterator().getType() + " 的迭代器子类");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private @NonNull <T extends IteratorVO> Iterator<T> create(Class<? extends Iterator> service,
                                                               final IteratorContext<T> ictx) {
        Iterator<T> iterator;
        try {
            iterator = (Iterator<T>) service.getDeclaredConstructor(ictx.getClass()).newInstance(ictx);
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
        var beanFactory = this.ctx.getAutowireCapableBeanFactory();
        beanFactory.autowireBean(iterator);
        beanFactory.initializeBean(iterator, iterator.getClass().getSimpleName());
        return iterator;
    }
}
