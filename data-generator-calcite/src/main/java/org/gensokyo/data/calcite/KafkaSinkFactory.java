package org.gensokyo.data.calcite;

import org.gensokyo.data.model.vo.writer.WriterVO;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaSinkFactory implements V2SinkFactory {
    private static final String TYPE = "KAFKA";

    private final TemplateV2RuntimeServices runtimeServices;

    public KafkaSinkFactory(TemplateV2RuntimeServices runtimeServices) {
        this.runtimeServices = runtimeServices;
    }

    @Override
    public boolean supports(WriterVO writer) {
        return writer != null && TYPE.equalsIgnoreCase(writer.getType());
    }

    @Override
    public RowSink create(WriterVO writer) {
        KafkaTemplate<String, String> kafkaTemplate = runtimeServices.kafkaTemplate(writer.getDataSourceId());
        return new KafkaRowSinkAdapter(kafkaTemplate, writer);
    }
}
