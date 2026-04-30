package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;

@Getter
@Setter
@AutoService(TransformVO.class)
@JsonSubType("SQL")
public class SqlTransformVO extends TransformVO {
    public SqlTransformVO() {
        setType("sql");
    }

    private String dialect;
    private String sql;
}
