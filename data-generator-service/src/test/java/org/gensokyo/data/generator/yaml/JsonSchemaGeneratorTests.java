/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import org.gensokyo.data.model.vo.TemplateVO;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * 生成Yaml文件的json-schema校验文件
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/11 , Version 1.0.0
 */
class JsonSchemaGeneratorTests {

    private void write(String path, JsonNode jsonSchema) throws Exception {
        var om = new ObjectMapper();
        //注册模块
        om.findAndRegisterModules();
        // 获取资源目录的路径
        URL resourceUrl = JsonSchemaGeneratorTests.class.getClassLoader().getResource(".");
        if (resourceUrl == null) {
            throw new IOException("Resource directory not found");
        }

        // 生成的文件路径
        File outputFile = Paths.get(resourceUrl.toURI())
                .resolve(path)
                .toFile();

        // 创建父目录，如果不存在
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        om.writerWithDefaultPrettyPrinter()
                .writeValue(outputFile, jsonSchema);
    }

    @Test
    void gen() throws Exception {
        JacksonModule jacksonModule = new JacksonModule();
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(jacksonModule)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS, Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES, Option.DEFINITION_FOR_MAIN_SCHEMA);
        configBuilder.forTypesInGeneral()
                .withSubtypeResolver(new ClassGraphSubtypeResolver());
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(TemplateVO.class);
        write("META-INF/template-schema.json", jsonSchema);
    }

    @Test
    void gen2() throws Exception {
        JacksonModule jacksonModule = new JacksonModule();
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS, Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
                .with(jacksonModule)
                .forTypesInGeneral()
                .withSubtypeResolver(new ClassGraphSubtypeResolver());
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(User.class);
        write("META-INF/user-schema.json", jsonSchema);
    }

}
