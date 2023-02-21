/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.writer;

import org.gensokyo.boot.kafka.support.MultipleKafkaTemplate;
import org.gensokyo.data.generator.domain.WriterPO;
import org.gensokyo.data.generator.util.TemplateKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Kafka数据写入器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
public class KafkaWriter extends AbstractWriter {

    private MultipleKafkaTemplate mkt;

    @Autowired
    public void setMkt(MultipleKafkaTemplate mkt) {
        this.mkt = mkt;
    }

    public KafkaWriter(final WriterPO wpo) {
        super(Objects.requireNonNull(wpo));
    }

    @Override
    public long write(final List<Map<String, Object>> data) {
        KafkaTemplate<String, String> kt = mkt.template(wpo.getDataSourceId());
        Objects.requireNonNull(data)
                .forEach(d -> kt.send(Objects.requireNonNull(wpo.getTarget()),
                        TemplateKit.toKafka(wpo.getTemplate(), d)));
        return data.size();
    }
}
