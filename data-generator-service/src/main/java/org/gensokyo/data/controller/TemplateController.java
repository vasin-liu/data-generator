/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.Templates;
import org.gensokyo.data.calcite.CalciteExecutionContext;
import org.gensokyo.data.calcite.CalciteSqlValidator;
import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.TemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.dto.QuerySourceCandidateSourceDTO;
import org.gensokyo.data.model.dto.QuerySourceCandidatePreflightDTO;
import org.gensokyo.data.model.dto.QuerySourceCandidatePreflightSummaryDTO;
import org.gensokyo.data.model.dto.QuerySourceMigrationAnalysisDTO;
import org.gensokyo.data.model.dto.QuerySourceTransformCandidateDTO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.qo.UpdateTemplateQO;
import org.gensokyo.data.model.vo.stage.ParamVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateDefinitionDetector;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.template.TemplateV2Validator;
import org.gensokyo.data.template.V1QuerySourceDraftConverter;
import org.gensokyo.data.template.V1QuerySourceMigrationWarningAnalyzer;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.io.FileKit;
import org.gensokyo.kit.io.IOKit;
import org.gensokyo.kit.security.Md5Kit;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板管理接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/9/20 , Version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/template")
@Validated
@RequiredArgsConstructor
public class TemplateController {
    private static final Pattern COLUMN_EQUALS_PARAM = Pattern.compile("(?i)([a-zA-Z_][\\w.]*)\\s*=\\s*:(\\w+)");
    private static final Pattern PARAM_EQUALS_COLUMN = Pattern.compile("(?i):(\\w+)\\s*=\\s*([a-zA-Z_][\\w.]*)");
    private static final List<String> STRUCTURAL_SCOPE_COLUMNS = List.of(
            "tenant_id",
            "org_id",
            "dept_id",
            "site_id",
            "project_id",
            "region_code",
            "area_code",
            "city_code",
            "province_code"
    );
    private static final List<String> STRUCTURAL_ENTITY_KEYS = List.of(
            "id",
            "code",
            "no",
            "key",
            "type",
            "version"
    );
    private static final Set<String> SOURCE_NAME_STOP_WORDS = Set.of(
            "lookup",
            "reader",
            "source",
            "query",
            "jdbc",
            "read",
            "stage",
            "input",
            "output",
            "rows",
            "row",
            "data"
    );

    private final TemplateRepository repository;
    private final YamlParser yamlParser;
    private final Templates templates;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final TemplateV2RuntimeRegistryProvider templateV2RuntimeRegistryProvider;

    @PostMapping("/updateById")
    public R<String> updateById(@Validated @RequestBody UpdateTemplateQO qo) {
        var po = repository.findById(qo.getId()).orElse(null);
        if (Objects.isNull(po)) {
            return R.fail(String.format("模板 '%s' 不存在", qo.getId()));
        }
        var parsed = parseTemplate(qo.getYaml());
        if (Objects.isNull(parsed)) {
            return R.fail("文件内容解析失败，请检查文件内容格式是否正确");
        }
        po.setName(parsed.name());
        po.setContentJson(parsed.contentJson());
        po.setContentYaml(qo.getYaml());
        repository.save(po);
        return R.ok(String.format("模板 '%s' 已更新", qo.getId()));
    }

    @PostMapping("/reloadAllFromFile")
    public R<String> reloadFromFile() {
        var list = repository.saveAllAndFlush(templates.reloadAll());
        return R.ok(String.format("所有模板重新加载完成，总共 %s 个文件", list.size()));
    }

    @PostMapping("/refreshV2Runtime")
    public R<String> refreshV2Runtime() {
        templateV2RuntimeRegistryProvider.refresh();
        return R.ok("V2 runtime registry refreshed");
    }

    @PostMapping("/uploadTemplate")
    public R<String> uploadTemplate(@NotNull @RequestParam("file") MultipartFile file) {
        try (var is = file.getInputStream()) {
            var content = IOKit.toString(is, StandardCharsets.UTF_8);
            var parsed = parseTemplate(content);
            if (Objects.isNull(parsed)) {
                return R.fail("文件内容解析失败，请检查文件内容格式是否正确");
            }
            var fileName = file.getName();
            var id = RandomKit.snowFlake().nextId();
            var prefix = "upload";
            if (StrKit.isBlank(fileName)) {
                fileName = prefix + File.separator + id;
            } else {
                fileName = prefix + File.separator + fileName;
            }
            var po = new TemplatePO();
            po.setId(id);
            po.setName(parsed.name());
            po.setFileExt(FileKit.getExtension(fileName));
            po.setFileName(fileName);
            po.setPathMd5(Md5Kit.encrypt(fileName));
            po.setContentYaml(content);
            po.setContentJson(parsed.contentJson());
            repository.save(po);
            return R.ok("文件上传成功");
        } catch (Exception e) {
            throw new DataGeneratorException("模板文件上传失败", e);
        }
    }

