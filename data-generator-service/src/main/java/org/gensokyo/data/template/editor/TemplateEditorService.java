/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.editor;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.template.TemplateDefinitionDetector;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.TemplateLifecycleService;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.template.TemplateV2Validator;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.yaml.YamlParser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads and persists Template V2 definitions for the operator console editor.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class TemplateEditorService {

    private final TemplateRepository repository;
    private final YamlParser yamlParser;
    private final TaskExecutionService taskExecutionService;
    private final TemplateLifecycleService templateLifecycleService;
    private final AuditService auditService;

    /**
     * Creates an in-memory empty V2 draft (not persisted until {@link #save(Long, TemplateV2DraftVO)}).
     *
     * @return editor payload without template id
     */
    public TemplateEditorPayload createEmptyDraft() {
        TemplateV2DraftVO draft = newEmptyV2Draft();
        return new TemplateEditorPayload(null, TemplateDefinitionKind.V2, draft, null, false, "DRAFT");
    }

    /**
     * Loads a template for editing.
     *
     * @param templateId persisted template id
     * @return editor payload with draft and kind
     * @throws IllegalArgumentException when the id is unknown
     * @throws IllegalStateException when the template is legacy V1
     */
    public TemplateEditorPayload loadForEditor(Long templateId) {
        TemplatePO entity = requireActiveOrArchivedEntity(templateId);
        rejectV1Template(entity);
        return buildPayload(entity);
    }

    /**
     * Validates, normalizes, and persists a V2 draft onto the template row.
     *
     * @param templateId target id (created when null in draft — caller must use {@link #createAndSave})
     * @param draft      draft from form or YAML advanced panel
     * @return saved payload
     * @throws IllegalStateException when the template is legacy V1
     */
    public TemplateEditorPayload save(Long templateId, TemplateV2DraftVO draft) {
        Objects.requireNonNull(templateId, "templateId");
        Objects.requireNonNull(draft, "draft");
        TemplatePO entity = requireEntity(templateId);
        rejectV1Template(entity);
        if (Boolean.TRUE.equals(entity.getArchived())) {
            throw new IllegalArgumentException("Cannot save archived template: " + templateId);
        }
        templateLifecycleService.markDraftAfterEdit(entity);
        persistV2Draft(entity, draft);
        TemplatePO saved = repository.saveAndFlush(entity);
        auditService.record(
                "TEMPLATE_SAVE",
                "TEMPLATE",
                String.valueOf(saved.getId()),
                java.util.Map.of("name", saved.getName()));
        return buildPayload(saved);
    }

    /**
     * Creates a new persisted template from a V2 draft.
     *
     * @param draft draft body
     * @return saved payload with new id
     */
    public TemplateEditorPayload createAndSave(TemplateV2DraftVO draft) {
        Objects.requireNonNull(draft, "draft");
        TemplatePO entity = new TemplatePO();
        entity.setId(RandomKit.snowFlake().nextId());
        entity.setArchived(Boolean.FALSE);
        entity.setArchivedAt(null);
        templateLifecycleService.ensureDraftOnCreate(entity);
        persistV2Draft(entity, draft);
        TemplatePO saved = repository.saveAndFlush(entity);
        auditService.record(
                "TEMPLATE_CREATE",
                "TEMPLATE",
                String.valueOf(saved.getId()),
                java.util.Map.of("name", saved.getName()));
        return buildPayload(saved);
    }

    /**
     * Archives a template (soft delete).
     *
     * @param templateId template id
     * @throws IllegalArgumentException when missing or already archived
     */
    public void archive(Long templateId) {
        TemplatePO entity = requireEntity(templateId);
        if (taskExecutionService.isRunning(templateId)) {
            throw new IllegalArgumentException("Cannot archive template while a run is active: " + templateId);
        }
        if (Boolean.TRUE.equals(entity.getArchived())) {
            throw new IllegalArgumentException("Template already archived: " + templateId);
        }
        entity.setArchived(Boolean.TRUE);
        entity.setArchivedAt(Instant.now());
        repository.saveAndFlush(entity);
    }

    /**
     * Restores an archived template.
     *
     * @param templateId template id
     * @throws IllegalArgumentException when missing or not archived
     */
    public void restore(Long templateId) {
        TemplatePO entity = requireEntity(templateId);
        if (!Boolean.TRUE.equals(entity.getArchived())) {
            throw new IllegalArgumentException("Template is not archived: " + templateId);
        }
        entity.setArchived(Boolean.FALSE);
        entity.setArchivedAt(null);
        repository.saveAndFlush(entity);
    }

    /**
     * Detects whether a persisted row is legacy V1-only (excluded from console catalog).
     *
     * @param entity template row
     * @return detected kind
     */
    public TemplateDefinitionKind detectDefinitionKind(TemplatePO entity) {
        String yaml = entity.getContentYaml();
        TemplateV2DraftVO v2Draft = tryParse(yaml, TemplateV2DraftVO.class);
        TemplateVO v1 = tryParse(yaml, TemplateVO.class);
        return TemplateDefinitionDetector.detect(v1, v2Draft);
    }

    private void rejectV1Template(TemplatePO entity) {
        if (detectDefinitionKind(entity) == TemplateDefinitionKind.V1) {
            throw new IllegalStateException(
                    "Legacy V1 templates are no longer supported; create a Template V2.");
        }
    }

    private TemplateEditorPayload buildPayload(TemplatePO entity) {
        TemplateDefinitionKind kind = detectDefinitionKind(entity);
        String yaml = entity.getContentYaml();
        TemplateV2DraftVO v2Draft = tryParse(yaml, TemplateV2DraftVO.class);
        TemplateVO v1 = tryParse(yaml, TemplateVO.class);
        boolean archived = Boolean.TRUE.equals(entity.getArchived());

        if (kind == TemplateDefinitionKind.V2 && v2Draft != null) {
            v2Draft.setId(entity.getId());
            TemplateMetadataSupport.mergeFromEntity(entity, v2Draft);
            return new TemplateEditorPayload(entity.getId(), kind, v2Draft, null, archived, resolveStatus(entity));
        }
        if (v1 != null) {
            TemplateV2DraftVO placeholder = v2Draft != null ? v2Draft : newEmptyV2Draft();
            placeholder.setId(entity.getId());
            placeholder.setName(v1.getName());
            return new TemplateEditorPayload(entity.getId(), TemplateDefinitionKind.V1, placeholder, yaml, archived, resolveStatus(entity));
        }
        TemplateV2DraftVO fallback = newEmptyV2Draft();
        fallback.setId(entity.getId());
        fallback.setName(entity.getName());
        return new TemplateEditorPayload(entity.getId(), TemplateDefinitionKind.UNKNOWN, fallback, yaml, archived, resolveStatus(entity));
    }

    private String resolveStatus(TemplatePO entity) {
        String status = entity.getStatus();
        if (status == null || status.isBlank()) {
            return Boolean.TRUE.equals(entity.getArchived()) ? "ARCHIVED" : "DRAFT";
        }
        return status;
    }

    private void persistV2Draft(TemplatePO entity, TemplateV2DraftVO draft) {
        TemplateV2VO normalized = TemplateV2Normalizer.normalize(draft);
        TemplateV2Validator.validate(normalized);
        entity.setName(normalized.getName());
        TemplateMetadataSupport.syncToEntity(entity, draft);
        entity.setContentYaml(yamlParser.dump(draft));
        entity.setContentJson(TemplateJsonCodec.write(draft));
    }

    private TemplateV2DraftVO newEmptyV2Draft() {
        TemplateV2DraftVO draft = new TemplateV2DraftVO();
        draft.setName("new-template");
        IteratorSourceVO input = new IteratorSourceVO();
        input.setType("iterator");
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(1);
        iterator.setTo(3);
        iterator.setStep(1);
        input.setIterator(iterator);
        Map<String, org.gensokyo.data.model.v2.SourceVO> sources = new LinkedHashMap<>();
        sources.put("input", input);
        draft.setSources(sources);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setType("sql");
        transform.setSql("SELECT value FROM input");
        draft.setTransform(transform);

        WriteStageVO sink = new WriteStageVO();
        ConsoleWriterVO console = new ConsoleWriterVO();
        console.setType("console");
        sink.setWriters(List.of(console));
        draft.setSink(sink);
        return draft;
    }

    private TemplatePO requireEntity(Long templateId) {
        return repository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
    }

    private TemplatePO requireActiveOrArchivedEntity(Long templateId) {
        return requireEntity(templateId);
    }

    private <T> T tryParse(String yaml, Class<T> clazz) {
        if (yaml == null || yaml.isBlank()) {
            return null;
        }
        try {
            return yamlParser.parse(yaml, clazz);
        } catch (Exception ignored) {
            return null;
        }
    }
}
