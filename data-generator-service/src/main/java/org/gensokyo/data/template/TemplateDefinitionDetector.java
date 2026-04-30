package org.gensokyo.data.template;

import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.kit.collect.CollectKit;

import java.util.Objects;

public final class TemplateDefinitionDetector {
    private TemplateDefinitionDetector() {
    }

    public static TemplateDefinitionKind detect(TemplateVO v1, TemplateV2DraftVO v2) {
        boolean looksV1 = Objects.nonNull(v1) && CollectKit.isNotEmpty(v1.getFields());
        boolean looksV2 = Objects.nonNull(v2)
                && CollectKit.isNotEmpty(v2.getSources())
                && (Objects.nonNull(v2.getTransform()) || CollectKit.isNotEmpty(v2.getTransformers()));
        if (looksV1 && !looksV2) {
            return TemplateDefinitionKind.V1;
        }
        if (looksV2 && !looksV1) {
            return TemplateDefinitionKind.V2;
        }
        return TemplateDefinitionKind.UNKNOWN;
    }
}
