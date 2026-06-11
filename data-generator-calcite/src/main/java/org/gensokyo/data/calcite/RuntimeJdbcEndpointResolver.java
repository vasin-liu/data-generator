package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;

public interface RuntimeJdbcEndpointResolver {
    String resolveSourceDataSourceId(QuerySourceVO source);

    String resolveSinkDataSourceId(JdbcWriterVO writer);
}
