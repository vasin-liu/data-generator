/*
 * Copyright 濠?2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address闁挎稒鐡岰I Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou闁挎稑鐡攈ina闁挎稑婀糹p code闁?10653闁?
 */
package org.gensokyo.data.ai.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;

import java.util.Map;
import java.util.Objects;


/**
 * 閻庡湱鍋樼紞瀣尵閺勫繒缈婚柛鎴︾細琚欓柡瀣姇濞?
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/11 , Version 1.0.0
 */
public class BeanOutputParser<T> implements OutputParser<T> {

    private String jsonSchema;

    @SuppressWarnings({"FieldMayBeFinal", "rawtypes"})
    private Class<T> clazz;

    @SuppressWarnings("FieldMayBeFinal")
    private ObjectMapper objectMapper;

    public BeanOutputParser(Class<T> clazz) {
        this(clazz, null);
    }

    public BeanOutputParser(Class<T> clazz, ObjectMapper objectMapper) {
        Objects.requireNonNull(clazz, "Java Class cannot be null;");
        this.clazz = clazz;
        this.objectMapper = objectMapper != null ? objectMapper : getObjectMapper();
        generateSchema();
    }

    private void generateSchema() {
        JacksonModule jacksonModule = new JacksonModule();
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(jacksonModule);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonNode = generator.generateSchema(this.clazz);
        ObjectWriter objectWriter = new ObjectMapper().writer(new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter().withLinefeed(System.lineSeparator())));
        try {
            this.jsonSchema = objectWriter.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not pretty print json schema for " + this.clazz, e);
        }
    }

    @Override
    public T parse(String text) {
        try {
            // If the response is a JSON Schema, extract the properties and use them as
            // the response.
            text = this.jsonSchemaToInstance(text);
            return (T) this.objectMapper.readValue(text, this.clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String jsonSchemaToInstance(String text) {
        try {
            Map<String, Object> map = this.objectMapper.readValue(text, Map.class);
            if (map.containsKey("$schema")) {
                return this.objectMapper.writeValueAsString(map.get("properties"));
            }
        } catch (Exception e) {
            // ignore
        }
        return text;
    }

    protected ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Override
    public String getFormat() {
        String template = """
                Your response should be in JSON format.
                Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
                Do not include markdown code blocks in your response.
                Here is the JSON Schema instance your output must adhere to:
                ```%s```
                """;
        return String.format(template, this.jsonSchema);
    }

}

