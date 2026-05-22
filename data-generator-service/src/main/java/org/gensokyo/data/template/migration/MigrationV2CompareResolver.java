/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.template.TemplateDefinitionDetector;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.TemplateV1Loader;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.collect.CollectKit;

import java.util.Objects;

/**
 * Resolves a normalized V2 template definition for dual-run compare (persisted draft or migration draft).
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class MigrationV2CompareResolver {

    private final YamlParser yamlParser;
    private final MigrationDraftService migrationDraftService;
    private final TemplateV1Loader v1Loader;

    /**
     * Creates the resolver.
     *
     * @param yamlParser            YAML parser
     * @param migrationDraftService V1 → V2 draft builder
     */
    public MigrationV2CompareResolver(YamlParser yamlParser, MigrationDraftService migrationDraftService) {
        this.yamlParser = Objects.requireNonNull(yamlParser, "yamlParser");
        this.migrationDraftService = Objects.requireNonNull(migrationDraftService, "migrationDraftService");
        this.v1Loader = new TemplateV1Loader(yamlParser);
    }

    /**
     * Builds the V2 side of a compare run from persisted content or an on-the-fly migration draft.
     *
     * @param entity persisted template row
     * @return normalized V2 template with id set
     * @throws IllegalArgumentException when no V2 sources are available for compare
     */
    public TemplateV2VO resolveForCompare(TemplatePO entity) {
        Objects.requireNonNull(entity, "entity");
        TemplateV2DraftVO v2Draft = tryParse(entity.getContentYaml(), TemplateV2DraftVO.class);
        TemplateVO v1Probe = tryParse(entity.getContentYaml(), TemplateVO.class);
        TemplateDefinitionKind kind = TemplateDefinitionDetector.detect(v1Probe, v2Draft);
        TemplateV2DraftVO draft;
        if (kind == TemplateDefinitionKind.V2 && Objects.nonNull(v2Draft)) {
            draft = v2Draft;
        }
        else {
            TemplateVO v1 = v1Loader.load(entity);
            draft = migrationDraftService.buildDraftForCompare(v1);
        }
        if (Objects.isNull(draft) || CollectKit.isEmpty(draft.getSources())) {
            throw new IllegalArgumentException(String.format(
                    "Template '%s' has no V2 sources for compare (migrate or persist a V2 draft first)",
                    entity.getId()));
        }
        TemplateV2VO normalized = TemplateV2Normalizer.normalize(draft);
        normalized.setId(entity.getId());
        return normalized;
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
