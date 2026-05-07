package org.gensokyo.data.template.querysource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuerySourceMigrationAnalysisDTO implements Serializable {
    private TemplateV2DraftVO draft;
    private boolean executable;
    private String recommendedScenario;
    private List<String> warnings;
    private List<QuerySourceTransformCandidateDTO> candidates;
}
