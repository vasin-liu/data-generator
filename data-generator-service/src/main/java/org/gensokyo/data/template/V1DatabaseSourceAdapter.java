package org.gensokyo.data.template;

import org.gensokyo.data.iterator.DatabaseIteratorVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.reader.JdbcReaderVO;

public final class V1DatabaseSourceAdapter {
    private V1DatabaseSourceAdapter() {
    }

    public static QuerySourceVO fromDatabaseIterator(DatabaseIteratorVO iterator) {
        if (iterator == null) {
            return null;
        }
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId(iterator.getDataSourceId());
        source.setSql(iterator.getSql());
        source.setParams(iterator.getParams());
        source.setPageIndex(iterator.getPageIndex());
        source.setPageSize(iterator.getPageSize());
        source.setMaxRows(iterator.getMaxRows());
        return source;
    }

    public static QuerySourceVO fromJdbcReader(ReadStageVO stage, JdbcReaderVO reader) {
        if (reader == null) {
            return null;
        }
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId(reader.getDataSourceId());
        source.setSql(reader.getContent());
        if (stage != null && stage.getParams() != null) {
            source.setParams(stage.getParams());
        }
        return source;
    }
}
