/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.collect.CollectKit;

import java.util.Objects;

/**
 * Resolves a normalized Template V2 definition from persisted content.
 *
 * @author Gensokyo
 * @since 2026-06-06
 */
public class TemplateV2DefinitionResolver {

    private final YamlParser yamlParser;

    /**
     * Creates the resolver.
     *
     * @param yamlParser YAML parser for definition probes
     */
    public TemplateV2DefinitionResolver(YamlParser yamlParser) {
        this.yamlParser = Objects.requireNonNull(yamlParser, "yamlParser");
    }

    /**
     * Builds a normalized V2 template from persisted V2 YAML.
     *
     * @param entity persisted template row
     * @return normalized V2 template with id set
     * @throws IllegalArgumentException when content is not V2 or has no sources (non-workflow templates)
     */
    public TemplateV2VO resolve(TemplatePO entity) {
        Objects.requireNonNull(entity, "entity");
        TemplateV2DraftVO v2Draft = tryParse(entity.getContentYaml(), TemplateV2DraftVO.class);
        TemplateVO v1Probe = tryParse(entity.getContentYaml(), TemplateVO.class);
        TemplateDefinitionKind kind = TemplateDefinitionDetector.detect(v1Probe, v2Draft);
        if (kind != TemplateDefinitionKind.V2 || Objects.isNull(v2Draft)) {
            throw new IllegalArgumentException(String.format(
                    "Template '%s' is not a Template V2 definition; legacy V1 templates are no longer supported",
                    entity.getId()));
        }
        boolean workflowMode = v2Draft.getWorkflow() != null;
        if (!workflowMode && CollectKit.isEmpty(v2Draft.getSources())) {
            throw new IllegalArgumentException(String.format(
                    "Template '%s' has no V2 sources", entity.getId()));
        }
        TemplateV2VO normalized = TemplateV2Normalizer.normalize(v2Draft);
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
