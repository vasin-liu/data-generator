package org.gensokyo.data.calcite;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElasticsearchRowSinkAdapter implements RowSink {
    private static final Pattern ERRORS_FALSE_PATTERN = Pattern.compile("\"errors\"\\s*:\\s*false");
    private static final Pattern SUCCESS_STATUS_PATTERN = Pattern.compile("\"status\"\\s*:\\s*(200|201)");

    private final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}");
    private final RestClient client;
    private final WriterVO writer;

    public ElasticsearchRowSinkAdapter(RestClient client, WriterVO writer) {
        this.client = client;
        this.writer = writer;
    }

    @Override
    public void write(RowSchema schema, List<Row> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String index = Objects.requireNonNull(writer.getTarget(), "Elasticsearch sink target index must not be null");
        Request request = new Request("POST", "/_bulk");
        request.setEntity(new NStringEntity(bulkPayload(index, rows),
                ContentType.create("application/x-ndjson", StandardCharsets.UTF_8)));
        try {
            Response response = client.performRequest(request);
            long successCount = countSuccessfulItems(response.getEntity(), rows.size());
            if (successCount != rows.size()) {
                throw new IllegalStateException("Elasticsearch bulk write only accepted " + successCount
                        + " of " + rows.size() + " documents");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write rows to Elasticsearch index: " + index, ex);
        }
    }

    private String bulkPayload(String index, List<Row> rows) {
        StringBuilder payload = new StringBuilder(rows.size() * 128);
        for (Row row : rows) {
            payload.append(action(index, row)).append('\n');
            if (WriterOptionResolver.booleanOption(writer, "upsert")) {
                payload.append("{\"doc\":").append(document(row)).append(",\"doc_as_upsert\":true}").append('\n');
            } else {
                payload.append(document(row)).append('\n');
            }
        }
        return payload.toString();
    }

    private String action(String index, Row row) {
        String action = WriterOptionResolver.booleanOption(writer, "upsert") ? "update" : "index";
        StringBuilder builder = new StringBuilder("{\"").append(action).append("\":{\"_index\":\"")
                .append(RowJsonCodec.escape(index)).append('"');
        String id = WriterOptionResolver.stringOption(writer, "id", row);
        if (StringUtils.hasText(id)) {
            builder.append(",\"_id\":\"").append(RowJsonCodec.escape(id)).append('"');
        }
        String routing = WriterOptionResolver.stringOption(writer, "routing", row);
        if (StringUtils.hasText(routing)) {
            builder.append(",\"routing\":\"").append(RowJsonCodec.escape(routing)).append('"');
        }
        return builder.append("}}").toString();
    }

    private String document(Row row) {
        if (StringUtils.hasText(writer.getTemplate())) {
            Properties properties = new Properties();
            row.values().forEach((key, value) -> properties.put(key, value == null ? "" : value.toString()));
            return placeholderHelper.replacePlaceholders(writer.getTemplate(), properties);
        }
        return RowJsonCodec.toJsonObject(row.values());
    }

    private static long countSuccessfulItems(HttpEntity entity, int expectedCount) throws IOException {
        if (entity == null) {
            return 0L;
        }
        String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        if (!StringUtils.hasText(responseBody)) {
            return 0L;
        }
        if (ERRORS_FALSE_PATTERN.matcher(responseBody).find()) {
            return expectedCount;
        }
        Matcher matcher = SUCCESS_STATUS_PATTERN.matcher(responseBody);
        long successCount = 0L;
        while (matcher.find()) {
            successCount++;
        }
        return successCount;
    }

}
