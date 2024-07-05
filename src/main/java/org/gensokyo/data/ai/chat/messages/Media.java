/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.messages;

import lombok.Getter;
import org.gensokyo.kit.Assert;
import org.springframework.util.MimeType;

/**
 * 载荷类型
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public record Media(MimeType mimeType, Object data) {

    public Media {
        Assert.notNull(mimeType, "MimeType 不能为空");
    }
}
