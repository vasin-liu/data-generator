/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read;

import org.gensokyo.data.context.ReaderContext;
import org.gensokyo.data.po.reader.ReaderPO;
import org.gensokyo.data.value.Value;

/**
 * 数据读取器接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/27 , Version 1.0.0
 */
public interface Reader<T extends ReaderPO> {

    Value read(final ReaderContext<T> ctx, final Value input);
}
