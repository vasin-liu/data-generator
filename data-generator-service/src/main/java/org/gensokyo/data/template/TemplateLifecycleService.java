/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.character.StrKit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Template DRAFT / PUBLISHED / ARCHIVED governance (Phase B).
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Service
@RequiredArgsConstructor
public class TemplateLifecycleService {

    private final TemplateRepository repository;
    private final YamlParser yamlParser;
    private final DataGeneratorProperties properties;
    private final AuditService auditService;

    /**
     * @param entity template row
     * @return effective lifecycle status
     */
    public TemplateLifecycleStatus statusOf(TemplatePO entity) {
        if (entity == null) {
            return TemplateLifecycleStatus.DRAFT;
        }
        if (Boolean.TRUE.equals(entity.getArchived())) {
            return TemplateLifecycleStatus.ARCHIVED;
        }
        if (StrKit.isBlank(entity.getStatus())) {
            return TemplateLifecycleStatus.PUBLISHED;
        }
        return TemplateLifecycleStatus.valueOf(entity.getStatus().trim().toUpperCase());
    }

    /**
     * Ensures new templates start as DRAFT when status is unset.
     *
     * @param entity template row before save
     */
    public void ensureDraftOnCreate(TemplatePO entity) {
        if (StrKit.isBlank(entity.getStatus())) {
            entity.setStatus(TemplateLifecycleStatus.DRAFT.name());
        }
    }

    /**
     * Marks a saved edit as DRAFT when the template was previously published.
     *
     * @param entity template row being saved
     */
    public void markDraftAfterEdit(TemplatePO entity) {
        TemplateLifecycleStatus current = statusOf(entity);
        if (current == TemplateLifecycleStatus.PUBLISHED) {
            entity.setStatus(TemplateLifecycleStatus.DRAFT.name());
        }
    }

    /**
     * Validates and publishes a template.
     *
     * @param templateId template id
     * @throws IllegalArgumentException when validation fails or template missing
     */
    @Transactional
    public void publish(Long templateId) {
        TemplatePO entity = repository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        if (Boolean.TRUE.equals(entity.getArchived())) {
            throw new IllegalArgumentException("Cannot publish archived template: " + templateId);
        }
        TemplateV2DraftVO draft = yamlParser.parse(entity.getContentYaml(), TemplateV2DraftVO.class);
        TemplateV2VO normalized = TemplateV2Normalizer.normalize(draft);
        TemplateV2Validator.validate(normalized);
        List<String> secretErrors = TemplateGovernanceSupport.collectSecretViolations(
                normalized, properties.getGovernance().isRejectPlaintextPasswordsInTemplates());
        if (!secretErrors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", secretErrors));
        }
        entity.setStatus(TemplateLifecycleStatus.PUBLISHED.name());
        repository.saveAndFlush(entity);
        auditService.record(
                "TEMPLATE_PUBLISH",
                "TEMPLATE",
                String.valueOf(templateId),
                Map.of("name", entity.getName()));
    }

    /**
     * @param entity template row
     * @throws IllegalArgumentException when not published
     */
    public void requirePublishedForTaskRun(TemplatePO entity) {
        if (!properties.getGovernance().isRequirePublishedForTaskRun()) {
            return;
        }
        if (statusOf(entity) != TemplateLifecycleStatus.PUBLISHED) {
            throw new IllegalArgumentException(
                    "Template must be PUBLISHED before task run; current status=" + statusOf(entity));
        }
    }
}