    @GetMapping("/previewQuerySourceV2ById/{templateId}")
    public R<TemplateV2DraftVO> previewQuerySourceV2ById(@NotNull @PathVariable Long templateId) {
        var entity = repository.findById(templateId).orElse(null);
        if (Objects.isNull(entity)) {
            return R.fail(String.format("Template '%s' does not exist", templateId));
        }

        TemplateV2DraftVO draft;
        try {
            draft = buildQuerySourceDraft(entity);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
        if (Objects.isNull(draft) || CollectKit.isEmpty(draft.getSources())) {
            return R.fail(String.format("Template '%s' has no database-backed sources that can be converted into QuerySourceVO", templateId));
        }
        return R.ok("Preview generated", draft);
    }

    @GetMapping("/analyzeQuerySourceV2ById/{templateId}")
    public R<QuerySourceMigrationAnalysisDTO> analyzeQuerySourceV2ById(@NotNull @PathVariable Long templateId) {
        var entity = repository.findById(templateId).orElse(null);
        if (Objects.isNull(entity)) {
            return R.fail(String.format("Template '%s' does not exist", templateId));
        }

        TemplateVO v1;
        TemplateV2DraftVO draft;
        try {
            v1 = buildV1Template(entity);
            draft = V1QuerySourceDraftConverter.convert(v1);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
        if (Objects.isNull(draft) || CollectKit.isEmpty(draft.getSources())) {
            return R.fail(String.format("Template '%s' has no database-backed sources that can be converted into QuerySourceVO", templateId));
        }
        return R.ok("Analysis generated", analyzeDraft(v1, draft));
    }

    @PostMapping("/applyQuerySourceCandidateById/{templateId}/{scenario}")
    public R<TemplateV2DraftVO> applyQuerySourceCandidateById(@NotNull @PathVariable Long templateId,
                                                              @NotNull @PathVariable String scenario) {
        var entity = repository.findById(templateId).orElse(null);
        if (Objects.isNull(entity)) {
            return R.fail(String.format("Template '%s' does not exist", templateId));
        }

        TemplateV2DraftVO draft;
        try {
            draft = buildQuerySourceDraft(entity);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
        if (Objects.isNull(draft) || CollectKit.isEmpty(draft.getSources())) {
            return R.fail(String.format("Template '%s' has no database-backed sources that can be converted into QuerySourceVO", templateId));
        }

        var candidate = buildTransformCandidates(draft).stream()
                .filter(it -> it.getScenario().equalsIgnoreCase(scenario))
                .findFirst()
                .orElse(null);
        if (Objects.isNull(candidate)) {
            return R.fail(String.format("No transform candidate scenario '%s' exists for template '%s'", scenario, templateId));
        }

        draft.setTransform(candidate.getTransform());
        entity.setName(draft.getName());
        entity.setContentYaml(yamlParser.dump(draft));
        entity.setContentJson(TemplateJsonCodec.write(draft));
        repository.saveAndFlush(entity);
        return R.ok("Transform candidate applied", draft);
    }

    @PostMapping("/applyQuerySourceCandidateAndNormalizeById/{templateId}/{scenario}")
    public R<TemplateV2DraftVO> applyQuerySourceCandidateAndNormalizeById(@NotNull @PathVariable Long templateId,
                                                                          @NotNull @PathVariable String scenario) {
        var entity = repository.findById(templateId).orElse(null);
        if (Objects.isNull(entity)) {
            return R.fail(String.format("Template '%s' does not exist", templateId));
        }

        TemplateV2DraftVO draft;
        try {
            draft = buildQuerySourceDraft(entity);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
        if (Objects.isNull(draft) || CollectKit.isEmpty(draft.getSources())) {
            return R.fail(String.format("Template '%s' has no database-backed sources that can be converted into QuerySourceVO", templateId));
        }

        var candidate = buildTransformCandidates(draft).stream()
                .filter(it -> it.getScenario().equalsIgnoreCase(scenario))
                .findFirst()
                .orElse(null);
        if (Objects.isNull(candidate)) {
            return R.fail(String.format("No transform candidate scenario '%s' exists for template '%s'", scenario, templateId));
        }

        draft.setTransform(candidate.getTransform());
        var preflight = preflightCandidate(draft, candidate);
        if (!preflight.isNormalized()) {
            return R.fail(String.format("Candidate '%s' could not be normalized into a valid V2 template: %s",
                    scenario, preflight.getMessage()));
        }
        if (!preflight.isCalciteValid()) {
            return R.fail(String.format("Candidate '%s' failed Calcite validation: %s",
                    scenario, preflight.getMessage()));
        }

        entity.setName(draft.getName());
        entity.setContentYaml(yamlParser.dump(draft));
        entity.setContentJson(TemplateJsonCodec.write(draft));
        repository.saveAndFlush(entity);
        return R.ok("Transform candidate applied and normalized", draft);
    }

    @GetMapping("/preflightQuerySourceCandidateById/{templateId}/{scenario}")
    public R<QuerySourceCandidatePreflightDTO> preflightQuerySourceCandidateById(@NotNull @PathVariable Long templateId,
                                                                                 @NotNull @PathVariable String scenario) {
        var entity = repository.findById(templateId).orElse(null);
        if (Objects.isNull(entity)) {
            return R.fail(String.format("Template '%s' does not exist", templateId));
        }

        TemplateV2DraftVO draft;
        try {
            draft = buildQuerySourceDraft(entity);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
        if (Objects.isNull(draft) || CollectKit.isEmpty(draft.getSources())) {
            return R.fail(String.format("Template '%s' has no database-backed sources that can be converted into QuerySourceVO", templateId));
        }

        var candidate = findTransformCandidate(draft, scenario);
        if (Objects.isNull(candidate)) {
            return R.fail(String.format("No transform candidate scenario '%s' exists for template '%s'", scenario, templateId));
        }

        draft.setTransform(candidate.getTransform());
        return R.ok("Candidate preflight completed", preflightCandidate(draft, candidate));
    }

    @PostMapping("/migrateQuerySourceV2ById/{templateId}")
    public R<TemplateV2DraftVO> migrateQuerySourceV2ById(@NotNull @PathVariable Long templateId) {
        var entity = repository.findById(templateId).orElse(null);
        if (Objects.isNull(entity)) {
            return R.fail(String.format("Template '%s' does not exist", templateId));
        }

        TemplateV2DraftVO draft;
        try {
            draft = buildQuerySourceDraft(entity);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
        if (Objects.isNull(draft) || CollectKit.isEmpty(draft.getSources())) {
            return R.fail(String.format("Template '%s' has no database-backed sources that can be converted into QuerySourceVO", templateId));
        }

        entity.setName(draft.getName());
        entity.setContentYaml(yamlParser.dump(draft));
        entity.setContentJson(TemplateJsonCodec.write(draft));
        repository.saveAndFlush(entity);
        return R.ok("Migration completed", draft);
    }

    private ParsedTemplate parseTemplate(String yaml) {
        var v2 = tryParse(yaml, TemplateV2DraftVO.class);
        var v1 = tryParse(yaml, TemplateVO.class);
        var kind = TemplateDefinitionDetector.detect(v1, v2);
        if (kind == TemplateDefinitionKind.V2 && Objects.nonNull(v2)) {
            return new ParsedTemplate(v2.getName(), TemplateJsonCodec.write(v2));
        }
        if (Objects.nonNull(v1)) {
            return new ParsedTemplate(v1.getName(), TemplateJsonCodec.write(v1));
        }
        return null;
    }

    private <T> T tryParse(String yaml, Class<T> clazz) {
        try {
            return yamlParser.parse(yaml, clazz);
        } catch (Exception ignored) {
            return null;
        }
    }

    private TemplateV2DraftVO buildQuerySourceDraft(TemplatePO entity) {
        return V1QuerySourceDraftConverter.convert(buildV1Template(entity));
    }

    private TemplateVO buildV1Template(TemplatePO entity) {
        var v2 = tryParse(entity.getContentYaml(), TemplateV2DraftVO.class);
        var v1 = tryParse(entity.getContentYaml(), TemplateVO.class);
        var kind = TemplateDefinitionDetector.detect(v1, v2);
        if (kind == TemplateDefinitionKind.V2 && Objects.nonNull(v2)) {
            throw new IllegalArgumentException(String.format("Template '%s' is already a V2 template", entity.getId()));
        }
        if (Objects.isNull(v1)) {
            throw new IllegalArgumentException(String.format("Template '%s' is not a valid V1 template", entity.getId()));
        }
        v1.setId(entity.getId());
        if (StrKit.isBlank(v1.getName())) {
            v1.setName(entity.getName());
        }
        return v1;
    }

    private QuerySourceMigrationAnalysisDTO analyzeDraft(TemplateVO v1, TemplateV2DraftVO draft) {
        List<String> warnings = new ArrayList<>(V1QuerySourceMigrationWarningAnalyzer.analyze(v1));
        List<QuerySourceTransformCandidateDTO> candidates = enrichCandidatesWithPreflight(draft, buildTransformCandidates(draft));
        boolean executable = Objects.nonNull(draft.getTransform()) && Objects.nonNull(draft.getSink());
        String recommendedScenario = recommendedScenario(candidates);

        if (draft.getSources().size() > 1 && Objects.isNull(draft.getTransform())) {
            warnings.add("Multiple QuerySourceVO sources were detected. SQL transform was not generated automatically.");
        }
        if (Objects.isNull(draft.getSink())) {
            warnings.add("No sink was migrated from the V1 template. Add at least one sink before execution.");
            executable = false;
        }
        if (Objects.isNull(draft.getTransform())) {
            warnings.add("No SQL transform is present in the migrated draft.");
            executable = false;
        }
        return new QuerySourceMigrationAnalysisDTO(draft, executable, recommendedScenario, warnings, candidates);
    }

    private QuerySourceCandidatePreflightDTO preflightCandidate(TemplateV2DraftVO draft,
                                                                QuerySourceTransformCandidateDTO candidate) {
        try {
            TemplateV2Validator.validate(TemplateV2Normalizer.normalize(draft));
        } catch (Exception e) {
            return new QuerySourceCandidatePreflightDTO(
                    draft,
                    candidate,
                    false,
                    false,
                    "Normalization/validation failed: " + e.getMessage()
            );
        }

        try {
            validateCandidateWithCalcite(draft);
            return new QuerySourceCandidatePreflightDTO(
                    draft,
                    candidate,
                    true,
                    true,
                    "Calcite validation passed"
            );
        } catch (Exception e) {
            return new QuerySourceCandidatePreflightDTO(
                    draft,
                    candidate,
                    true,
                    false,
                    "Calcite validation failed: " + e.getMessage()
            );
        }
    }

    private List<QuerySourceTransformCandidateDTO> enrichCandidatesWithPreflight(TemplateV2DraftVO draft,
                                                                                 List<QuerySourceTransformCandidateDTO> candidates) {
        List<QuerySourceTransformCandidateDTO> enriched = new ArrayList<>();
        for (QuerySourceTransformCandidateDTO candidate : candidates) {
            TemplateV2DraftVO candidateDraft = buildCandidateDraft(draft, candidate);
            QuerySourceCandidatePreflightDTO preflight = preflightCandidate(candidateDraft, candidate);
            candidate.setPreflight(new QuerySourceCandidatePreflightSummaryDTO(
                    preflight.isNormalized(),
                    preflight.isCalciteValid(),
                    preflight.getMessage()
            ));
            enriched.add(candidate);
        }
        return enriched;
    }

    private void validateCandidateWithCalcite(TemplateV2DraftVO draft) {
        CalciteExecutionContext context = new CalciteExecutionContext();
        for (var entry : draft.getSources().entrySet()) {
            context.addSource(createSource(entry.getKey(), entry.getValue()));
        }
        if (!(draft.getTransform() instanceof SqlTransformVO sqlTransform)) {
            throw new IllegalArgumentException("QuerySource candidate preflight currently supports SQL transform only");
        }
        var result = new CalciteSqlValidator().validate(sqlTransform.getSql(), context);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getMessage());
        }
    }

    private RowSource createSource(String name, SourceVO source) {
        return templateV2RuntimeRegistryProvider.current().createSource(name, source);
    }

    private List<QuerySourceTransformCandidateDTO> buildTransformCandidates(TemplateV2DraftVO draft) {
        List<QuerySourceTransformCandidateDTO> candidates = new ArrayList<>();
        List<String> sourceOrder = new ArrayList<>(draft.getSources().keySet());
        String primarySource = sourceOrder.getFirst();
        List<QuerySourceCandidateSourceDTO> singleSourceMetadata = List.of(sourceMetadata(draft, primarySource, "s0"));
        List<String> singleProjection = List.of("s0.*");

        candidates.add(new QuerySourceTransformCandidateDTO(
                "single-source-baseline",
                primarySource,
                List.of(primarySource),
                List.of("s0"),
                singleProjection,
                List.of(),
                singleSourceMetadata,
                sqlTransform("SELECT " + String.join(", ", singleProjection) + " FROM " + primarySource + " s0",
                        "candidate_single_source_baseline"),
                null
        ));

        if (sourceOrder.size() > 1) {
            LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
            for (int i = 0; i < sourceOrder.size(); i++) {
                aliases.put(sourceOrder.get(i), "s" + i);
            }
            Map<String, List<String>> sourceColumns = resolveSourceColumns(draft, sourceOrder);
            List<QuerySourceCandidateSourceDTO> sourceMetadata = multiSourceMetadata(draft, sourceOrder, aliases);
            List<String> projectionSkeleton = projectionSkeleton(sourceOrder, aliases, sourceColumns);
            if (hasParameterizedSecondarySource(sourceMetadata)) {
                candidates.add(new QuerySourceTransformCandidateDTO(
                        "multi-source-lookup-skeleton",
                        primarySource,
                        sourceOrder,
                        new ArrayList<>(aliases.values()),
                        projectionSkeleton,
                        lookupJoinHints(draft, sourceOrder, aliases, sourceMetadata, sourceColumns),
                        sourceMetadata,
                        sqlTransform(buildMultiSourceSql(draft, sourceOrder, aliases, projectionSkeleton, sourceMetadata, sourceColumns, true),
                                "candidate_multi_source_lookup_skeleton"),
                        null
                ));
            }
            candidates.add(new QuerySourceTransformCandidateDTO(
                    "multi-source-join-skeleton",
                    primarySource,
                    sourceOrder,
                    new ArrayList<>(aliases.values()),
                    projectionSkeleton,
                    genericJoinHints(draft, sourceOrder, aliases, sourceMetadata, sourceColumns),
                    sourceMetadata,
                    sqlTransform(buildMultiSourceSql(draft, sourceOrder, aliases, projectionSkeleton, sourceMetadata, sourceColumns, false),
                            "candidate_multi_source_join_skeleton"),
                    null
            ));
        }
        return candidates;
    }

    private QuerySourceTransformCandidateDTO findTransformCandidate(TemplateV2DraftVO draft, String scenario) {
        return buildTransformCandidates(draft).stream()
                .filter(it -> it.getScenario().equalsIgnoreCase(scenario))
                .findFirst()
                .orElse(null);
    }

    private TemplateV2DraftVO buildCandidateDraft(TemplateV2DraftVO draft, QuerySourceTransformCandidateDTO candidate) {
        TemplateV2DraftVO candidateDraft = new TemplateV2DraftVO();
        candidateDraft.setId(draft.getId());
        candidateDraft.setInstanceId(draft.getInstanceId());
        candidateDraft.setName(draft.getName());
        candidateDraft.setGenerator(draft.getGenerator());
        candidateDraft.setSources(new LinkedHashMap<>(draft.getSources()));
        candidateDraft.setSinkExecutionPolicy(draft.getSinkExecutionPolicy());
        candidateDraft.setSink(draft.getSink());
        candidateDraft.setSinks(draft.getSinks());
        candidateDraft.setTransform(candidate.getTransform());
        candidateDraft.setTransformers(draft.getTransformers());
        return candidateDraft;
    }

    private String recommendedScenario(List<QuerySourceTransformCandidateDTO> candidates) {
        QuerySourceTransformCandidateDTO preferred = preferredCandidate(candidates);
        if (preferred != null) {
            return preferred.getScenario();
        }
        for (QuerySourceTransformCandidateDTO candidate : candidates) {
            if (candidate.getPreflight() != null
                    && candidate.getPreflight().isNormalized()
                    && candidate.getPreflight().isCalciteValid()) {
                return candidate.getScenario();
            }
        }
        for (QuerySourceTransformCandidateDTO candidate : candidates) {
            if (candidate.getPreflight() != null && candidate.getPreflight().isNormalized()) {
                return candidate.getScenario();
            }
        }
        return candidates.isEmpty() ? null : candidates.getFirst().getScenario();
    }

    private QuerySourceTransformCandidateDTO preferredCandidate(List<QuerySourceTransformCandidateDTO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        boolean hasMultiSource = candidates.stream().anyMatch(it -> it.getSourceOrder() != null && it.getSourceOrder().size() > 1);
        if (!hasMultiSource) {
            return null;
        }
        QuerySourceTransformCandidateDTO lookup = firstValidScenario(candidates, "multi-source-lookup-skeleton");
        if (lookup != null) {
            return lookup;
        }
        QuerySourceTransformCandidateDTO join = firstValidScenario(candidates, "multi-source-join-skeleton");
        if (join != null) {
            return join;
        }
        return null;
    }

    private QuerySourceTransformCandidateDTO firstValidScenario(List<QuerySourceTransformCandidateDTO> candidates, String scenario) {
        for (QuerySourceTransformCandidateDTO candidate : candidates) {
            if (!scenario.equals(candidate.getScenario())) {
                continue;
            }
            if (candidate.getPreflight() != null
                    && candidate.getPreflight().isNormalized()
                    && candidate.getPreflight().isCalciteValid()) {
                return candidate;
            }
        }
        for (QuerySourceTransformCandidateDTO candidate : candidates) {
            if (!scenario.equals(candidate.getScenario())) {
                continue;
            }
            if (candidate.getPreflight() != null && candidate.getPreflight().isNormalized()) {
                return candidate;
            }
        }
        return null;
    }

    private QuerySourceCandidateSourceDTO sourceMetadata(TemplateV2DraftVO draft, String sourceName, String alias) {
        if (!(draft.getSources().get(sourceName) instanceof QuerySourceVO source)) {
            return new QuerySourceCandidateSourceDTO(sourceName, alias, null, null, false, false);
        }
        boolean parameterized = CollectKit.isNotEmpty(source.getParams());
        boolean paged = Objects.nonNull(source.getPageIndex())
                || Objects.nonNull(source.getPageSize())
                || Objects.nonNull(source.getMaxRows());
        return new QuerySourceCandidateSourceDTO(
                sourceName,
                alias,
                source.getDataSourceId(),
                source.getSql(),
                parameterized,
                paged
        );
    }

    private List<QuerySourceCandidateSourceDTO> multiSourceMetadata(TemplateV2DraftVO draft,
                                                                    List<String> sourceOrder,
                                                                    LinkedHashMap<String, String> aliases) {
        List<QuerySourceCandidateSourceDTO> sourceMetadata = new ArrayList<>();
        for (String sourceName : sourceOrder) {
            sourceMetadata.add(sourceMetadata(draft, sourceName, aliases.get(sourceName)));
        }
        return sourceMetadata;
    }

    private List<String> projectionSkeleton(List<String> sourceOrder,
                                           LinkedHashMap<String, String> aliases,
                                           Map<String, List<String>> sourceColumns) {
        List<String> projectionSkeleton = new ArrayList<>();
        Map<String, Integer> aliasCounters = new LinkedHashMap<>();
        for (String sourceName : sourceOrder) {
            String alias = aliases.get(sourceName);
            List<String> columns = sourceColumns.getOrDefault(sourceName, List.of());
            if (CollectKit.isEmpty(columns)) {
                projectionSkeleton.add(alias + ".*");
                continue;
            }
            for (String column : columns) {
                projectionSkeleton.add(alias
                        + "."
                        + column
                        + " AS "
                        + uniqueProjectionAlias(sourceName, alias, column, aliasCounters));
            }
        }
        return projectionSkeleton;
    }

    private boolean hasParameterizedSecondarySource(List<QuerySourceCandidateSourceDTO> sourceMetadata) {
        if (sourceMetadata.size() <= 1) {
            return false;
        }
        for (int i = 1; i < sourceMetadata.size(); i++) {
            if (sourceMetadata.get(i).isParameterized()) {
                return true;
            }
        }
        return false;
    }

    private List<String> genericJoinHints(TemplateV2DraftVO draft,
                                          List<String> sourceOrder,
                                          LinkedHashMap<String, String> aliases,
                                          List<QuerySourceCandidateSourceDTO> sourceMetadata,
                                          Map<String, List<String>> sourceColumns) {
        List<String> joinHints = new ArrayList<>();
        for (int i = 1; i < sourceOrder.size(); i++) {
            InferredJoinCondition inferred = inferJoinCondition(draft, sourceOrder, aliases, sourceMetadata, sourceColumns, i);
            if (inferred != null) {
                joinHints.add("Review inferred join condition "
                        + inferred.predicate()
                        + " for source '"
                        + sourceMetadata.get(i).getSourceName()
                        + "'.");
                continue;
            }
            joinHints.add("Replace ON 1 = 1 with a business join condition between "
                    + aliases.get(sourceOrder.get(i - 1)) + " and " + aliases.get(sourceOrder.get(i)));
        }
        return joinHints;
    }

    private List<String> lookupJoinHints(TemplateV2DraftVO draft,
                                         List<String> sourceOrder,
                                         LinkedHashMap<String, String> aliases,
                                         List<QuerySourceCandidateSourceDTO> sourceMetadata,
                                         Map<String, List<String>> sourceColumns) {
        List<String> joinHints = new ArrayList<>();
        for (int i = 1; i < sourceOrder.size(); i++) {
            QuerySourceCandidateSourceDTO source = sourceMetadata.get(i);
            if (source.isParameterized()) {
                InferredJoinCondition inferred = inferJoinCondition(draft, sourceOrder, aliases, sourceMetadata, sourceColumns, i);
                if (inferred != null) {
                    joinHints.add("Source '" + source.getSourceName() + "' expects params "
                            + parameterNames(draft, source.getSourceName())
                            + ". Inferred join "
                            + inferred.predicate()
                            + "; if the source query is still row-parameterized, widen it into a relational lookup before finalizing.");
                    continue;
                }
                joinHints.add("Source '" + source.getSourceName() + "' expects params "
                        + parameterNames(draft, source.getSourceName())
                        + ". Replace ON 1 = 1 with a business join that derives these values from upstream rows.");
            } else {
                joinHints.add("Replace ON 1 = 1 with a business lookup condition between "
                        + aliases.get(sourceOrder.get(i - 1)) + " and " + aliases.get(sourceOrder.get(i)));
            }
        }
        return joinHints;
    }

    private List<String> parameterNames(TemplateV2DraftVO draft, String sourceName) {
        if (!(draft.getSources().get(sourceName) instanceof QuerySourceVO source) || source.getParams() == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (ParamVO param : source.getParams()) {
            if (param != null && StrKit.isNotBlank(param.getName())) {
                names.add(param.getName());
            }
        }
        return names;
    }

    private String buildMultiSourceSql(TemplateV2DraftVO draft,
                                       List<String> sourceOrder,
                                       LinkedHashMap<String, String> aliases,
                                       List<String> projectionSkeleton,
                                       List<QuerySourceCandidateSourceDTO> sourceMetadata,
                                       Map<String, List<String>> sourceColumns,
                                       boolean lookupMode) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(String.join(", ", projectionSkeleton)).append(" FROM ");
        for (int i = 0; i < sourceOrder.size(); i++) {
            String sourceName = sourceOrder.get(i);
            String alias = aliases.get(sourceName);
            if (i == 0) {
                sql.append(sourceName).append(' ').append(alias);
                continue;
            }
            String joinType = lookupMode && sourceMetadata.get(i).isParameterized() ? " LEFT JOIN " : " JOIN ";
            InferredJoinCondition inferred = inferJoinCondition(draft, sourceOrder, aliases, sourceMetadata, sourceColumns, i);
            sql.append(joinType)
                    .append(sourceName)
                    .append(' ')
                    .append(alias)
                    .append(" ON ")
                    .append(inferred == null ? "1 = 1" : inferred.predicate());
        }
        return sql.toString();
    }

    private Map<String, List<String>> resolveSourceColumns(TemplateV2DraftVO draft, List<String> sourceOrder) {
        Map<String, List<String>> sourceColumns = new LinkedHashMap<>();
        for (String sourceName : sourceOrder) {
            sourceColumns.put(sourceName, resolveSourceColumns(draft, sourceName));
        }
        return sourceColumns;
    }

    private List<String> resolveSourceColumns(TemplateV2DraftVO draft, String sourceName) {
        SourceVO source = draft.getSources().get(sourceName);
        if (source == null) {
            return List.of();
        }
        if (source instanceof QuerySourceVO querySource
                && querySource.getSchema() != null
                && CollectKit.isNotEmpty(querySource.getSchema().getColumns())) {
            List<String> columns = new ArrayList<>();
            querySource.getSchema().getColumns().forEach(column -> columns.add(column.getName()));
            return columns;
        }
        try {
            RowSource rowSource = createSource(sourceName, source);
            if (rowSource.schema() == null || CollectKit.isEmpty(rowSource.schema().getColumns())) {
                return List.of();
            }
            List<String> columns = new ArrayList<>();
            rowSource.schema().getColumns().forEach(column -> columns.add(column.getName()));
            return columns;
        } catch (Exception e) {
            log.debug("Failed to resolve source columns for candidate inference: {}", sourceName, e);
            return List.of();
        }
    }

    private InferredJoinCondition inferJoinCondition(TemplateV2DraftVO draft,
                                                     List<String> sourceOrder,
                                                     LinkedHashMap<String, String> aliases,
                                                     List<QuerySourceCandidateSourceDTO> sourceMetadata,
                                                     Map<String, List<String>> sourceColumns,
                                                     int currentIndex) {
        if (currentIndex <= 0 || currentIndex >= sourceOrder.size()) {
            return null;
        }
        String currentSourceName = sourceOrder.get(currentIndex);
        List<String> currentColumns = sourceColumns.getOrDefault(currentSourceName, List.of());
        LinkedHashSet<String> predicates = new LinkedHashSet<>();
        List<String> params = parameterNames(draft, currentSourceName);
        if (CollectKit.isNotEmpty(params)) {
            Map<String, String> paramColumns = extractParameterColumns(sourceMetadata.get(currentIndex).getSql());
            for (String paramName : params) {
                SourceColumnRef upstream = findUpstreamColumn(paramName, sourceOrder, currentIndex, aliases, sourceColumns);
                String currentColumn = findCurrentColumn(paramName, paramColumns, currentColumns);
                if (upstream == null || StrKit.isBlank(currentColumn)) {
                    continue;
                }
                predicates.add(upstream.alias() + "." + upstream.column() + " = " + aliases.get(currentSourceName) + "." + currentColumn);
            }
        }
        if (predicates.isEmpty()) {
            predicates.addAll(inferStructuralJoinPredicates(sourceOrder, currentIndex, aliases, sourceColumns));
        }
        if (predicates.isEmpty()) {
            return null;
        }
        return new InferredJoinCondition(String.join(" AND ", predicates));
    }

    private List<String> inferStructuralJoinPredicates(List<String> sourceOrder,
                                                       int currentIndex,
                                                       LinkedHashMap<String, String> aliases,
                                                       Map<String, List<String>> sourceColumns) {
        String currentSourceName = sourceOrder.get(currentIndex);
        List<String> currentColumns = sourceColumns.getOrDefault(currentSourceName, List.of());
        if (CollectKit.isEmpty(currentColumns)) {
            return List.of();
        }
        List<String> bestPredicates = List.of();
        for (int i = currentIndex - 1; i >= 0; i--) {
            String upstreamSourceName = sourceOrder.get(i);
            List<String> upstreamColumns = sourceColumns.getOrDefault(upstreamSourceName, List.of());
            if (CollectKit.isEmpty(upstreamColumns)) {
                continue;
            }
            List<String> predicates = structuralJoinPredicates(
                    currentSourceName,
                    currentColumns,
                    upstreamColumns,
                    aliases.get(upstreamSourceName),
                    aliases.get(currentSourceName)
            );
            if (predicates.size() > bestPredicates.size()) {
                bestPredicates = predicates;
            }
        }
        return bestPredicates;
    }

    private List<String> structuralJoinPredicates(String currentSourceName,
                                                  List<String> currentColumns,
                                                  List<String> upstreamColumns,
                                                  String upstreamAlias,
                                                  String currentAlias) {
        LinkedHashSet<String> predicates = new LinkedHashSet<>();
        predicates.addAll(foreignKeyPredicates(currentSourceName, currentColumns, upstreamColumns, upstreamAlias, currentAlias));
        predicates.addAll(sharedScopePredicates(currentColumns, upstreamColumns, upstreamAlias, currentAlias));
        return new ArrayList<>(predicates);
    }

    private List<String> foreignKeyPredicates(String currentSourceName,
                                              List<String> currentColumns,
                                              List<String> upstreamColumns,
                                              String upstreamAlias,
                                              String currentAlias) {
        LinkedHashSet<String> predicates = new LinkedHashSet<>();
        for (String currentKey : currentStructuralKeys(currentColumns)) {
            String currentColumn = findMatchingColumn(currentColumns, List.of(currentKey));
            if (StrKit.isBlank(currentColumn)) {
                continue;
            }
            for (String token : sourceEntityTokens(currentSourceName)) {
                String upstreamColumn = findMatchingColumn(upstreamColumns, foreignKeyCandidates(token, currentKey));
                if (StrKit.isBlank(upstreamColumn)) {
                    continue;
                }
                predicates.add(upstreamAlias + "." + upstreamColumn + " = " + currentAlias + "." + currentColumn);
            }
        }
        return new ArrayList<>(predicates);
    }

    private List<String> sharedScopePredicates(List<String> currentColumns,
                                               List<String> upstreamColumns,
                                               String upstreamAlias,
                                               String currentAlias) {
        List<String> predicates = new ArrayList<>();
        for (String scopeColumn : STRUCTURAL_SCOPE_COLUMNS) {
            String upstreamColumn = findMatchingColumn(upstreamColumns, List.of(scopeColumn));
            String currentColumn = findMatchingColumn(currentColumns, List.of(scopeColumn));
            if (StrKit.isBlank(upstreamColumn) || StrKit.isBlank(currentColumn)) {
                continue;
            }
            predicates.add(upstreamAlias + "." + upstreamColumn + " = " + currentAlias + "." + currentColumn);
        }
        return predicates;
    }

    private List<String> currentStructuralKeys(List<String> currentColumns) {
        List<String> keys = new ArrayList<>();
        for (String key : STRUCTURAL_ENTITY_KEYS) {
            addCurrentStructuralKey(keys, currentColumns, key);
        }
        return keys;
    }

    private void addCurrentStructuralKey(List<String> keys, List<String> currentColumns, String key) {
        if (containsColumn(currentColumns, key) && !keys.contains(key)) {
            keys.add(key);
        }
    }

    private List<String> sourceEntityTokens(String sourceName) {
        if (StrKit.isBlank(sourceName)) {
            return List.of();
        }
        String normalized = toSnakeCase(sourceName)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_");
        String[] rawTokens = normalized.split("_");
        List<String> tokens = new ArrayList<>();
        for (String rawToken : rawTokens) {
            if (StrKit.isBlank(rawToken) || SOURCE_NAME_STOP_WORDS.contains(rawToken) || rawToken.chars().allMatch(Character::isDigit)) {
                continue;
            }
            addCandidate(tokens, rawToken);
        }
        if (tokens.isEmpty()) {
            addCandidate(tokens, normalized);
        }
        return tokens;
    }

    private List<String> foreignKeyCandidates(String token, String currentKey) {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, token + "_" + currentKey);
        addCandidate(candidates, token + currentKey);
        addCandidate(candidates, toSnakeCase(token + currentKey.substring(0, 1).toUpperCase(Locale.ROOT) + currentKey.substring(1)));
        return candidates;
    }

    private SourceColumnRef findUpstreamColumn(String paramName,
                                               List<String> sourceOrder,
                                               int currentIndex,
                                               LinkedHashMap<String, String> aliases,
                                               Map<String, List<String>> sourceColumns) {
        List<String> candidates = candidateColumnNames(paramName);
        for (int i = currentIndex - 1; i >= 0; i--) {
            String sourceName = sourceOrder.get(i);
            String column = findMatchingColumn(sourceColumns.getOrDefault(sourceName, List.of()), candidates);
            if (StrKit.isNotBlank(column)) {
                return new SourceColumnRef(sourceName, aliases.get(sourceName), column);
            }
        }
        return null;
    }

    private String findCurrentColumn(String paramName,
                                     Map<String, String> paramColumns,
                                     List<String> currentColumns) {
        String hinted = paramColumns.get(paramName);
        if (StrKit.isNotBlank(hinted)) {
            return hinted;
        }
        String matched = findMatchingColumn(currentColumns, candidateColumnNames(paramName));
        if (StrKit.isNotBlank(matched)) {
            return matched;
        }
        if (paramName != null
                && paramName.toLowerCase(Locale.ROOT).endsWith("id")
                && containsColumn(currentColumns, "id")) {
            return "id";
        }
        return null;
    }

    private Map<String, String> extractParameterColumns(String sql) {
        Map<String, String> columns = new LinkedHashMap<>();
        if (StrKit.isBlank(sql)) {
            return columns;
        }
        collectParameterColumns(columns, COLUMN_EQUALS_PARAM.matcher(sql), false);
        collectParameterColumns(columns, PARAM_EQUALS_COLUMN.matcher(sql), true);
        return columns;
    }

    private void collectParameterColumns(Map<String, String> columns, Matcher matcher, boolean reversed) {
        while (matcher.find()) {
            String column = unqualifyIdentifier(reversed ? matcher.group(2) : matcher.group(1));
            String param = reversed ? matcher.group(1) : matcher.group(2);
            if (StrKit.isNotBlank(column) && StrKit.isNotBlank(param)) {
                columns.putIfAbsent(param, column);
            }
        }
    }

    private String unqualifyIdentifier(String identifier) {
        if (StrKit.isBlank(identifier)) {
            return null;
        }
        String normalized = identifier.trim();
        int index = normalized.lastIndexOf('.');
        if (index >= 0 && index + 1 < normalized.length()) {
            normalized = normalized.substring(index + 1);
        }
        return normalized.replace("\"", "")
                .replace("`", "")
                .replace("[", "")
                .replace("]", "");
    }

    private List<String> candidateColumnNames(String paramName) {
        if (StrKit.isBlank(paramName)) {
            return List.of();
        }
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, paramName);
        addCandidate(candidates, toSnakeCase(paramName));
        addCandidate(candidates, normalizeIdentifier(paramName));
        String lower = paramName.toLowerCase(Locale.ROOT);
        if (lower.endsWith("id") && paramName.length() > 2) {
            String base = paramName.substring(0, paramName.length() - 2);
            addCandidate(candidates, base);
            addCandidate(candidates, toSnakeCase(base));
            addCandidate(candidates, normalizeIdentifier(base));
        }
        return candidates;
    }

    private void addCandidate(List<String> candidates, String candidate) {
        if (StrKit.isBlank(candidate)) {
            return;
        }
        for (String existing : candidates) {
            if (existing.equalsIgnoreCase(candidate)) {
                return;
            }
        }
        candidates.add(candidate);
    }

    private String findMatchingColumn(List<String> columns, List<String> candidates) {
        if (CollectKit.isEmpty(columns) || CollectKit.isEmpty(candidates)) {
            return null;
        }
        for (String candidate : candidates) {
            for (String column : columns) {
                if (column.equalsIgnoreCase(candidate)) {
                    return column;
                }
            }
        }
        for (String candidate : candidates) {
            String normalizedCandidate = normalizeIdentifier(candidate);
            for (String column : columns) {
                if (normalizeIdentifier(column).equals(normalizedCandidate)) {
                    return column;
                }
            }
        }
        return null;
    }

    private boolean containsColumn(List<String> columns, String candidate) {
        return StrKit.isNotBlank(findMatchingColumn(columns, List.of(candidate)));
    }

    private String uniqueProjectionAlias(String sourceName,
                                         String sqlAlias,
                                         String column,
                                         Map<String, Integer> aliasCounters) {
        String sourceToken = sanitizedProjectionToken(sourceName);
        if (StrKit.isBlank(sourceToken)) {
            sourceToken = sanitizedProjectionToken(sqlAlias);
        }
        String columnToken = sanitizedProjectionToken(column);
        if (StrKit.isBlank(columnToken)) {
            columnToken = "column";
        }
        String base = sourceToken + "_" + columnToken;
        Integer count = aliasCounters.merge(base, 1, Integer::sum);
        return count == 1 ? base : base + "_" + count;
    }

    private String sanitizedProjectionToken(String value) {
        if (StrKit.isBlank(value)) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+", "");
        normalized = normalized.replaceAll("_+$", "");
        if (normalized.isEmpty()) {
            return "";
        }
        if (Character.isDigit(normalized.charAt(0))) {
            return "c_" + normalized;
        }
        return normalized;
    }

    private String normalizeIdentifier(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String toSnakeCase(String value) {
        if (StrKit.isBlank(value)) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isUpperCase(current) && i > 0) {
                char previous = value.charAt(i - 1);
                if (Character.isLowerCase(previous) || Character.isDigit(previous)) {
                    builder.append('_');
                }
            }
            builder.append(Character.toLowerCase(current));
        }
        return builder.toString();
    }

    private SqlTransformVO sqlTransform(String sql, String name) {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setName(name.toLowerCase(Locale.ROOT));
        transform.setSql(sql);
        return transform;
    }

    private record InferredJoinCondition(String predicate) {
    }

    private record SourceColumnRef(String sourceName, String alias, String column) {
    }

    private record ParsedTemplate(String name, String contentJson) {
    }
}
