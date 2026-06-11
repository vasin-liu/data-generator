package org.gensokyo.data.template.querysource;

import org.gensokyo.data.iterator.DatabaseIteratorVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.vo.FieldVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.reader.JdbcReaderVO;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class V1QuerySourceExtractor {
    private V1QuerySourceExtractor() {
    }

    public static Map<String, QuerySourceVO> extract(TemplateVO template) {
        Map<String, QuerySourceVO> sources = new LinkedHashMap<>();
        Set<String> usedNames = new HashSet<>();
        if (template == null) {
            return sources;
        }
        if (template.getIterator() instanceof DatabaseIteratorVO databaseIterator) {
            sources.put("iterator", V1DatabaseSourceAdapter.fromDatabaseIterator(databaseIterator));
            usedNames.add("iterator");
        }
        if (template.getFields() == null) {
            return sources;
        }
        for (FieldVO field : template.getFields()) {
            if (field == null || field.getStages() == null) {
                continue;
            }
            for (int i = 0; i < field.getStages().size(); i++) {
                StageVO stage = field.getStages().get(i);
                if (!(stage instanceof ReadStageVO readStage) || readStage.getReaders() == null) {
                    continue;
                }
                SelectStageVO nextSelectStage = null;
                if (i + 1 < field.getStages().size() && field.getStages().get(i + 1) instanceof SelectStageVO selectStage) {
                    nextSelectStage = selectStage;
                }
                for (ReaderVO reader : readStage.getReaders()) {
                    if (reader instanceof JdbcReaderVO jdbcReader) {
                        String sourceName = uniqueSourceName(baseSourceName(field), usedNames);
                        sources.put(sourceName, V1DatabaseSourceAdapter.fromJdbcReader(readStage, nextSelectStage, jdbcReader));
                    }
                }
            }
        }
        return sources;
    }

    private static String baseSourceName(FieldVO field) {
        return field.getName() == null || field.getName().isBlank()
                ? "jdbc_reader"
                : field.getName();
    }

    private static String uniqueSourceName(String baseName, Set<String> usedNames) {
        if (usedNames.add(baseName)) {
            return baseName;
        }
        int index = 2;
        while (!usedNames.add(baseName + "_" + index)) {
            index++;
        }
        return baseName + "_" + index;
    }
}
