package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class JsonRowSinkAdapter implements RowSink {
    private static final String ARRAY_MODE = "ARRAY";
    private static final String NDJSON_MODE = "NDJSON";

    private final WriterVO writer;

    public JsonRowSinkAdapter(WriterVO writer) {
        this.writer = writer;
    }

    @Override
    public void write(RowSchema schema, List<Row> rows) {
        Path path = Path.of(Objects.requireNonNull(writer.getTarget(), "JSON sink target must not be null"));
        Charset charset = Charset.forName(stringOption("charset", StandardCharsets.UTF_8.name()));
        String content = content(rows, mode());
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, charset);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write JSON sink: " + path, e);
        }
    }

    private String content(List<Row> rows, String mode) {
        return switch (mode) {
            case ARRAY_MODE -> jsonArray(rows);
            case NDJSON_MODE -> ndjson(rows);
            default -> throw new IllegalArgumentException("Unsupported JSON sink mode [" + mode
                    + "] for target [" + writer.getTarget() + "]");
        };
    }

    private String jsonArray(List<Row> rows) {
        return rows.stream()
                .map(row -> RowJsonCodec.toJsonObject(row.values()))
                .reduce((left, right) -> left + "," + System.lineSeparator() + right)
                .map(body -> "[" + System.lineSeparator() + body + System.lineSeparator() + "]")
                .orElse("[]");
    }

    private String ndjson(List<Row> rows) {
        return rows.stream()
                .map(row -> RowJsonCodec.toJsonObject(row.values()))
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    private String stringOption(String name, String defaultValue) {
        Object value = writer.getOptions() == null ? null : writer.getOptions().get(name);
        return value == null ? defaultValue : value.toString();
    }

    private String mode() {
        return stringOption("mode", ARRAY_MODE).toUpperCase(Locale.ROOT);
    }
}
