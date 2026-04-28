/*
 * Copyright 漏 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address锛歅CI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou锛孋hina锛圸ip code锛?10653锛?
 */
package org.gensokyo.data.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.elasticsearch.support.DynamicElasticsearchClientRegistry;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ElasticSearch鏁版嵁鍐欏叆鍣?
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ElasticsearchWriter<S extends WriteStageVO, T extends ElasticsearchWriterVO> implements Writer<S, T> {

    private static final Pattern ERRORS_FALSE_PATTERN = Pattern.compile("\"errors\"\\s*:\\s*false");
    private static final Pattern SUCCESS_STATUS_PATTERN = Pattern.compile("\"status\"\\s*:\\s*(200|201)");

    private final DynamicElasticsearchClientRegistry elasticsearchClientRegistry;

    @Override
    public long write(final StageContext<S> ctx, final T wvo, final List<Map<String, Object>> dataset) {
        try {
            List<Map<String, Object>> documents = Objects.requireNonNull(dataset);
            if (CollectionUtils.isEmpty(documents)) {
                return 0L;
            }

            RestClient client = elasticsearchClientRegistry.llc(wvo.getDataSourceId());
            Request request = new Request("POST", "/_bulk");
            request.setEntity(new NStringEntity(buildBulkPayload(wvo.getTemplate(), wvo.getTarget(), documents),
                    ContentType.create("application/x-ndjson", StandardCharsets.UTF_8)));

            Response response = client.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return countSuccessfulItems(responseBody, documents.size());
        } catch (Exception e) {
            throw new DataGeneratorException(String.format("鍐欏叆鏁版嵁闆嗗嚭鐜板紓甯革紝鍐欏叆鍣ㄧ被鍨嬩负锛?s 锛屾暟鎹簮缂栧彿涓猴細%s 锛岀洰鏍囪〃鍚嶄负锛?s锛屽啓鍏ユā鏉夸负锛?s銆?,
                    wvo.getType(), wvo.getDataSourceId(), wvo.getTarget(), wvo.getTemplate()), e);
        }
    }

    private static String buildBulkPayload(String template, String index, List<Map<String, Object>> dataset) {
        String action = TemplateKit.toBulkIndexAction(index);
        StringBuilder payload = new StringBuilder(dataset.size() * 128);
        for (Map<String, Object> document : dataset) {
            payload.append(action).append('\n');
            payload.append(TemplateKit.toBulkDocument(template, document)).append('\n');
        }
        return payload.toString();
    }

    private static long countSuccessfulItems(String responseBody, int expectedCount) {
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
