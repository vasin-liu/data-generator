/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.write;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.WriteStagePO;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 控制台数据写入类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
public class ConsoleWriter extends AbstractWriter {

    protected ConsoleWriter(WriteStagePO wpo) {
        super(wpo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public long write(final List<Map<String, Object>> dataset) {
        try {
            log.info("开始写入控制台数据：\n" + JsonKit.write(dataset));
            log.info("写入控制台数据成功！");
            return Objects.nonNull(dataset) ? dataset.size() : 0;
        } catch (Exception e) {
            log.error("写控制台出现异常：", e);
            throw new DataGeneratorException("写控制台出现异常", e);
        }
    }

}
