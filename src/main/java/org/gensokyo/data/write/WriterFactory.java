/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.write;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.po.WriterPO;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * 写入器工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@RequiredArgsConstructor
public class WriterFactory {
    private final AutowireCapableBeanFactory beanFactory;

    public @NonNull Writer newInstance(WriterPO wpo) {
        var writer = switch (Objects.requireNonNull(wpo).getType()) {
            case CONSOLE -> new ConsoleWriter(wpo);
            case JDBC -> new JdbcWriter(wpo);
            default -> null;
        };
        Assert.notNull(writer, "未找到类型为 " + wpo.getType() + " 的数据写入器类");
        beanFactory.autowireBean(writer);
        beanFactory.initializeBean(writer, writer.getClass().getSimpleName());
        return writer;
    }
}
