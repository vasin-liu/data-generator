package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class SourcePolicyVO implements Serializable {
    private Boolean inMemory;
    private String materialization;
    private String selectionStrategy;
    private Integer limit;
}
