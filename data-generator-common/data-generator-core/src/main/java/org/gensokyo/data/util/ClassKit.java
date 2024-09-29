/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.util;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.HashSet;
import java.util.Set;

/**
 * 类工具
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/17 , Version 1.0.0
 */
public class ClassKit {

    private ClassKit() {
        throw new UnsupportedOperationException();
    }

    public static <T> Set<Class<? extends T>> getImplementations(Class<T> interfaceClass) throws ClassNotFoundException {
        return getImplementations(interfaceClass, interfaceClass.getPackage().getName());
    }

    public static <T> Set<Class<? extends T>> getImplementations(Class<T> interfaceClass, String basePackage) throws ClassNotFoundException {
        // 创建一个类路径扫描器
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        // 添加一个过滤器，只包含指定接口的实现类
        scanner.addIncludeFilter(new AssignableTypeFilter(interfaceClass));

        // 扫描指定包路径
        Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);

        Set<Class<? extends T>> implementations = new HashSet<>();
        for (BeanDefinition beanDefinition : candidateComponents) {
            @SuppressWarnings("unchecked")
            Class<? extends T> clazz = (Class<? extends T>) Class.forName(beanDefinition.getBeanClassName());
            implementations.add(clazz);
        }

        return implementations;
    }
}
