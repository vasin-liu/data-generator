package org.gensokyo.data.calcite.parser;

import org.gensokyo.data.calcite.*;

import org.gensokyo.data.model.v2.CsvSourceVO;

import java.util.ArrayList;
import java.util.List;

/**
 * UTF-8 CSV parser with optional BOM strip on the first line (D-09).
 * <p>
 * Only UTF-8 is supported for chunked streaming reads; other encodings are out of scope for Phase 8.
 */
public class DefaultCsvParser implements CsvParser {

    /**
     * Removes a leading UTF-8 BOM ({@code U+FEFF}) when present.
     *
     * @param line raw line text
     * @return line without BOM prefix
     */
    public static String stripUtf8Bom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    @Override
    public List<List<String>> parse(CsvSourceVO source, List<String> lines) {
        List<List<String>> records = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = i == 0 ? stripUtf8Bom(lines.get(i)) : lines.get(i);
            records.add(parseLine(source, line, i + 1));
        }
        return records;
    }

    /**
     * Parses a single CSV line without reading the whole file into memory.
     *
     * @param source     CSV source configuration (delimiter, path for errors)
     * @param line       raw line text (caller should strip BOM on first line)
     * @param lineNumber 1-based line number for error messages
     * @return parsed field values
     */
    public List<String> parseLine(CsvSourceVO source, String line, int lineNumber) {
        return parseLine(line, delimiter(source), lineNumber, source);
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
