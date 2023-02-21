/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.gensokyo.data.generator.constant.Const;
import org.gensokyo.kit.base.ObjectKit;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * 模板工具
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/5 , Version 1.0.0
 */
@Slf4j
public final class TemplateKit {
    private static final PropertyPlaceholderHelper HELPER = new PropertyPlaceholderHelper("${", "}");

    private TemplateKit() {
        throw new UnsupportedOperationException();
    }

    public static String toSql(String template) {
        if (StringUtils.hasText(template)) {
            return HELPER.replacePlaceholders(template, Const.COLON::concat);
        }
        return Strings.EMPTY;
    }

    public static String toKafka(String template, Map<String, Object> data) {
        return fillValue(template, data);
    }

    public static String fillValue(String template, Map<String, Object> data) {
        if (StringUtils.hasText(template) && !CollectionUtils.isEmpty(data)) {
            Properties properties = new Properties();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                properties.put(entry.getKey(), ObjectKit.toString(entry.getValue()));
            }
            return HELPER.replacePlaceholders(template, properties);
        }
        return Strings.EMPTY;
    }

    public static IndexRequest toEs(String template, String index, Map<String, Object> data) {
        IndexRequest ir = new IndexRequest(Objects.requireNonNull(index));
        ir.create(false);
        String value = fillValue(template, Objects.requireNonNull(data));
        log.debug("IndexRequest source ===> {}", value);
        ir.source(value, XContentType.JSON);
        return ir;
    }
}
