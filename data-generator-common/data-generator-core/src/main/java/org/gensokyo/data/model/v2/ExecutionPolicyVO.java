package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ExecutionPolicyVO implements Serializable {
    private String mode;
    private Integer maxRowsInMemory;
    private Integer previewRowLimit;
    private Integer sourceChunkSize;
    private Integer sinkBatchSize;
    private Boolean failOnLimitExceeded;
    private Integer broadcastMaxRows;
}
