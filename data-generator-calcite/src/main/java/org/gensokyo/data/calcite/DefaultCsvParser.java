package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.CsvSourceVO;

import java.util.ArrayList;
import java.util.List;

public class DefaultCsvParser implements CsvParser {
    @Override
    public List<List<String>> parse(CsvSourceVO source, List<String> lines) {
        char delimiter = delimiter(source);
        return lines.stream()
                .map(line -> parseLine(line, delimiter))
                .toList();
    }

    private char delimiter(CsvSourceVO source) {
        String delimiter = source.getDelimiter();
        return delimiter == null || delimiter.isEmpty() ? ',' : delimiter.charAt(0);
    }

    private List<String> parseLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == delimiter && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }
}
