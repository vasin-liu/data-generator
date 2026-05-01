package org.gensokyo.data.template;

import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.kit.collect.CollectKit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

public final class TemplateV2Normalizer {
    private TemplateV2Normalizer() {
    }

    public static TemplateV2VO normalize(TemplateV2DraftVO draft) {
        if (draft == null) {
            return null;
        }
        var normalized = new TemplateV2VO();
        normalized.setId(draft.getId());
        normalized.setInstanceId(draft.getInstanceId());
        normalized.setName(draft.getName());
        normalized.setGenerator(draft.getGenerator());
        normalized.setSinkExecutionPolicy(draft.getSinkExecutionPolicy());
        if (draft.getSources() != null) {
            normalized.setSources(new LinkedHashMap<>(draft.getSources()));
        }

        if (Objects.nonNull(draft.getTransform()) && CollectKit.isNotEmpty(draft.getTransformers())) {
            throw new IllegalArgumentException("Template V2 cannot define both 'transform' and 'transformers'");
        }
        if (Objects.nonNull(draft.getSink()) && CollectKit.isNotEmpty(draft.getSinks())) {
            throw new IllegalArgumentException("Template V2 cannot define both 'sink' and 'sinks'");
        }

        var transformers = new ArrayList<org.gensokyo.data.model.v2.TransformVO>();
        if (Objects.nonNull(draft.getTransform())) {
            transformers.add(draft.getTransform());
        }
        if (CollectKit.isNotEmpty(draft.getTransformers())) {
            transformers.addAll(draft.getTransformers());
        }
        normalized.setTransformers(transformers);

        var sinks = new ArrayList<org.gensokyo.data.model.vo.stage.WriteStageVO>();
        if (Objects.nonNull(draft.getSink())) {
            sinks.add(draft.getSink());
        }
        if (CollectKit.isNotEmpty(draft.getSinks())) {
            sinks.addAll(draft.getSinks());
        }
        normalized.setSinks(sinks);
        return normalized;
    }
}
