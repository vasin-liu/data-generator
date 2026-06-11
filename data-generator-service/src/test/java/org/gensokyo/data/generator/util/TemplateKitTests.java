/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * 模板工具测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/5 , Version 1.0.0
 */
class TemplateKitTests {

    private final PropertyPlaceholderHelper pph = new PropertyPlaceholderHelper("${", "}");

    @Test
    void case1() {
        var template = "insert into table (col1,col2,col3) values (${col1},${col2},${col3})";
        String sql = pph.replacePlaceholders(template, placeholderName -> {
            System.out.println(placeholderName);
            return ":" + placeholderName;
        });
        System.out.println(sql);
        Assertions.assertEquals(-1, sql.indexOf("$"));
    }

    @Test
    void case2() {
        var template = "LOAD DATA LOCAL INFILE 'sql.csv' IGNORE INTO TABLE test (${f_string},${f_date},${f_float},${f_integer},${f_integer_desc})";
        String sql = pph.replacePlaceholders(template, placeholderName -> {
            System.out.println(placeholderName);
            return ":" + placeholderName;
        });
        System.out.println(sql);
        Assertions.assertEquals(-1, sql.indexOf("$"));
    }
}
