package org.gensokyo.data.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuerySourceCandidatePreflightDTO implements Serializable {
    private TemplateV2DraftVO draft;
    private QuerySourceTransformCandidateDTO candidate;
    private boolean normalized;
    private boolean calciteValid;
    private String message;
}
