package org.gensokyo.data.calcite;

import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class TemplateV2RunnerInFlightRefreshTests {

    @Test
    void inFlightRunKeepsInitialRegistrySnapshotThroughSinkExecution() {
        SnapshotSinkFactory.writes.clear();
        MutableRegistryProvider provider = new MutableRegistryProvider();
        RefreshingTransformFactory transformFactory = new RefreshingTransformFactory(provider);
        provider.current = registry("old", transformFactory, new SnapshotSinkFactory("old"));
        provider.next = registry("new", new SqlTransformFactory(), new SnapshotSinkFactory("new"));

        TemplateV2Runner runner = new TemplateV2Runner(provider);

        TemplateV2RunResult first = runner.run(template());

        Assertions.assertEquals(1, first.getRows().size());
        Assertions.assertEquals(List.of("old"), SnapshotSinkFactory.writes);
        TemplateV2RunResult second = runner.run(template());
        Assertions.assertEquals(1, second.getRows().size());
        Assertions.assertEquals(List.of("old", "new"), SnapshotSinkFactory.writes);
    }

    private TemplateV2RuntimeRegistry registry(String name,
                                               V2TransformFactory transformFactory,
                                               V2SinkFactory sinkFactory) {
        return new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(transformFactory),
                List.of(sinkFactory)
        );
    }

    private TemplateV2VO template() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("in-flight-refresh");
        template.setSources(Map.of("seed", numberSource()));
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT value FROM seed");
        template.setTransformers(List.of(transform));
        WriterVO writer = new WriterVO();
        writer.setType("SNAPSHOT");
        writer.setTarget("snapshot");
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));
        template.setSinks(List.of(sink));
        return template;
    }

    private IteratorSourceVO numberSource() {
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(1);
        iterator.setTo(1);
        iterator.setStep(1);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }

    private static final class MutableRegistryProvider implements TemplateV2RuntimeRegistryProvider {
        private TemplateV2RuntimeRegistry current;
        private TemplateV2RuntimeRegistry next;

        private MutableRegistryProvider() {
        }

        @Override
        public TemplateV2RuntimeRegistry current() {
            return current;
        }

        @Override
        public TemplateV2RuntimeRegistry refresh() {
            current = next;
            return current;
        }
    }

    private static final class RefreshingTransformFactory implements V2TransformFactory {
        private final MutableRegistryProvider provider;

        private RefreshingTransformFactory(MutableRegistryProvider provider) {
            this.provider = provider;
        }

        @Override
        public boolean supports(TransformVO transform) {
            return transform instanceof SqlTransformVO;
        }

        @Override
        public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
            provider.refresh();
            return new CalciteRowTransformer(((SqlTransformVO) transform).getSql()).transform(context);
        }
    }

    private static final class SnapshotSinkFactory implements V2SinkFactory {
        private static final List<String> writes = new ArrayList<>();
        private final String name;

        private SnapshotSinkFactory(String name) {
            this.name = name;
        }

        @Override
        public boolean supports(WriterVO writer) {
            return "SNAPSHOT".equals(writer.getType());
        }

        @Override
        public RowSink create(WriterVO writer) {
            return new RowSink() {
                @Override
                public void write(RowSchema schema, List<Row> rows) {
                    writes.add(name);
                }
            };
        }
    }
}
