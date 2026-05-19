/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateDefinitionDetector;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.yaml.JacksonParser;
import org.gensokyo.kit.character.StrKit;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Builds {@link MigrationInventoryEntry} rows from classpath regression fixtures and database templates.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class MigrationInventorySeeder {

    private static final String REGRESSION_CLASSPATH_PATTERN = "classpath:migration/regression/*.yaml";

    private final JacksonParser yamlParser;

    /**
     * Creates a seeder using the default YAML parser.
     */
    public MigrationInventorySeeder() {
        this(new JacksonParser());
    }

    /**
     * Creates a seeder with the given YAML parser (for tests).
     *
     * @param yamlParser parser used to read template YAML
     */
    public MigrationInventorySeeder(JacksonParser yamlParser) {
        this.yamlParser = Objects.requireNonNull(yamlParser, "yamlParser");
    }

    /**
     * Loads regression inventory rows from {@code classpath:migration/regression/*.yaml}.
     *
     * @return entries with ids {@code regression-{basename}} and {@code origin=repository}
     */
    public List<MigrationInventoryEntry> regressionEntriesFromClasspath() {
        List<MigrationInventoryEntry> entries = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(REGRESSION_CLASSPATH_PATTERN);
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                String yaml = resource.getContentAsString(StandardCharsets.UTF_8);
                String basename = basenameWithoutExtension(resource.getFilename());
                entries.add(entryFromRegressionYaml(basename, yaml));
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to load regression templates from classpath", e);
        }
        entries.sort(Comparator.comparing(MigrationInventoryEntry::getId));
        return entries;
    }

    /**
     * Builds inventory rows for each persisted template that parses as V1 (same rules as
     * {@link org.gensokyo.data.controller.TemplateController#buildV1Template}).
     *
     * @param repository template persistence
     * @return entries with ids {@code db-{id}} and {@code origin=database}
     */
    public List<MigrationInventoryEntry> entriesFromDatabase(TemplateRepository repository) {
        List<MigrationInventoryEntry> entries = new ArrayList<>();
        for (TemplatePO entity : repository.findAll()) {
            entryFromTemplate(entity).ifPresent(entries::add);
        }
        entries.sort(Comparator.comparing(MigrationInventoryEntry::getId));
        return entries;
    }

    /**
     * Heuristic scenario family from raw template YAML (query/JDBC vs iterator-only shapes).
     *
     * @param yaml template YAML text
     * @return {@code multi_source} or {@code synthetic}
     */
    public static String inferScenarioFamily(String yaml) {
        if (StrKit.isBlank(yaml)) {
            return "synthetic";
        }
        String lower = yaml.toLowerCase(Locale.ROOT);
        if (lower.contains("type: jdbc") || lower.contains("type: read")) {
            return "multi_source";
        }
        if (lower.contains("iterator:")) {
            return "synthetic";
        }
        return "synthetic";
    }

    private MigrationInventoryEntry entryFromRegressionYaml(String basename, String yaml) {
        TemplateVO v1 = tryParse(yaml, TemplateVO.class);
        MigrationInventoryEntry entry = new MigrationInventoryEntry();
        entry.setId("regression-" + basename);
        entry.setOrigin("repository");
        entry.setMigrationClass(MigrationClassification.UNCLASSIFIED);
        entry.setScenarioFamily(inferScenarioFamily(yaml));
        if (v1 != null && StrKit.isNotBlank(v1.getName())) {
            entry.setName(v1.getName());
        }
        else {
            entry.setName(basename);
        }
        return entry;
    }

    private java.util.Optional<MigrationInventoryEntry> entryFromTemplate(TemplatePO entity) {
        if (entity == null || StrKit.isBlank(entity.getContentYaml())) {
            return java.util.Optional.empty();
        }
        TemplateV2DraftVO v2 = tryParse(entity.getContentYaml(), TemplateV2DraftVO.class);
        TemplateVO v1 = tryParse(entity.getContentYaml(), TemplateVO.class);
        TemplateDefinitionKind kind = TemplateDefinitionDetector.detect(v1, v2);
        if (kind == TemplateDefinitionKind.V2 && v2 != null) {
            return java.util.Optional.empty();
        }
        if (v1 == null) {
            return java.util.Optional.empty();
        }
        MigrationInventoryEntry entry = new MigrationInventoryEntry();
        entry.setId("db-" + entity.getId());
        entry.setOrigin("database");
        entry.setDbTemplateId(entity.getId());
        entry.setMigrationClass(MigrationClassification.UNCLASSIFIED);
        entry.setScenarioFamily(inferScenarioFamily(entity.getContentYaml()));
        entry.setV2DraftPresent(false);
        if (StrKit.isNotBlank(entity.getName())) {
            entry.setName(entity.getName());
        }
        else if (StrKit.isNotBlank(v1.getName())) {
            entry.setName(v1.getName());
        }
        else {
            entry.setName("template-" + entity.getId());
        }
        return java.util.Optional.of(entry);
    }

    private <T> T tryParse(String yaml, Class<T> clazz) {
        try {
            return yamlParser.parse(yaml, clazz);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static String basenameWithoutExtension(String filename) {
        if (filename == null) {
            return "unknown";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
