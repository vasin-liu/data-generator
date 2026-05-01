package org.gensokyo.data.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gensokyo.data.model.v2.SqlTransformVO;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuerySourceTransformCandidateDTO implements Serializable {
    private String scenario;
    private String primarySource;
    private List<String> sourceOrder;
    private List<String> aliases;
    private List<String> projectionSkeleton;
    private List<String> joinHints;
    private List<QuerySourceCandidateSourceDTO> sourceMetadata;
    private SqlTransformVO transform;
    private QuerySourceCandidatePreflightSummaryDTO preflight;
}
