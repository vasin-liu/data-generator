/*
 * Copyright 漏 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address锛歅CI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou锛孋hina锛圸ip code锛?10653锛?
 */
package org.gensokyo.data.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.datasource.kafka.DynamicKafkaTemplateRegistry;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.kit.base.ObjectKit;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.PropertyPlaceholderHelper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Kafka鏁版嵁鍐欏叆鍣?
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaWriter<S extends WriteStageVO, T extends KafkaWriterVO> implements Writer<S, T> {
    private final PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}");
    private final DynamicKafkaTemplateRegistry kafkaTemplateRegistry;

    @Override
    public long write(final StageContext<S> ctx, final T wvo, final List<Map<String, Object>> dataset) {
        try {
            KafkaTemplate<String, String> kt = kafkaTemplateRegistry.template(wvo.getDataSourceId());
            Objects.requireNonNull(dataset)
                    .forEach(d -> kt.send(Objects.requireNonNull(wvo.getTarget()), fillValue(wvo.getTemplate(), d)));
            return dataset.size();

        } catch (Exception e) {
            throw new DataGeneratorException(String.format(
                    "写入数据集时发生异常，写入器类型为：%s，数据源编号为：%s，目标为：%s，写入模板为：%s。",
                    wvo.getType(), wvo.getDataSourceId(), wvo.getTarget(), wvo.getTemplate()), e);
        }
    }

    public String fillValue(String template, Map<String, Object> data) {
        if (StrKit.isNotBlank(template) && CollectKit.isNotEmpty(data)) {
            Properties properties = new Properties();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                properties.put(entry.getKey(), ObjectKit.toString(entry.getValue()));
            }
            return helper.replacePlaceholders(template, properties);
        }
        return Strings.EMPTY;
    }
}
