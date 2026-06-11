/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.plugin;

/**
 * 插件接口定义
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/17 , Version 1.0.0
 */
public interface Plugin {

    /**
     * 插件名称
     *
     * @return 插件名称
     */
    String name();

    /**
     * 插件类型
     *
     * @return 插件类型
     */
    String type();

    /**
     * 插件别名
     *
     * @return 插件别名
     */
    String[] aliases();

    /**
     * 插件版本号
     *
     * @return 插件版本号
     */
    String version();

    default int priority() {
        return 0;
    }

    default int compareTo(Plugin o) {
        return o.priority() - this.priority();
    }
}
