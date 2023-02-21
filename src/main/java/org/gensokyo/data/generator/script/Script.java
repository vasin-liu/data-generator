/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.script;

/**
 * 脚本执行接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/5 , Version 1.0.0
 */
public interface Script extends AutoCloseable {

    default Object eval(String script, Object dataset, Object... args) {
        return eval(dataset, args);
    }

    Object eval(Object dataset, Object... args);
}
