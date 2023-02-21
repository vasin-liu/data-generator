/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.reader;

import org.gensokyo.data.generator.dataset.Dataset;
import org.gensokyo.data.generator.domain.Context;

/**
 * 数据读取接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
public interface Reader {

    Dataset read(final Context ctx);
}
