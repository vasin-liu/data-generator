/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.reader;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.gensokyo.boot.elasticsearch.support.MultipleElasticsearchRestClient;
import org.gensokyo.boot.elasticsearch.util.Responses;
import org.gensokyo.data.generator.dataset.Dataset;
import org.gensokyo.data.generator.dataset.ReadableDataset;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.ReaderPO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.factory.ScriptFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

/**
 * ElasticSearch数据读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
public class ElasticsearchReader extends AbstractReader {

    private MultipleElasticsearchRestClient restClient;

    @Autowired
    public void setRestClient(MultipleElasticsearchRestClient restClient) {
        this.restClient = restClient;
    }

    public ElasticsearchReader(final ReaderPO rpo, final ScriptFactory scriptFactory) {
        super(Objects.requireNonNull(rpo), Objects.requireNonNull(scriptFactory));
    }

    @SuppressWarnings({"resource"})
    @Override
    public Dataset read(final Context ctx) {
        try {
            var rhlc = restClient.hlc(rpo.getDataSourceId());
            var req = new SearchRequest();
            var resp = rhlc.search(req, RequestOptions.DEFAULT);
            var data = evalScript(ctx, List.copyOf(Responses.hits(resp.getHits())));
            return ReadableDataset.of(data);
        } catch (Exception e) {
            throw new DataGeneratorException("读取数据库出现异常", e);
        }
    }
}
