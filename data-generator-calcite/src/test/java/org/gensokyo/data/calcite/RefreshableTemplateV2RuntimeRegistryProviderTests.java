package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.codec.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class RefreshableTemplateV2RuntimeRegistryProviderTests {

    @Test
    void wrapsInitializationFailure() {
        TemplateV2RuntimePluginProvider failingProvider = context -> {
            throw new IllegalStateException("initialization boom");
        };

        TemplateV2RuntimeRegistryBuildException exception = Assertions.assertThrows(
                TemplateV2RuntimeRegistryBuildException.class,
                () -> new RefreshableTemplateV2RuntimeRegistryProvider(
                        List.of(failingProvider),
                        new TemplateV2RuntimeRegistryFactory(),
                        TemplateV2RuntimeContext.empty()
                ));

        Assertions.assertTrue(exception.getMessage().contains("initialize"));
        Assertions.assertTrue(exception.getCause().getMessage().contains("provider index [0]"));
    }

    @Test
    void refreshFailureKeepsCurrentRegistry() {
        AtomicInteger calls = new AtomicInteger();
        TemplateV2RuntimePluginProvider sometimesFailingProvider = context -> {
            if (calls.incrementAndGet() > 1) {
                throw new IllegalStateException("refresh boom");
            }
            return new DefaultTemplateV2RuntimePlugin();
        };
        RefreshableTemplateV2RuntimeRegistryProvider provider = new RefreshableTemplateV2RuntimeRegistryProvider(
                List.of(sometimesFailingProvider),
                new TemplateV2RuntimeRegistryFactory(),
                TemplateV2RuntimeContext.empty()
        );
        TemplateV2RuntimeRegistry original = provider.current();

        TemplateV2RuntimeRegistryBuildException exception = Assertions.assertThrows(
                TemplateV2RuntimeRegistryBuildException.class,
                provider::refresh);

        Assertions.assertTrue(exception.getMessage().contains("refresh"));
        Assertions.assertSame(original, provider.current());
        Assertions.assertEquals(1, new TemplateV2Runner(provider).run(template()).getRows().size());
    }

    private TemplateV2VO template() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("refresh-diagnostics");
        template.setSources(Map.of("seed", numberSource()));
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT value FROM seed");
        template.setTransformers(List.of(transform));
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));
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
}
