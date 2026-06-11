package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class TransformerCapabilityVO implements Serializable {
    private String name;
    private String type;
    private String family;
    private String executionShape;
    private boolean requiresSingleInput;
    private boolean requiresMaterialization;
    private boolean supportsChunking;
    private boolean supportsStreaming;
    private RowSchema inputSchema;
    private RowSchema outputSchema;
}
