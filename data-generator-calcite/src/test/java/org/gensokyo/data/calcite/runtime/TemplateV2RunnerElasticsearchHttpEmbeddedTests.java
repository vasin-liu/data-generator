/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.plugin.DefaultTemplateV2RuntimePlugin;
import org.gensokyo.data.calcite.plugin.ElasticsearchTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.support.EmbeddedElasticsearchHttpSupport;
import org.gensokyo.data.calcite.support.InMemoryCatalog;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * End-to-end {@link TemplateV2Runner} test with Elasticsearch sink against in-process HTTP bulk server.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class TemplateV2RunnerElasticsearchHttpEmbeddedTests {

    private EmbeddedElasticsearchHttpSupport httpServer;

    @BeforeEach
    void startServer() throws Exception {
        httpServer = new EmbeddedElasticsearchHttpSupport();
        httpServer.start();
    }

    @AfterEach
    void stopServer() {
        if (httpServer != null) {
            httpServer.close();
        }
    }

    @Test
    void runnerWritesFilteredRowsToEmbeddedElasticsearchBulkEndpoint() throws Exception {
        try (var client = httpServer.restClient()) {
            InMemoryCatalog catalog = InMemoryCatalog.elasticsearchOnly("main", client);
            TemplateV2RuntimeContext context = new TemplateV2RuntimeContext(
                    new NoopRuntimeJdbcEndpointResolver(),
                    new TemplateV2RuntimeServices(null, catalog),
                    List.of(),
                    getClass().getClassLoader());
            TemplateV2RuntimeRegistry runtimeRegistry = new TemplateV2RuntimeRegistryFactory().fromPlugins(List.of(
                    new DefaultTemplateV2RuntimePlugin(),
                    new ElasticsearchTemplateV2RuntimePluginProvider().createPlugin(context)));

            new TemplateV2Runner(runtimeRegistry).run(template());
        }

        Assertions.assertEquals(1, httpServer.bulkBodies().size());
        String payload = httpServer.bulkBodies().getFirst();
        Assertions.assertTrue(payload.contains("{\"value\":2}"));
        Assertions.assertTrue(payload.contains("{\"value\":3}"));
    }

    private TemplateV2VO template() {
        WriterVO writer = new WriterVO();
        writer.setType("ELASTICSEARCH");
        writer.setDataSourceId("main");
        writer.setTarget("metrics_v2");
        writer.setTemplate("{\"value\":${value}}");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT value FROM seed WHERE value >= 2");

        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(1);
        iterator.setTo(3);
        iterator.setStep(1);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("elasticsearch-v2-embedded-runner");
        template.setSources(Map.of("seed", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }
}
