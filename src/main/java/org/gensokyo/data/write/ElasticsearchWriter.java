/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.write;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.gensokyo.boot.elasticsearch.support.MultipleElasticsearchRestClient;
import org.gensokyo.data.context.WriterContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.writer.ElasticsearchWriterPO;
import org.gensokyo.data.util.TemplateKit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ElasticSearch数据写入器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ElasticsearchWriter<T extends ElasticsearchWriterPO> implements Writer<T> {

    private final MultipleElasticsearchRestClient restClient;

    @Override
    public long write(final WriterContext<T> ctx, final List<Map<String, Object>> dataset) {
        var wpo = ctx.writer();
        try {
            RestHighLevelClient rhlc = restClient.hlc(wpo.getDataSourceId());
            BulkRequest br = new BulkRequest();
            Objects.requireNonNull(dataset)
                    .forEach(d -> br.add(TemplateKit.toEs(wpo.getTemplate(), wpo.getTarget(), d)));
            BulkResponse resp = rhlc.bulk(br, RequestOptions.DEFAULT);
            return Arrays.stream(resp.getItems()).filter(r -> RestStatus.OK.equals(r.status())).count();
        } catch (Exception e) {
            throw new DataGeneratorException(String.format("写入数据集出现异常，数据库类型为：%s ，数据源编号为：%s ，目标表名为：%s，写入模板为：%s。",
                    wpo.getWriterType(), wpo.getDataSourceId(), wpo.getTarget(), wpo.getTemplate()), e);
        }
    }

}
