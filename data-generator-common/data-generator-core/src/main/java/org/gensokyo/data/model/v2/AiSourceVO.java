package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;

@Getter
@Setter
@AutoService(SourceVO.class)
@JsonSubType("AI")
public class AiSourceVO extends SourceVO {
    public AiSourceVO() {
        setType("ai");
    }

    private String api;
    private AiProviderVO provider;
    private String prompt;
    private String parser;
    private RowSchema schema;
}
