/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.parser;

import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;

/**
 * 列表输出解析器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/11 , Version 1.0.0
 */
public class ListOutputParser extends AbstractConversionServiceOutputParser<List<String>> {

    public ListOutputParser(DefaultConversionService defaultConversionService) {
        super(defaultConversionService);
    }

    @Override
    public String getFormat() {
        return """
              响应以中文返回并以符号`,`分隔的值列表，例如：结果1,结果2,结果3
              """;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> parse(String text) {
        return getConversionService().convert(text, List.class);
    }

}
