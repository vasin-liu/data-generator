package org.gensokyo.data.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuerySourceCandidatePreflightSummaryDTO implements Serializable {
    private boolean normalized;
    private boolean calciteValid;
    private String message;
}
