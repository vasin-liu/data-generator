/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.editor;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.template.TemplateDefinitionKind;

import java.io.Serializable;

/**
 * Editor bundle returned to the Vaadin console or REST clients.
 *
 * @param templateId   persisted id, or null for unsaved create flow
 * @param kind         detected definition kind
 * @param draft        editable V2 draft (may be synthesized for V1 rows)
 * @param v1Yaml       original V1 YAML when {@code kind == V1}; otherwise null
 * @param archived     whether the row is archived
 */
public record TemplateEditorPayload(
        @JsonSerialize(using = ToStringSerializer.class) Long templateId,
        TemplateDefinitionKind kind,
        TemplateV2DraftVO draft,
        String v1Yaml,
        boolean archived) implements Serializable {
}
