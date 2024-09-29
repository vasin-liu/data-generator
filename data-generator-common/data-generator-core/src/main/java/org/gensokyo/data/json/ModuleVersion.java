/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.core.util.VersionUtil;

/**
 * SPI方式加载的子类型模块版本定义
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/9 , Version 1.0.0
 */
public class ModuleVersion implements Versioned {

    public final static Version VERSION = VersionUtil.parseVersion(
            "3.0.0", "org.gensokyo.data", "jackson-modules-spi-subtype"
    );

    @Override
    public Version version() {
        return VERSION;
    }
}
