package org.gensokyo.data.template;

import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.TemplateVO;

import java.util.LinkedHashMap;

public final class V1QuerySourceDraftConverter {
    private V1QuerySourceDraftConverter() {
    }

    public static TemplateV2DraftVO convert(TemplateVO template) {
        if (template == null) {
            return null;
        }
        TemplateV2DraftVO draft = new TemplateV2DraftVO();
        draft.setId(template.getId());
        draft.setInstanceId(template.getInstanceId());
        draft.setName(template.getName());
        draft.setGenerator(template.getGenerator());
        draft.setSources(new LinkedHashMap<>(V1QuerySourceExtractor.extract(template)));
        if (draft.getSources().size() == 1) {
            String sourceName = draft.getSources().keySet().iterator().next();
            SqlTransformVO transform = new SqlTransformVO();
            transform.setSql("SELECT * FROM " + sourceName);
            draft.setTransform(transform);
        }
        if (template.getOutput() != null) {
            draft.setSink(template.getOutput());
        }
        SinkExecutionPolicyVO sinkExecutionPolicy = new SinkExecutionPolicyVO();
        sinkExecutionPolicy.setMode("FAIL_FAST");
        draft.setSinkExecutionPolicy(sinkExecutionPolicy);
        return draft;
    }
}
