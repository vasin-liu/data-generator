package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.CsvSourceVO;

import java.util.ArrayList;
import java.util.List;

public class DefaultCsvParser implements CsvParser {
    @Override
    public List<List<String>> parse(CsvSourceVO source, List<String> lines) {
        char delimiter = delimiter(source);
        List<List<String>> records = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            records.add(parseLine(lines.get(i), delimiter, i + 1, source));
        }
        return records;
    }

    private char delimiter(CsvSourceVO source) {
        String delimiter = source.getDelimiter();
        if (delimiter == null || delimiter.isEmpty()) {
            return ',';
        }
        if (delimiter.length() != 1) {
            throw new IllegalArgumentException("CSV source delimiter must be exactly one character: "
                    + source.getPath());
        }
        return delimiter.charAt(0);
    }

    private List<String> parseLine(String line, char delimiter, int lineNumber, CsvSourceVO source) {
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
        if (quoted) {
            throw new IllegalArgumentException("Unclosed quoted CSV field at line [" + lineNumber + "]: "
                    + source.getPath());
        }
        values.add(current.toString());
        return values;
    }
}
