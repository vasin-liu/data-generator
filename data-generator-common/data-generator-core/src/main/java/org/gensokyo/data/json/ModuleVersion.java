/*
 * Copyright 漏 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address锛歅CI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou锛孋hina锛圸ip code锛?10653锛?
 */
package org.gensokyo.data.json;

import tools.jackson.core.Version;
import tools.jackson.core.Versioned;
import tools.jackson.core.util.VersionUtil;

/**
 * SPI鏂瑰紡鍔犺浇鐨勫瓙绫诲瀷妯″潡鐗堟湰瀹氫箟
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

