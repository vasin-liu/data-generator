/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.write;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.boot.kafka.support.MultipleKafkaTemplate;
import org.gensokyo.data.context.WriterContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.writer.KafkaWriterPO;
import org.gensokyo.data.util.TemplateKit;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Kafka数据写入器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaWriter<T extends KafkaWriterPO> implements Writer<T> {

    private final MultipleKafkaTemplate multipleKafkaTemplate;

    @Override
    public long write(final WriterContext<T> ctx, final List<Map<String, Object>> dataset) {
        var wpo = ctx.writer();
        try {
            KafkaTemplate<String, String> kt = multipleKafkaTemplate.template(wpo.getDataSourceId());
            Objects.requireNonNull(dataset)
                    .forEach(d -> kt.send(Objects.requireNonNull(wpo.getTarget()),
                            TemplateKit.toKafka(wpo.getTemplate(), d)));
            return dataset.size();

        } catch (Exception e) {
            throw new DataGeneratorException(String.format("写入数据集出现异常，数据库类型为：%s ，数据源编号为：%s ，目标表名为：%s，写入模板为：%s。",
                    wpo.getType(), wpo.getDataSourceId(), wpo.getTarget(), wpo.getTemplate()), e);
        }
    }

}
