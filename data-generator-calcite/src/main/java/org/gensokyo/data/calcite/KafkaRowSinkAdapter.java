package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class KafkaRowSinkAdapter implements RowSink {
    private final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}");
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WriterVO writer;

    public KafkaRowSinkAdapter(KafkaTemplate<String, String> kafkaTemplate, WriterVO writer) {
        this.kafkaTemplate = kafkaTemplate;
        this.writer = writer;
    }

    @Override
    public void write(RowSchema schema, List<Row> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String topic = Objects.requireNonNull(writer.getTarget(), "Kafka sink target topic must not be null");
        for (Row row : rows) {
            kafkaTemplate.send(topic, value(row));
        }
    }

    private String value(Row row) {
        if (StringUtils.hasText(writer.getTemplate())) {
            Properties properties = new Properties();
            row.values().forEach((key, value) -> properties.put(key, value == null ? "" : value.toString()));
            return placeholderHelper.replacePlaceholders(writer.getTemplate(), properties);
        }
        return toJsonLikeValue(row.values());
    }

    private String toJsonLikeValue(Map<String, Object> values) {
        Map<String, Object> ordered = new LinkedHashMap<>(values);
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : ordered.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escape(entry.getKey())).append('"').append(':');
            Object value = entry.getValue();
            if (value == null) {
                builder.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
            } else {
                builder.append('"').append(escape(value.toString())).append('"');
            }
            first = false;
        }
        return builder.append('}').toString();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
