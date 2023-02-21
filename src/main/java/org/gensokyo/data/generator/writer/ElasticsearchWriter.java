/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.writer;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.gensokyo.boot.elasticsearch.support.MultipleElasticsearchRestClient;
import org.gensokyo.data.generator.domain.WriterPO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.util.TemplateKit;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ElasticSearch数据写入器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Slf4j
public class ElasticsearchWriter extends AbstractWriter {

    private MultipleElasticsearchRestClient restClient;

    @Autowired
    public void setRestClient(MultipleElasticsearchRestClient restClient) {
        this.restClient = restClient;
    }

    public ElasticsearchWriter(final WriterPO wpo) {
        super(Objects.requireNonNull(wpo));
    }

    @SuppressWarnings({"resource", "deprecation"})
    @Override
    public long write(final List<Map<String, Object>> data) {
        try {
            RestHighLevelClient rhlc = restClient.hlc(wpo.getDataSourceId());
            BulkRequest br = new BulkRequest();
            Objects.requireNonNull(data)
                    .forEach(d -> br.add(TemplateKit.toEs(wpo.getTemplate(), wpo.getTarget(), d)));
            BulkResponse resp = rhlc.bulk(br, RequestOptions.DEFAULT);
            return Arrays.stream(resp.getItems()).filter(r -> RestStatus.OK.equals(r.status())).count();
        } catch (Exception e) {
            log.error("写入数据库出现异常：", e);
            throw new DataGeneratorException("写入数据库出现异常", e);
        }
    }
}
