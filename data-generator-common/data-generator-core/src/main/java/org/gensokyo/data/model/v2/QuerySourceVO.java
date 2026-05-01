package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.stage.ParamVO;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AutoService(SourceVO.class)
@JsonSubType("QUERY")
public class QuerySourceVO extends SourceVO {
    public QuerySourceVO() {
        setType("query");
    }

    private String dataSourceId;
    private InlineDataSourceVO dataSource;
    private String sql;
    private List<ParamVO> params = new ArrayList<>();
    private Integer pageIndex;
    private Integer pageSize;
    private Long maxRows;
    private RowSchema schema;
}
