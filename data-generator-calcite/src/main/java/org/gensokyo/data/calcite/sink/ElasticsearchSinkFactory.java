package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeServices;

import org.elasticsearch.client.RestClient;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.util.Set;

public class ElasticsearchSinkFactory implements V2SinkFactory {
    private static final Set<String> TYPES = Set.of("ELASTICSEARCH", "ES");

    private final TemplateV2RuntimeServices runtimeServices;

    public ElasticsearchSinkFactory(TemplateV2RuntimeServices runtimeServices) {
        this.runtimeServices = runtimeServices;
    }

    @Override
    public boolean supports(WriterVO writer) {
        return writer != null && writer.getType() != null
                && TYPES.contains(writer.getType().trim().toUpperCase());
    }

    @Override
    public RowSink create(WriterVO writer) {
        RestClient client = runtimeServices.elasticsearchClient(writer.getDataSourceId());
        return new ElasticsearchRowSinkAdapter(client, writer);
    }
}
