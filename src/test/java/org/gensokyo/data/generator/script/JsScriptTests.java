/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.script;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.generator.constant.FieldType;
import org.gensokyo.data.generator.constant.ReaderType;
import org.gensokyo.data.generator.constant.ScriptType;
import org.gensokyo.data.generator.constant.WriterType;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.ScriptPO;
import org.gensokyo.data.generator.domain.TemplatePO;
import org.gensokyo.data.generator.yaml.CaseInsensitiveEnumSerializer;
import org.gensokyo.data.generator.yaml.ClassSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * javascript脚本测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/6 , Version 1.0.0
 */
@Slf4j
class JsScriptTests {
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final ObjectMapper om = new ObjectMapper();

    private TemplatePO readMeta() throws IOException {
        Resource r = resolver.getResource("classpath:template/mysql.yaml");
        var reader = new YamlReader(new FileReader(r.getFile()), yamlConfig());
        return reader.read(TemplatePO.class);
    }

    private YamlConfig yamlConfig() {
        var config = new YamlConfig();
        config.setScalarSerializer(ScriptType.class, new CaseInsensitiveEnumSerializer<>(ScriptType.class));
        config.setScalarSerializer(FieldType.class, new CaseInsensitiveEnumSerializer<>(FieldType.class));
        config.setScalarSerializer(ReaderType.class, new CaseInsensitiveEnumSerializer<>(ReaderType.class));
        config.setScalarSerializer(WriterType.class, new CaseInsensitiveEnumSerializer<>(WriterType.class));
        config.setScalarSerializer(Class.class, new ClassSerializer());
        return config;
    }

    @Test
    void case1() throws IOException {
        TemplatePO meta = readMeta();
        var ctx = new Context(meta);
        ctx.global("global_ds_1", List.of(Map.of("CODE", 1), Map.of("CODE", 2), Map.of("CODE", 3)));
        ctx.global("global_ds_2", List.of(Map.of("CODE", 1, "NAME", "黑色"), Map.of("CODE", 2, "NAME", "蓝色"), Map.of("CODE", 3, "NAME", "黄色")));
        Map<String, List<Object>> dataset = Map.of("current_ds_1", List.of(Map.of("CODE", 1), Map.of("CODE", 2), Map.of("CODE", 3)),
                "current_ds_2", List.of(Map.of("CODE", 1, "NAME", "黑色"), Map.of("CODE", 2, "NAME", "蓝色"), Map.of("CODE", 3, "NAME", "黄色")));
        ScriptPO spo = new ScriptPO();
        spo.setType(ScriptType.JAVASCRIPT);
        spo.setContent("classpath:js/test.js");
        try (var js = new JsScript(spo, ctx)) {
            var re = js.eval(dataset);
            Assertions.assertNotNull(re);
            log.info(om.writeValueAsString(re));
        }
    }

    @Test
    void case2() throws IOException {
        TemplatePO meta = readMeta();
        var ctx = new Context(meta);
        ctx.global("global_ds_1", List.of(Map.of("CODE", 1), Map.of("CODE", 2), Map.of("CODE", 3)));
        ctx.global("global_ds_2", List.of(Map.of("CODE", 1, "NAME", "黑色"), Map.of("CODE", 2, "NAME", "蓝色"), Map.of("CODE", 3, "NAME", "黄色")));
        Map<String, List<Object>> dataset = Map.of("current_ds_1", List.of(Map.of("CODE", 1), Map.of("CODE", 2), Map.of("CODE", 3)),
                "current_ds_2", List.of(Map.of("CODE", 1, "NAME", "黑色"), Map.of("CODE", 2, "NAME", "蓝色"), Map.of("CODE", 3, "NAME", "黄色")));
        ScriptPO spo = new ScriptPO();
        spo.setType(ScriptType.JAVASCRIPT);
        spo.setContent("classpath:js/test2.js");
        try (var js = new JsScript(spo, ctx)) {
            var re = js.eval(dataset);
            Assertions.assertNotNull(re);
            log.info(om.writeValueAsString(re));
        }
    }

    @Test
    void case3() throws IOException {
        TemplatePO meta = readMeta();
        var ctx = new Context(meta);
        ctx.global("global_ds_1", List.of(Map.of("CODE", 1), Map.of("CODE", 2), Map.of("CODE", 3)));
        ctx.global("global_ds_2", List.of(Map.of("CODE", 1, "NAME", "黑色"), Map.of("CODE", 2, "NAME", "蓝色"), Map.of("CODE", 3, "NAME", "黄色")));
        Map<String, List<Object>> dataset = Map.of("current_ds_1", List.of(Map.of("CODE", 1), Map.of("CODE", 2), Map.of("CODE", 3)),
                "current_ds_2", List.of(Map.of("CODE", 1, "NAME", "黑色"), Map.of("CODE", 2, "NAME", "蓝色"), Map.of("CODE", 3, "NAME", "黄色")));
        ScriptPO spo = new ScriptPO();
        spo.setType(ScriptType.JAVASCRIPT);
        var script = """
                (context, dataset, arg) => {
                    var arr = dataset['current_ds_2'];
                    var data = [];
                    for (var i = 0; i < arr.length; i++) {
                        console.log(arr[i]);
                        console.log(arr[i].CODE);
                        data.push(arr[i].CODE);
                    }
                    console.log(data);
                    return data;
                }
                """;
        spo.setContent(script);
        try (var js = new JsScript(spo, ctx)) {
            var re = js.eval(dataset);
            Assertions.assertNotNull(re);
            log.info(om.writeValueAsString(re));
        }
    }
}
