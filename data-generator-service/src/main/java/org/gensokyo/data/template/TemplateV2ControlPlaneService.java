/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.sql.ExecutionShape;
import org.gensokyo.data.calcite.sql.ExecutionShapeClassifier;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.migration.MigrationDraftService;
import org.gensokyo.data.template.migration.MigrationPlanExplain;
import org.gensokyo.data.template.migration.MigrationPlanExplainService;
import org.gensokyo.data.template.migration.MigrationV2CompareResolver;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.character.StrKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Orchestrates Template V2 control-plane operations: validate, explain, and preview.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public class TemplateV2ControlPlaneService {

    private final TemplateRepository repository;
    private final YamlParser yamlParser;
    private final MigrationDraftService migrationDraftService;
    private final MigrationPlanExplainService planExplainService;
    private final TemplateV1Loader templateV1Loader;
    private final MigrationV2CompareResolver v2CompareResolver;
    private final TemplateV2Runner templateV2Runner;
    private final DataGeneratorProperties properties;

    /**
     * Creates the control-plane service with persistence and migration helpers.
     *
     * @param repository              template persistence
     * @param yamlParser              YAML parser for definition probes
     * @param migrationDraftService   V1 → V2 draft builder when content is V1
     * @param planExplainService      Calcite plan summary and V1/V2 diff notes
     * @param templateV2Runner        V2 runtime runner for bounded preview
     * @param properties              service properties (default preview row cap)
     */
    public TemplateV2ControlPlaneService(
            TemplateRepository repository,
            YamlParser yamlParser,
            MigrationDraftService migrationDraftService,
            MigrationPlanExplainService planExplainService,
            TemplateV2Runner templateV2Runner,
            DataGeneratorProperties properties) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.yamlParser = Objects.requireNonNull(yamlParser, "yamlParser");
        this.migrationDraftService = Objects.requireNonNull(migrationDraftService, "migrationDraftService");
        this.planExplainService = Objects.requireNonNull(planExplainService, "planExplainService");
        this.templateV1Loader = new TemplateV1Loader(yamlParser);
        this.v2CompareResolver = new MigrationV2CompareResolver(yamlParser, migrationDraftService);
        this.templateV2Runner = Objects.requireNonNull(templateV2Runner, "templateV2Runner");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * Validates a Template V2 draft: normalizes singular/plural fields, runs structural validation,
     * and attaches execution-shape warnings when CHUNKED policy applies.
     *
     * @param draft operator draft; may use singular {@code transform} / {@code sink}
     * @return validation outcome with errors and optional warnings; never {@code null}
     */
    public TemplateV2ValidationResult validate(TemplateV2DraftVO draft) {
        if (draft == null) {
            return TemplateV2ValidationResult.invalid("Template V2 draft must not be null");
        }

        var errors = new ArrayList<String>();
        var warnings = new ArrayList<String>();

        TemplateV2VO normalized;
        try {
            normalized = TemplateV2Normalizer.normalize(draft);
        }
        catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
            return TemplateV2ValidationResult.from(errors, warnings);
        }

        if (normalized == null) {
            errors.add("Template V2 must not be null");
            return TemplateV2ValidationResult.from(errors, warnings);
        }

        try {
            TemplateV2Validator.validate(normalized);
        }
        catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
            return TemplateV2ValidationResult.from(errors, warnings);
        }

        appendChunkedExecutionShapeWarnings(normalized, warnings);
        return TemplateV2ValidationResult.from(errors, warnings);
    }

    /**
     * Loads a persisted template, resolves normalized V2 (from content or V1 migration draft),
     * and returns a bounded plan explain with source summaries and V1/V2 diff notes.
     *
     * @param templateId persisted template id
     * @return migration plan explain block; never {@code null}
     * @throws IllegalArgumentException when the template is missing, content is empty, or V2 cannot be resolved
     */
    public MigrationPlanExplain explain(Long templateId) {
        Objects.requireNonNull(templateId, "templateId");
        TemplatePO entity = repository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Template '%s' does not exist", templateId)));
        if (StrKit.isBlank(entity.getContentYaml())) {
            throw new IllegalArgumentException(String.format("Template '%s' has empty content", templateId));
        }

        TemplateVO v1 = resolveV1ForExplain(entity);
        TemplateV2VO v2 = v2CompareResolver.resolveForCompare(entity);
        return planExplainService.explain(v1, v2);
    }

    /**
     * Runs a bounded in-memory preview for a persisted template.
     *
     * @param templateId persisted template id
     * @param maxRows    optional row cap; when {@code null} or non-positive, uses {@link DataGeneratorProperties#getPreviewMaxRows()}
     * @return preview schema, truncated rows, and warnings; never {@code null}
     * @throws IllegalArgumentException when the template is missing, content is empty, execution mode is not
     *                                  {@code IN_MEMORY}, or V2 cannot be resolved
     */
    public TemplateV2PreviewDTO preview(Long templateId, Integer maxRows) {
        Objects.requireNonNull(templateId, "templateId");
        int rowCap = resolvePreviewRowCap(maxRows);

        TemplatePO entity = repository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Template '%s' does not exist", templateId)));
        if (StrKit.isBlank(entity.getContentYaml())) {
            throw new IllegalArgumentException(String.format("Template '%s' has empty content", templateId));
        }

        TemplateV2VO v2 = v2CompareResolver.resolveForCompare(entity);
        var warnings = new ArrayList<String>();

        EffectiveExecutionPolicy effective = EffectiveExecutionPolicy.resolve(v2.getExecutionPolicy());
        String mode = effective.mode();
        if ("CHUNKED".equals(mode) || "STREAMING".equals(mode)) {
            throw new IllegalArgumentException(
                    "Preview supports IN_MEMORY execution only; template mode is " + mode);
        }

        TemplateV2VO runnable = prepareForPreview(v2);
        TemplateV2RunResult result = templateV2Runner.run(runnable);

        RowSchema schema = result.getSchema();
        List<Row> rows = result.getRows() == null ? List.of() : result.getRows();
        if (rows.size() > rowCap) {
            rows = List.copyOf(rows.subList(0, rowCap));
            warnings.add("Preview truncated result rows to " + rowCap);
        }

        return new TemplateV2PreviewDTO(schema, rows, warnings);
    }

    private int resolvePreviewRowCap(Integer maxRows) {
        if (maxRows != null && maxRows > 0) {
            return maxRows;
        }
        Integer configured = properties.getPreviewMaxRows();
        if (configured != null && configured > 0) {
            return configured;
        }
        return 100;
    }

    private static TemplateV2VO prepareForPreview(TemplateV2VO template) {
        TemplateV2VO copy = copyTemplate(template);
        ExecutionPolicyVO policy = copy.getExecutionPolicy();
        if (policy == null) {
            policy = new ExecutionPolicyVO();
            copy.setExecutionPolicy(policy);
        }
        // Preview materializes rows in memory regardless of stored policy hints.
        policy.setMode("IN_MEMORY");
        return copy;
    }

    private static TemplateV2VO copyTemplate(TemplateV2VO template) {
        TemplateV2VO copy = new TemplateV2VO();
        copy.setId(template.getId());
        copy.setInstanceId(template.getInstanceId());
        copy.setName(template.getName());
        copy.setGenerator(template.getGenerator());
        copy.setExecutionPolicy(template.getExecutionPolicy());
        copy.setSinkExecutionPolicy(template.getSinkExecutionPolicy());
        if (template.getSources() != null) {
            copy.setSources(new java.util.LinkedHashMap<>(template.getSources()));
        }
        copy.setTransformers(new ArrayList<>(template.getTransformers()));
        copy.setSinks(new ArrayList<>(template.getSinks()));
        return copy;
    }

    private TemplateVO resolveV1ForExplain(TemplatePO entity) {
        TemplateV2DraftVO v2Draft = tryParse(entity.getContentYaml(), TemplateV2DraftVO.class);
        TemplateVO v1Probe = tryParse(entity.getContentYaml(), TemplateVO.class);
        TemplateDefinitionKind kind = TemplateDefinitionDetector.detect(v1Probe, v2Draft);
        // V2-only persisted yaml cannot be loaded as V1; use probe when present for diff hints.
        if (kind == TemplateDefinitionKind.V2) {
            return v1Probe;
        }
        return templateV1Loader.load(entity);
    }

    private <T> T tryParse(String yaml, Class<T> clazz) {
        try {
            return yamlParser.parse(yaml, clazz);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static void appendChunkedExecutionShapeWarnings(TemplateV2VO normalized, List<String> warnings) {
        ExecutionPolicyVO policy = normalized.getExecutionPolicy();
        if (policy == null || StrKit.isBlank(policy.getMode())) {
            return;
        }
        if (!"CHUNKED".equals(policy.getMode().trim().toUpperCase(Locale.ROOT))) {
            return;
        }

        EffectiveExecutionPolicy effective = EffectiveExecutionPolicy.resolve(policy);
        try {
            ExecutionShape shape = ExecutionShapeClassifier.classify(normalized);
            if (shape == ExecutionShape.MATERIALIZATION_REQUIRED) {
                warnings.add(
                        "CHUNKED mode requested but SQL shape is MATERIALIZATION_REQUIRED — runtime may fall back or approximate");
            }
            if (shape == ExecutionShape.ROW_LOCAL) {
                warnings.add("CHUNKED row-local shape — suitable for large JDBC export path");
            }
            if (shape == ExecutionShape.BROADCAST_JOIN) {
                warnings.add("Broadcast join shape — verify dimension source maxRows/broadcastMaxRows ("
                        + effective.broadcastMaxRows()
                        + ")");
            }
        }
        catch (RuntimeException e) {
            warnings.add("Execution shape classification failed: " + e.getMessage());
        }
    }
}
