package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.CsvSourceVO;

import java.util.List;

public interface CsvParser {
    List<List<String>> parse(CsvSourceVO source, List<String> lines);
}
