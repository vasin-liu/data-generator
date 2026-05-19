/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.character.StrKit;

import java.util.Objects;

/**
 * Loads a {@link TemplateVO} from persisted template content using the same rules as
 * {@link org.gensokyo.data.controller.TemplateController#buildV1Template}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class TemplateV1Loader {

    private final YamlParser yamlParser;

    /**
     * Creates a loader with the given YAML parser.
     *
     * @param yamlParser parser used to read template YAML
     */
    public TemplateV1Loader(YamlParser yamlParser) {
        this.yamlParser = Objects.requireNonNull(yamlParser, "yamlParser");
    }

    /**
     * Parses {@code entity} content as V1, applies id/name from the entity, and rejects V2-only templates.
     *
     * @param entity persisted template row
     * @return hydrated V1 template
     * @throws IllegalArgumentException when content is V2-only or not valid V1
     */
    public TemplateVO load(TemplatePO entity) {
        Objects.requireNonNull(entity, "entity");
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

    private <T> T tryParse(String yaml, Class<T> clazz) {
        try {
            return yamlParser.parse(yaml, clazz);
        }
        catch (Exception ignored) {
            return null;
        }
    }
}
