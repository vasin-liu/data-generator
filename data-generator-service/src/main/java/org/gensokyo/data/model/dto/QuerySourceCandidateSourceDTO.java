package org.gensokyo.data.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuerySourceCandidateSourceDTO implements Serializable {
    private String sourceName;
    private String alias;
    private String dataSourceId;
    private String sql;
    private boolean parameterized;
    private boolean paged;
    private String suggestedSql;
}
