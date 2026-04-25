/*
 * Copyright 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 */
package org.gensokyo.data.generator.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import org.gensokyo.data.model.vo.TemplateVO;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * JSON schema generator tests.
 */
class JsonSchemaGeneratorTests {

    private void write(String path, JsonNode jsonSchema) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        URL resourceUrl = JsonSchemaGeneratorTests.class.getClassLoader().getResource(".");
        if (resourceUrl == null) {
            throw new IOException("Resource directory not found");
        }

        File outputFile = Paths.get(resourceUrl.toURI())
                .resolve(path)
                .toFile();

        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(outputFile, jsonSchema);
    }

    @Test
    void gen() throws Exception {
        JacksonModule jacksonModule = new JacksonModule();
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON
        ).with(jacksonModule)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS, Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES, Option.DEFINITION_FOR_MAIN_SCHEMA);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(TemplateVO.class);
        write("META-INF/template-schema.json", jsonSchema);
    }

    @Test
    void gen2() throws Exception {
        JacksonModule jacksonModule = new JacksonModule();
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON
        );
        configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS, Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
                .with(jacksonModule);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(User.class);
        write("META-INF/user-schema.json", jsonSchema);
    }
}
