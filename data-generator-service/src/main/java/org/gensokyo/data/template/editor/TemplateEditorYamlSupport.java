/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.editor;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.yaml.YamlParser;
import org.springframework.stereotype.Component;

/**
 * Parses and serializes {@link TemplateV2DraftVO} for the YAML advanced panel.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Component
@RequiredArgsConstructor
public class TemplateEditorYamlSupport {

    private final YamlParser yamlParser;

    /**
     * Serializes a draft to YAML text.
     *
     * @param draft V2 draft
     * @return YAML string
     */
    public String toYaml(TemplateV2DraftVO draft) {
        return yamlParser.dump(draft);
    }

    /**
     * Parses YAML into a draft.
     *
     * @param yaml YAML body
     * @return parsed draft
     * @throws IllegalArgumentException when parsing fails
     */
    public TemplateV2DraftVO parseYaml(String yaml) {
        try {
            TemplateV2DraftVO parsed = yamlParser.parse(yaml, TemplateV2DraftVO.class);
            if (parsed == null) {
                throw new IllegalArgumentException("YAML did not parse as Template V2 draft");
            }
            return parsed;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid YAML: " + e.getMessage(), e);
        }
    }
}
