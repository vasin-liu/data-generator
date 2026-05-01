package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;

public class NoopRuntimeJdbcEndpointResolver implements RuntimeJdbcEndpointResolver {
    @Override
    public String resolveSourceDataSourceId(QuerySourceVO source) {
        return source == null ? null : source.getDataSourceId();
    }

    @Override
    public String resolveSinkDataSourceId(JdbcWriterVO writer) {
        return writer == null ? null : writer.getDataSourceId();
    }
}
