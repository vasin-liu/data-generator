package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
@AutoService(SourceVO.class)
@JsonSubType("JSON")
public class JsonSourceVO extends SourceVO {
    public JsonSourceVO() {
        setType("json");
    }

    private String path;
    private String charset = StandardCharsets.UTF_8.name();
    /**
     * Optional JSON layout: {@code ndjson} (line-delimited objects) or {@code array} (top-level JSON array).
     * When absent, the reader auto-detects from the first non-whitespace character ({@code [} → array).
     */
    private String format;
    private String root;
    private Long maxRows;
    private RowSchema schema;
}
