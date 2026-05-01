package org.gensokyo.data.template;

import org.gensokyo.data.iterator.DatabaseIteratorVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.vo.FieldVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.reader.JdbcReaderVO;

import java.util.LinkedHashMap;
import java.util.Map;

public final class V1QuerySourceExtractor {
    private V1QuerySourceExtractor() {
    }

    public static Map<String, QuerySourceVO> extract(TemplateVO template) {
        Map<String, QuerySourceVO> sources = new LinkedHashMap<>();
        if (template == null) {
            return sources;
        }
        if (template.getIterator() instanceof DatabaseIteratorVO databaseIterator) {
            sources.put("iterator", V1DatabaseSourceAdapter.fromDatabaseIterator(databaseIterator));
        }
        if (template.getFields() == null) {
            return sources;
        }
        for (FieldVO field : template.getFields()) {
            if (field == null || field.getStages() == null) {
                continue;
            }
            for (StageVO stage : field.getStages()) {
                if (!(stage instanceof ReadStageVO readStage) || readStage.getReaders() == null) {
                    continue;
                }
                for (ReaderVO reader : readStage.getReaders()) {
                    if (reader instanceof JdbcReaderVO jdbcReader) {
                        String sourceName = field.getName() == null || field.getName().isBlank()
                                ? "jdbc_reader"
                                : field.getName();
                        sources.put(sourceName, V1DatabaseSourceAdapter.fromJdbcReader(readStage, jdbcReader));
                    }
                }
            }
        }
        return sources;
    }
}
