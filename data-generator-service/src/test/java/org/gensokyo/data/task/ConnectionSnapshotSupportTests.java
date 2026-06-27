/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionHealthStatus;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.JdbcCatalogMetadata;
import org.gensokyo.data.datasource.api.snapshot.ExecutionConnectionSnapshot;
import org.gensokyo.data.datasource.api.snapshot.SnapshottedConnectionRef;
import org.gensokyo.data.messaging.MessagingClusterType;
import org.gensokyo.data.model.po.DataSourceConfigPO;
import org.gensokyo.data.model.po.MessagingClusterConfigPO;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.gensokyo.data.repository.MessagingClusterConfigRepository;
import org.gensokyo.data.writer.KafkaWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConnectionSnapshotSupport} ref extraction (D-03, D-05, D-06).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
@ExtendWith(MockitoExtension.class)
class ConnectionSnapshotSupportTests {

    @Mock
    private DataSourceConfigRepository dataSourceConfigRepository;

    @Mock
    private MessagingClusterConfigRepository messagingClusterConfigRepository;

    @Mock
    private ConnectionCatalog connectionCatalog;

    private ConnectionSnapshotSupport support;

    @BeforeEach
    void setUp() {
        support = new ConnectionSnapshotSupport(dataSourceConfigRepository, messagingClusterConfigRepository);
    }

    @Test
    void buildSnapshot_collectsJdbcKafkaInlineRefsWithSourceTags() {
        Instant updatedAt = Instant.parse("2026-06-27T08:00:00Z");
        when(connectionCatalog.findEntry("orders-db", ConnectionKind.JDBC)).thenReturn(Optional.of(
                new CatalogEntry(
                        "orders-db",
                        ConnectionKind.JDBC,
                        CatalogEntrySource.MANAGED,
                        new JdbcCatalogMetadata("jdbc:h2:mem:orders", "org.h2.Driver"),
                        updatedAt.toEpochMilli(),
                        updatedAt,
                        ConnectionHealthStatus.HEALTHY,
                        null,
                        null)));
        when(connectionCatalog.findEntry("events", ConnectionKind.KAFKA)).thenReturn(Optional.of(
                new CatalogEntry("events", ConnectionKind.KAFKA, CatalogEntrySource.BOOTSTRAP, null)));

        DataSourceConfigPO jdbcRow = new DataSourceConfigPO();
        jdbcRow.setName("orders-db");
        jdbcRow.setUrl("jdbc:h2:mem:orders");
        jdbcRow.setUsername("sa");
        jdbcRow.setPasswordSecretRef("secrets/orders");
        jdbcRow.setDriverClassName("org.h2.Driver");
        jdbcRow.setEnabled(Boolean.TRUE);
        when(dataSourceConfigRepository.findById("orders-db")).thenReturn(Optional.of(jdbcRow));

        MessagingClusterConfigPO kafkaRow = new MessagingClusterConfigPO();
        kafkaRow.setName("events");
        kafkaRow.setClusterType(MessagingClusterType.KAFKA.name());
        kafkaRow.setEnabled(Boolean.TRUE);
        kafkaRow.setConfigJson("{\"bootstrapServers\":[\"localhost:9092\"]}");
        when(messagingClusterConfigRepository.findById("events")).thenReturn(Optional.of(kafkaRow));

        TemplateV2VO template = new TemplateV2VO();
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("orders-db");
        template.getSources().put("orders", source);

        InlineDataSourceVO inline = new InlineDataSourceVO();
        inline.setName("inline-sink");
        inline.setUrl("jdbc:h2:mem:inline");
        inline.setUsername("sa");
        inline.setPasswordSecretRef("secrets/inline");
        inline.setDriverClassName("org.h2.Driver");
        JdbcWriterVO jdbcSink = new JdbcWriterVO();
        jdbcSink.setDataSource(inline);

        KafkaWriterVO kafkaSink = new KafkaWriterVO();
        kafkaSink.setType("KAFKA");
        kafkaSink.setDataSourceId("events");

        WriteStageVO sinkStage = new WriteStageVO();
        sinkStage.setWriters(List.of(jdbcSink, kafkaSink));
        template.setSinks(List.of(sinkStage));

        ExecutionConnectionSnapshot snapshot = support.buildSnapshot(template, connectionCatalog);

        Assertions.assertFalse(snapshot.connections().isEmpty());
        SnapshottedConnectionRef jdbc = snapshot.connections().stream()
                .filter(ref -> "orders-db".equals(ref.name()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(ConnectionKind.JDBC, jdbc.kind());
        Assertions.assertEquals(CatalogEntrySource.MANAGED, jdbc.source());
        Assertions.assertEquals(updatedAt.toEpochMilli(), jdbc.catalogVersion());
        Assertions.assertEquals("secrets/orders", jdbc.configParams().get("passwordSecretRef"));

        SnapshottedConnectionRef inlineRef = snapshot.connections().stream()
                .filter(ref -> "inline-sink".equals(ref.name()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(ConnectionKind.JDBC, inlineRef.kind());
        Assertions.assertEquals("jdbc:h2:mem:inline", inlineRef.configParams().get("url"));

        SnapshottedConnectionRef kafka = snapshot.connections().stream()
                .filter(ref -> "events".equals(ref.name()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(ConnectionKind.KAFKA, kafka.kind());
        Assertions.assertEquals(CatalogEntrySource.BOOTSTRAP, kafka.source());
        Assertions.assertEquals(List.of("localhost:9092"), kafka.configParams().get("bootstrapServers"));
    }

    @Test
    void buildSnapshot_deduplicatesRepeatedRefs() {
        TemplateV2VO template = new TemplateV2VO();
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("shared-db");
        template.getSources().put("a", source);

        JdbcWriterVO sink = new JdbcWriterVO();
        sink.setType(Const.WriterType.JDBC);
        sink.setDataSourceId("shared-db");
        WriteStageVO stage = new WriteStageVO();
        stage.setWriters(List.of(sink));
        template.setSinks(List.of(stage));

        when(connectionCatalog.findEntry(eq("shared-db"), eq(ConnectionKind.JDBC))).thenReturn(Optional.of(
                new CatalogEntry("shared-db", ConnectionKind.JDBC, CatalogEntrySource.MANAGED, null)));
        DataSourceConfigPO row = new DataSourceConfigPO();
        row.setName("shared-db");
        row.setUrl("jdbc:h2:mem:shared");
        row.setDriverClassName("org.h2.Driver");
        row.setEnabled(Boolean.TRUE);
        when(dataSourceConfigRepository.findById("shared-db")).thenReturn(Optional.of(row));

        ExecutionConnectionSnapshot snapshot = support.buildSnapshot(template, connectionCatalog);

        long jdbcCount = snapshot.connections().stream()
                .filter(ref -> ref.kind() == ConnectionKind.JDBC && "shared-db".equals(ref.name()))
                .count();
        Assertions.assertEquals(1L, jdbcCount);
    }
}
