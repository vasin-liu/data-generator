package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.codec.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.gensokyo.data.datasource.elasticsearch.DynamicElasticsearchClientRegistry;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Fast unit tests for Elasticsearch sink adapters (Mockito on {@link RestClient}).
 * HTTP integration uses {@link org.gensokyo.data.calcite.sink.ElasticsearchRowSinkAdapterHttpEmbeddedTests};
 * runner E2E uses {@link org.gensokyo.data.calcite.runtime.TemplateV2RunnerElasticsearchHttpEmbeddedTests}.
 */
class ElasticsearchSinkFactoryTests {

    @Test
    void writesRowsToElasticsearchUsingWriterTemplate() throws Exception {
        RestClient client = restClient();
        WriterVO writer = elasticsearchWriter();
        writer.setTemplate("{\"device\":\"${device}\",\"value\":${value}}");

        new ElasticsearchRowSinkAdapter(client, writer).write(schema(), List.of(
                new Row(Map.of("device", "d1", "value", 10)),
                new Row(Map.of("device", "d2", "value", 20))
        ));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        Mockito.verify(client).performRequest(requestCaptor.capture());
        Request request = requestCaptor.getValue();
        Assertions.assertEquals("POST", request.getMethod());
        Assertions.assertEquals("/_bulk", request.getEndpoint());
        String payload = EntityUtils.toString(request.getEntity(), StandardCharsets.UTF_8);
        Assertions.assertTrue(payload.contains("{\"index\":{\"_index\":\"metrics_v2\"}}"));
        Assertions.assertTrue(payload.contains("{\"device\":\"d1\",\"value\":10}"));
        Assertions.assertTrue(payload.contains("{\"device\":\"d2\",\"value\":20}"));
    }

    @Test
    void writesRowsToElasticsearchAsJsonDocumentWhenTemplateIsBlank() throws Exception {
        RestClient client = restClient();

        new ElasticsearchRowSinkAdapter(client, elasticsearchWriter()).write(schema(), List.of(
                new Row(Map.of("device", "d1", "value", 10))
        ));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        Mockito.verify(client).performRequest(requestCaptor.capture());
        String payload = EntityUtils.toString(requestCaptor.getValue().getEntity(), StandardCharsets.UTF_8);
        Assertions.assertTrue(payload.contains("\"device\":\"d1\""));
        Assertions.assertTrue(payload.contains("\"value\":10"));
    }

    @Test
    void writesRowsToElasticsearchWithResolvedIdRoutingAndUpsert() throws Exception {
        RestClient client = restClient();
        WriterVO writer = elasticsearchWriter();
        writer.setOptions(Map.of(
                "id", "${device}",
                "routing", "route-${device}",
                "upsert", true
        ));

        new ElasticsearchRowSinkAdapter(client, writer).write(schema(), List.of(
                new Row(Map.of("device", "d1", "value", 10))
        ));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        Mockito.verify(client).performRequest(requestCaptor.capture());
        String payload = EntityUtils.toString(requestCaptor.getValue().getEntity(), StandardCharsets.UTF_8);
        Assertions.assertTrue(payload.contains(
                "{\"update\":{\"_index\":\"metrics_v2\",\"_id\":\"d1\",\"routing\":\"route-d1\"}}"));
        Assertions.assertTrue(payload.contains("\"doc_as_upsert\":true"));
        Assertions.assertTrue(payload.contains("\"doc\":{"));
        Assertions.assertTrue(payload.contains("\"device\":\"d1\""));
    }

    @Test
    void writeBatchIssuesOneBulkRequestPerBatchSlice() throws Exception {
        RestClient client = restClient();

        new ElasticsearchRowSinkAdapter(client, elasticsearchWriter()).writeBatch(schema(), List.of(
                new Row(Map.of("device", "d1", "value", 10)),
                new Row(Map.of("device", "d2", "value", 20)),
                new Row(Map.of("device", "d3", "value", 30))
        ), 1);

        Mockito.verify(client, Mockito.times(3)).performRequest(Mockito.any(Request.class));
    }

    @Test
    void rejectsPartialBulkSuccess() throws Exception {
        RestClient client = restClient("{\"errors\":true,\"items\":[{\"index\":{\"status\":201}}]}");

        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class,
                () -> new ElasticsearchRowSinkAdapter(client, elasticsearchWriter()).write(schema(), List.of(
                        new Row(Map.of("device", "d1", "value", 10)),
                        new Row(Map.of("device", "d2", "value", 20))
                )));

