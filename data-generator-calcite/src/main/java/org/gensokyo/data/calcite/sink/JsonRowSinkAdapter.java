/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.codec.RowJsonCodec;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * JSON file sink for Template V2 pipelines (ARRAY and NDJSON modes) with optional per-chunk streaming.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class JsonRowSinkAdapter implements StreamingFileRowSink {
    private static final String ARRAY_MODE = "ARRAY";
    private static final String NDJSON_MODE = "NDJSON";

    private final WriterVO writer;
    private boolean streaming;
    private boolean initialized;
    private boolean arrayOpen;
    private boolean hasArrayElements;

    /**
     * Creates a JSON sink for the given writer configuration.
     *
     * @param writer writer VO with target path and options
     */
    public JsonRowSinkAdapter(WriterVO writer) {
        this.writer = writer;
    }

    @Override
    public void enableStreaming() {
        this.streaming = true;
    }

    @Override
    public void write(RowSchema schema, List<Row> rows) {
        if (streaming) {
            writeStreaming(rows);
            return;
        }
        writeOneShot(rows);
    }

    @Override
    public void finish() {
        if (!streaming || !arrayOpen) {
            return;
        }
        Path path = targetPath();
        Charset charset = charset();
        try {
            String suffix = hasArrayElements ? System.lineSeparator() + "]" : "]";
            Files.writeString(path, suffix, charset, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to finalize JSON ARRAY sink: " + path, e);
        } finally {
            arrayOpen = false;
        }
    }

    private void writeOneShot(List<Row> rows) {
        Path path = targetPath();
        Charset charset = charset();
        String content = content(rows, mode());
        try {
            ensureParent(path);
            Files.writeString(path, content, charset);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write JSON sink: " + path, e);
        }
    }

    private void writeStreaming(List<Row> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String jsonMode = mode();
        if (NDJSON_MODE.equals(jsonMode)) {
            writeStreamingNdjson(rows);
        } else if (ARRAY_MODE.equals(jsonMode)) {
            writeStreamingArray(rows);
        } else {
            throw new IllegalArgumentException("Unsupported JSON sink mode [" + jsonMode
                    + "] for target [" + writer.getTarget() + "]");
        }
    }

    private void writeStreamingNdjson(List<Row> rows) {
        Path path = targetPath();
        Charset charset = charset();
        String body = ndjson(rows);
        if (body.isEmpty()) {
            return;
        }
        try {
            ensureParent(path);
            if (!initialized) {
                Files.writeString(path, body, charset,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                initialized = true;
                return;
            }
            Files.writeString(path, System.lineSeparator() + body, charset,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write JSON NDJSON sink: " + path, e);
        }
    }

    private void writeStreamingArray(List<Row> rows) {
        Path path = targetPath();
        Charset charset = charset();
        String body = joinJsonObjects(rows);
        if (body.isEmpty()) {
            return;
        }
        String lineSep = System.lineSeparator();
        try {
            ensureParent(path);
            if (!initialized) {
                Files.writeString(path, "[" + lineSep + body, charset,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                initialized = true;
                arrayOpen = true;
                hasArrayElements = true;
                return;
            }
            String prefix = hasArrayElements ? "," + lineSep : lineSep;
            Files.writeString(path, prefix + body, charset, StandardOpenOption.APPEND);
            hasArrayElements = true;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write JSON ARRAY sink: " + path, e);
        }
    }

    private String joinJsonObjects(List<Row> rows) {
        return rows.stream()
                .map(row -> RowJsonCodec.toJsonObject(row.values()))
                .reduce((left, right) -> left + "," + System.lineSeparator() + right)
                .orElse("");
    }

    private String content(List<Row> rows, String jsonMode) {
        return switch (jsonMode) {
            case ARRAY_MODE -> jsonArray(rows);
            case NDJSON_MODE -> ndjson(rows);
            default -> throw new IllegalArgumentException("Unsupported JSON sink mode [" + jsonMode
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

    private void ensureParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private Path targetPath() {
        return Path.of(Objects.requireNonNull(writer.getTarget(), "JSON sink target must not be null"));
    }

    private Charset charset() {
        return Charset.forName(stringOption("charset", StandardCharsets.UTF_8.name()));
    }

    private String stringOption(String name, String defaultValue) {
        Object value = writer.getOptions() == null ? null : writer.getOptions().get(name);
        return value == null ? defaultValue : value.toString();
    }

    private String mode() {
        return stringOption("mode", ARRAY_MODE).toUpperCase(Locale.ROOT);
    }
}
