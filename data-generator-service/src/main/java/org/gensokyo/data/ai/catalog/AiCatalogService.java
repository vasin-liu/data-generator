/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.catalog;

import org.gensokyo.data.ai.parser.ListOutputParser;
import org.gensokyo.data.api.console.dto.AiCatalogDto;
import org.gensokyo.data.api.console.dto.AiParserEntryDto;
import org.gensokyo.data.api.console.dto.AiPromptTemplateDto;
import org.gensokyo.data.api.console.dto.AiProviderEntryDto;
import org.gensokyo.data.yaml.YamlParser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bundled AI authoring metadata for the operator console.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
@Service
public class AiCatalogService {

    private static final String PROMPT_TEMPLATE_RESOURCE = "template/ai/prompt-templates.yaml";

    private final YamlParser yamlParser;

    /**
     * @param yamlParser YAML parser for bundled prompt templates
     */
    public AiCatalogService(YamlParser yamlParser) {
        this.yamlParser = yamlParser;
    }

    /**
     * @return providers, parsers, and prompt templates for the console AI source editor
     */
    public AiCatalogDto catalog() {
        return new AiCatalogDto(listProviders(), listParsers(), loadPromptTemplates());
    }

    private static List<AiProviderEntryDto> listProviders() {
        return List.of(
                new AiProviderEntryDto(
                        "INLINE",
                        "Inline rows (deterministic)",
                        "Embed generated rows in provider.options.rows for CI-friendly scenarios.",
                        false),
                new AiProviderEntryDto(
                        "ECHO",
                        "Echo prompt",
                        "Materialize the prompt text as a single content column (no external call).",
                        false),
                new AiProviderEntryDto(
                        "OLLAMA",
                        "Ollama",
                        "Call a remote Ollama endpoint via the service runtime bridge.",
                        true),
                new AiProviderEntryDto(
                        "OPENAI",
                        "OpenAI",
                        "Call an OpenAI-compatible chat-completions endpoint (apiKey + model in provider.options).",
                        true),
                new AiProviderEntryDto(
                        "AZURE_OPENAI",
                        "Azure OpenAI",
                        "Call an Azure OpenAI deployment URL via source.api with apiKey + model in provider.options.",
                        true));
    }

    private static List<AiParserEntryDto> listParsers() {
        return List.of(
                new AiParserEntryDto(
                        "",
                        "Raw text",
                        "Keep model output as a single string without structured parsing."),
                new AiParserEntryDto(
                        ListOutputParser.class.getName(),
                        "Comma-separated list",
                        "Parse comma-separated values into a list of rows."));
    }

    @SuppressWarnings("unchecked")
    private List<AiPromptTemplateDto> loadPromptTemplates() {
        ClassPathResource resource = new ClassPathResource(PROMPT_TEMPLATE_RESOURCE);
        if (!resource.exists()) {
            return List.of();
        }
        try (InputStream in = resource.getInputStream()) {
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> root = yamlParser.parse(yaml, Map.class);
            if (root == null || !root.containsKey("templates")) {
                return List.of();
            }
            Object templatesNode = root.get("templates");
            if (!(templatesNode instanceof List<?> rows)) {
                return List.of();
            }
            List<AiPromptTemplateDto> templates = new ArrayList<>();
            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> map)) {
                    continue;
                }
                String id = stringValue(map.get("id"));
                String label = stringValue(map.get("label"));
                String prompt = stringValue(map.get("prompt"));
                if (id == null || label == null || prompt == null) {
                    continue;
                }
                templates.add(new AiPromptTemplateDto(id, label, prompt));
            }
            return List.copyOf(templates);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to read AI prompt templates: " + PROMPT_TEMPLATE_RESOURCE, ex);
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
