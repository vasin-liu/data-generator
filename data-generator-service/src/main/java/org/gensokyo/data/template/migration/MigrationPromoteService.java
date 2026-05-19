/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateDefinitionDetector;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.TemplateV1Loader;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.template.TemplateV2Validator;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.character.StrKit;

import java.util.Objects;

/**
 * Promotes a reviewed V2 migration draft onto {@link TemplatePO} and updates scenario inventory.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class MigrationPromoteService {

    private final TemplateRepository repository;
    private final MigrationDraftService migrationDraftService;
    private final MigrationInventoryService migrationInventoryService;
    private final YamlParser yamlParser;
    private final TemplateV1Loader templateV1Loader;

    /**
     * Creates the promote workflow service.
     *
     * @param repository                 template persistence
     * @param migrationDraftService      unified draft builder
     * @param migrationInventoryService  scenario inventory
     * @param yamlParser                 YAML serializer
     */
    public MigrationPromoteService(
            TemplateRepository repository,
            MigrationDraftService migrationDraftService,
            MigrationInventoryService migrationInventoryService,
            YamlParser yamlParser) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.migrationDraftService = Objects.requireNonNull(migrationDraftService, "migrationDraftService");
        this.migrationInventoryService = Objects.requireNonNull(migrationInventoryService, "migrationInventoryService");
        this.yamlParser = Objects.requireNonNull(yamlParser, "yamlParser");
        this.templateV1Loader = new TemplateV1Loader(yamlParser);
    }

    /**
     * Validates, persists V2 yaml/json on the template, and records inventory promotion (V1 yaml retained).
     *
     * @param templateId persisted template id
     * @return promoted draft
     * @throws IllegalArgumentException when the template is missing or the draft fails validation
     */
    public TemplateV2DraftVO promote(Long templateId) {
        Objects.requireNonNull(templateId, "templateId");
        TemplatePO entity = repository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Template '%s' does not exist", templateId)));

        TemplateV2DraftVO draft = resolveDraft(entity);
        if (draft == null || CollectKit.isEmpty(draft.getSources())) {
            throw new IllegalArgumentException(String.format(
                    "Template '%s' has no V2 sources to promote (build or compare a draft first)", templateId));
        }

        TemplateV2Validator.validate(TemplateV2Normalizer.normalize(draft));

        entity.setName(draft.getName());
        entity.setContentYaml(yamlParser.dump(draft));
        entity.setContentJson(TemplateJsonCodec.write(draft));
        repository.saveAndFlush(entity);

        migrationInventoryService.updatePromoteResult(templateId);
        return draft;
    }

    private TemplateV2DraftVO resolveDraft(TemplatePO entity) {
        if (StrKit.isBlank(entity.getContentYaml())) {
            throw new IllegalArgumentException(String.format("Template '%s' has empty content", entity.getId()));
        }
        TemplateV2DraftVO v2Draft = tryParse(entity.getContentYaml(), TemplateV2DraftVO.class);
        TemplateVO v1Probe = tryParse(entity.getContentYaml(), TemplateVO.class);
        TemplateDefinitionKind kind = TemplateDefinitionDetector.detect(v1Probe, v2Draft);
        if (kind == TemplateDefinitionKind.V2 && v2Draft != null && CollectKit.isNotEmpty(v2Draft.getSources())) {
            v2Draft.setId(entity.getId());
            return v2Draft;
        }
        TemplateVO v1 = templateV1Loader.load(entity);
        TemplateV2DraftVO built = migrationDraftService.buildDraft(v1);
        built.setId(entity.getId());
        return built;
    }

    private <T> T tryParse(String yaml, Class<T> clazz) {
        try {
            return yamlParser.parse(yaml, clazz);
        }
        catch (Exception ignored) {
            return null;
        }
    }
}
