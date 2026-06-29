/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.ScenarioCatalogEntryDto;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.yaml.YamlParser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads official greenfield scenario YAML from {@code template/v2-scenarios/} for the console wizard.
 *
 * @author Gensokyo
 * @since 2026-06-02
 */
@Service
@RequiredArgsConstructor
public class V2ScenarioCatalogService {

    private static final String SCENARIO_ROOT = "template/v2-scenarios/";

    private static final Pattern SCENARIO_ID = Pattern.compile("^#\\s*scenarioId:\\s*(\\S+)\\s*$");
    private static final Pattern CATALOG_REF = Pattern.compile("^#\\s*catalogRef:\\s*(.+?)\\s*$");

    private static final List<CatalogBinding> OFFICIAL_CATALOG = List.of(
            new CatalogBinding("GF-A", "A", "scenario-a-synthetic.yaml"),
            new CatalogBinding("GF-B", "B", "scenario-dag-join.yaml"),
            new CatalogBinding("GF-BJ", "B", "scenario-b-lookup-join.yaml"),
            new CatalogBinding("GF-IR", "A", "scenario-inline-rows.yaml"),
            new CatalogBinding("GF-SP", "JS", "scenario-spel-transform.yaml"),
            new CatalogBinding("GF-WF", "WF", "scenario-wf-branch.yaml"),
            new CatalogBinding("GF-WFS", "WF", "scenario-wf-shared-state.yaml"),
            new CatalogBinding("GF-JS", "JS", "scenario-js-transform.yaml"),
            new CatalogBinding("GF-EP", "E", "scenario-e-partial-sink.yaml"),
            new CatalogBinding("GF-AI", "AI", "scenario-ai-inline.yaml"),
            new CatalogBinding("GF-FC", "F", "scenario-f-chunked-csv.yaml"),
            new CatalogBinding("GF-FN", "F", "scenario-f-streaming-ndjson.yaml"),
            new CatalogBinding("GF-GP", "G", "scenario-g-upsert-pg.yaml"),
            new CatalogBinding("GF-GM", "G", "scenario-g-upsert-mysql.yaml"));

    private final YamlParser yamlParser;

    /**
     * @return official scenario catalog entries in roadmap family order
     */
    public List<ScenarioCatalogEntryDto> listOfficial() {
        List<ScenarioCatalogEntryDto> rows = new ArrayList<>();
        for (CatalogBinding binding : OFFICIAL_CATALOG) {
            String yaml = readScenarioYaml(binding.resourceFile());
            ScenarioHeader header = parseHeader(yaml);
            TemplateV2DraftVO draft = yamlParser.parse(yaml, TemplateV2DraftVO.class);
            String name = draft != null && draft.getName() != null ? draft.getName() : binding.resourceFile();
            rows.add(new ScenarioCatalogEntryDto(
                    binding.scenarioId(),
                    binding.family(),
                    name,
                    header.catalogRef() != null ? header.catalogRef() : binding.scenarioId(),
                    binding.resourceFile()));
        }
        return rows;
    }

    /**
     * Parses scenario YAML into an editable V2 draft with a unique name suffix.
     *
     * @param scenarioId official catalog id
     * @return draft suitable for the console editor
     * @throws IllegalArgumentException when the id is unknown or YAML cannot be read
     */
    public TemplateV2DraftVO loadDraft(String scenarioId) {
        CatalogBinding binding = resolveBinding(scenarioId);
        String yaml = readScenarioYaml(binding.resourceFile());
        TemplateV2DraftVO draft = yamlParser.parse(yaml, TemplateV2DraftVO.class);
        if (draft == null) {
            throw new IllegalArgumentException("Scenario YAML did not parse as Template V2: " + binding.resourceFile());
        }
        // Avoid catalog name collisions when the operator saves without renaming.
        String baseName = draft.getName() != null && !draft.getName().isBlank()
                ? draft.getName().trim()
                : binding.scenarioId().toLowerCase(Locale.ROOT);
        draft.setName(baseName + "-" + System.currentTimeMillis());
        return draft;
    }

    private CatalogBinding resolveBinding(String scenarioId) {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId is required");
        }
        String normalized = scenarioId.trim().toUpperCase(Locale.ROOT);
        for (CatalogBinding binding : OFFICIAL_CATALOG) {
            if (binding.scenarioId().equals(normalized)) {
                return binding;
            }
        }
        throw new IllegalArgumentException("Unknown scenario catalog id: " + scenarioId);
    }

    private String readScenarioYaml(String resourceFile) {
        ClassPathResource resource = new ClassPathResource(SCENARIO_ROOT + resourceFile);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Scenario resource missing: " + resourceFile);
        }
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Failed to read scenario: " + resourceFile, e);
        }
    }

    private static ScenarioHeader parseHeader(String yaml) {
        String catalogRef = null;
        for (String line : yaml.split("\\R")) {
            Matcher refMatcher = CATALOG_REF.matcher(line);
            if (refMatcher.matches()) {
                catalogRef = refMatcher.group(1).trim();
            }
            Matcher idMatcher = SCENARIO_ID.matcher(line);
            if (idMatcher.matches()) {
                // scenarioId from file is informational; catalog binding is authoritative.
            }
        }
        return new ScenarioHeader(catalogRef);
    }

    private record CatalogBinding(String scenarioId, String family, String resourceFile) {
    }

    private record ScenarioHeader(String catalogRef) {
    }
}
