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
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateDefinitionDetector;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.template.TemplateV2Validator;
import org.gensokyo.data.template.V1QuerySourceDraftConverter;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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

        TemplateV2DraftVO draft;
        try {
            draft = buildQuerySourceDraft(entity);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
        if (Objects.isNull(draft) || CollectKit.isEmpty(draft.getSources())) {
            return R.fail(String.format("Template '%s' has no database-backed sources that can be converted into QuerySourceVO", templateId));
        }
        return R.ok("Analysis generated", analyzeDraft(draft));
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
        return V1QuerySourceDraftConverter.convert(v1);
    }

    private QuerySourceMigrationAnalysisDTO analyzeDraft(TemplateV2DraftVO draft) {
        List<String> warnings = new ArrayList<>();
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
            List<String> projectionSkeleton = new ArrayList<>();
            List<String> joinHints = new ArrayList<>();
            List<QuerySourceCandidateSourceDTO> sourceMetadata = new ArrayList<>();
            StringBuilder sql = new StringBuilder("SELECT ");
            for (String alias : aliases.values()) {
                projectionSkeleton.add(alias + ".*");
            }
            sql.append(String.join(", ", projectionSkeleton)).append(" FROM ");
            for (int i = 0; i < sourceOrder.size(); i++) {
                String sourceName = sourceOrder.get(i);
                String alias = aliases.get(sourceName);
                sourceMetadata.add(sourceMetadata(draft, sourceName, alias));
                if (i == 0) {
                    sql.append(sourceName).append(' ').append(alias);
                } else {
                    sql.append(" JOIN ").append(sourceName).append(' ').append(alias).append(" ON 1 = 1");
                    joinHints.add("Replace ON 1 = 1 with a business join condition between "
                            + aliases.get(sourceOrder.get(i - 1)) + " and " + alias);
                }
            }
            candidates.add(new QuerySourceTransformCandidateDTO(
                    "multi-source-join-skeleton",
                    primarySource,
                    sourceOrder,
                    new ArrayList<>(aliases.values()),
                    projectionSkeleton,
                    joinHints,
                    sourceMetadata,
                    sqlTransform(sql.toString(), "candidate_multi_source_join_skeleton"),
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

    private SqlTransformVO sqlTransform(String sql, String name) {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setName(name.toLowerCase(Locale.ROOT));
        transform.setSql(sql);
        return transform;
    }

    private record ParsedTemplate(String name, String contentJson) {
    }
}