        Assertions.assertTrue(failure.getMessage().contains("only accepted 1 of 2"));
    }

    @Test
    void providerContributesElasticsearchSinkFactoryWhenRuntimeServiceExists() {
        DynamicElasticsearchClientRegistry registry = new DynamicElasticsearchClientRegistry("main",
                Map.of("main", restClient()));

        TemplateV2RuntimePlugin plugin = new ElasticsearchTemplateV2RuntimePluginProvider()
                .createPlugin(new TemplateV2RuntimeContext(
                        new NoopRuntimeJdbcEndpointResolver(),
                        new TemplateV2RuntimeServices(null, null, registry),
                        List.of(),
                        getClass().getClassLoader()
                ));

        Assertions.assertEquals(1, plugin.sinkFactories().size());
        Assertions.assertTrue(plugin.sinkFactories().getFirst() instanceof ElasticsearchSinkFactory);
        Assertions.assertTrue(plugin.sinkFactories().getFirst().supports(elasticsearchWriter()));
    }

    @Test
    void providerContributesNoFactoriesWhenElasticsearchRuntimeServiceIsMissing() {
        TemplateV2RuntimePlugin plugin = new ElasticsearchTemplateV2RuntimePluginProvider()
                .createPlugin(new TemplateV2RuntimeContext(
                        new NoopRuntimeJdbcEndpointResolver(),
                        new TemplateV2RuntimeServices(null, null, null),
                        List.of(),
                        getClass().getClassLoader()
                ));

        Assertions.assertTrue(plugin.sinkFactories().isEmpty());
    }

    @Test
    void runnerWritesTransformRowsToElasticsearchSinkFromRuntimeProvider() throws Exception {
        RestClient client = restClient();
        DynamicElasticsearchClientRegistry registry = new DynamicElasticsearchClientRegistry("main",
                Map.of("main", client));
        TemplateV2RuntimeContext context = new TemplateV2RuntimeContext(
                new NoopRuntimeJdbcEndpointResolver(),
                new TemplateV2RuntimeServices(null, null, registry),
                List.of(),
                getClass().getClassLoader()
        );
        TemplateV2RuntimeRegistry runtimeRegistry = new TemplateV2RuntimeRegistryFactory().fromPlugins(List.of(
                new DefaultTemplateV2RuntimePlugin(),
                new ElasticsearchTemplateV2RuntimePluginProvider().createPlugin(context)
        ));

        new TemplateV2Runner(runtimeRegistry).run(template());

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        Mockito.verify(client).performRequest(requestCaptor.capture());
        String payload = EntityUtils.toString(requestCaptor.getValue().getEntity(), StandardCharsets.UTF_8);
        Assertions.assertTrue(payload.contains("{\"value\":2}"));
        Assertions.assertTrue(payload.contains("{\"value\":3}"));
    }

    private RestClient restClient() {
        return restClient("{\"errors\":false}");
    }

    private RestClient restClient(String responseBody) {
        RestClient client = Mockito.mock(RestClient.class);
        Response response = Mockito.mock(Response.class);
        try {
            Mockito.when(response.getEntity()).thenReturn(new NStringEntity(responseBody,
                    ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
            Mockito.when(client.performRequest(Mockito.any(Request.class))).thenReturn(response);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return client;
    }

    private WriterVO elasticsearchWriter() {
        WriterVO writer = new WriterVO();
        writer.setType("ELASTICSEARCH");
        writer.setDataSourceId("main");
        writer.setTarget("metrics_v2");
        return writer;
    }

    private TemplateV2VO template() {
        WriterVO writer = elasticsearchWriter();
        writer.setTemplate("{\"value\":${value}}");
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT value FROM seed WHERE value >= 2");

        TemplateV2VO template = new TemplateV2VO();
        template.setName("elasticsearch-v2-runtime-demo");
        template.setSources(Map.of("seed", numberSource()));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private IteratorSourceVO numberSource() {
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(1);
        iterator.setTo(3);
        iterator.setStep(1);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }

    private RowSchema schema() {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("device", "VARCHAR", false),
                new ColumnDef("value", "BIGINT", false)
        ));
        return schema;
    }
}
