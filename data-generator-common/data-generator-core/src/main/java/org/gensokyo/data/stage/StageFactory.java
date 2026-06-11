/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.util.ClassKit;
import org.gensokyo.data.util.TypeKit;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据读取器工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@RequiredArgsConstructor
public class StageFactory {

    private final ApplicationContext ctx;
    @SuppressWarnings("rawtypes")
    private final Map<String, Class<? extends Stage>> cache = new ConcurrentHashMap<>(8);

    @SuppressWarnings({"rawtypes"})
    public @NonNull <T extends StageVO> Stage<T> newInstance(final StageContext<T> fctx) {
        var key = fctx.stage().getClass().getName();
        if (cache.containsKey(key)) {
            return create(cache.get(key), fctx);
        }

        try {
            Set<Class<? extends Stage>> services = ClassKit.getImplementations(Stage.class);
            for (Class<? extends Stage> service : services) {
                if (TypeKit.isMatchingType(Stage.class, service, fctx.stage().getClass())) {
                    cache.put(key, service);
                    return create(service, fctx);
                }
            }
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
        throw new DataGeneratorException("未找到类型为 " + fctx.stage().getType() + " 的字段数据处理器类");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private @NonNull <T extends StageVO> Stage<T> create(Class<? extends Stage> service, final StageContext<T> fctx) {
        Stage<T> stage;
        try {
            stage = (Stage<T>) service.getDeclaredConstructor(fctx.getClass()).newInstance(fctx);
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
        var beanFactory = this.ctx.getAutowireCapableBeanFactory();
        beanFactory.autowireBean(stage);
        beanFactory.initializeBean(stage, stage.getClass().getSimpleName());
        return stage;
    }
}
