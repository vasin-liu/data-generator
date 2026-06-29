/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Seeds classpath scenario JDBC fixtures for Podman/Playwright E2E (Phase 8, D-23).
 *
 * <p>Active only under the {@code e2e} profile so production coordinators never execute test DDL.
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
@Service
@Profile("e2e")
@RequiredArgsConstructor
public class E2eV2ScenarioFixtureService {

    private static final int UPSERT_FIXTURE_ROWS = 40;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver;

    /**
     * Prepares idempotent JDBC fixtures for an official scenario catalog id.
     *
     * @param scenarioId catalog id (e.g. {@code GF-GP})
     * @param draft      scenario draft about to be edited or run
     */
    public void prepareIfNeeded(String scenarioId, TemplateV2DraftVO draft) {
        if (scenarioId == null || draft == null) {
            return;
        }
        String normalized = scenarioId.trim().toUpperCase(Locale.ROOT);
        TemplateV2VO template = TemplateV2Normalizer.normalize(draft);
        switch (normalized) {
            case "GF-GP" -> prepareUpsertScenario(template, "gf-g-upsert-pg-source", "gf-g-upsert-pg-target");
            case "GF-GM" -> prepareUpsertScenario(template, "gf-g-upsert-mysql-source", "gf-g-upsert-mysql-target");
            case "GF-EP" -> preparePartialSinkScenario(template);
            default -> {
                // CSV/JSON streaming scenarios use classpath fixtures only.
            }
        }
    }

    /**
     * Mutates upsert source rows so a second run exercises update-on-conflict (D-15).
     *
     * @param scenarioId catalog id {@code GF-GP} or {@code GF-GM}
     */
    public void mutateUpsertSourceForSecondRun(String scenarioId) {
        String normalized = scenarioId.trim().toUpperCase(Locale.ROOT);
        String sourceId = switch (normalized) {
            case "GF-GP" -> "gf-g-upsert-pg-source";
            case "GF-GM" -> "gf-g-upsert-mysql-source";
            default -> throw new IllegalArgumentException("Not an upsert scenario id: " + scenarioId);
        };
        for (int i = 0; i < UPSERT_FIXTURE_ROWS; i++) {
            final int rowId = i;
            final String name = "u" + i;
            execOn(sourceId, () -> namedParameterJdbcTemplate.getJdbcTemplate()
                    .update("update gf_upsert_source set name = ? where id = ?", name, rowId));
        }
    }

    /**
     * @return {@code true} when embedded H2 accepts PostgreSQL {@code ON CONFLICT DO UPDATE} (W-01)
     */
    public boolean h2SupportsPostgresUpsert() {
        try {
            org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                    new org.springframework.jdbc.datasource.DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:pg_upsert_probe_e2e;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
            jdbc.execute("create table upsert_probe(id int primary key, v int)");
            jdbc.execute("insert into upsert_probe(id, v) values (1, 1)");
            jdbc.execute("insert into upsert_probe(id, v) values (1, 2) on conflict (id) do update set v = 2");
            Integer value = jdbc.queryForObject("select v from upsert_probe where id = 1", Integer.class);
            return value != null && value == 2;
        }
        catch (RuntimeException ex) {
            return false;
        }
    }

    private void prepareUpsertScenario(TemplateV2VO template, String sourceId, String targetId) {
        registerInlineEndpoints(template);
        execOn(sourceId,
                "drop table if exists gf_upsert_source",
                "create table gf_upsert_source(id bigint primary key, name varchar(64))",
                seedUpsertSourceRows(UPSERT_FIXTURE_ROWS, "n"));
        execOn(targetId,
                "drop table if exists gf_upsert_target",
                "create table gf_upsert_target(id bigint primary key, name varchar(64))");
    }

    private void preparePartialSinkScenario(TemplateV2VO template) {
        registerInlineEndpoints(template);
        // Intentionally omit __missing_sink_target__ so the first JDBC sink fails with actionable errors.
        execOn("gf-partial-bad", "drop table if exists __missing_sink_target__");
    }

    private void registerInlineEndpoints(TemplateV2VO template) {
        for (QuerySourceVO querySource : template.getSources().values().stream()
                .filter(QuerySourceVO.class::isInstance)
                .map(QuerySourceVO.class::cast)
                .toList()) {
            runtimeJdbcEndpointResolver.resolveSourceDataSourceId(querySource);
        }
        if (template.getSinks() != null) {
            for (var sink : template.getSinks()) {
                if (sink.getWriters() == null) {
                    continue;
                }
                for (WriterVO writer : sink.getWriters()) {
                    if (writer instanceof JdbcWriterVO jdbcWriter) {
                        runtimeJdbcEndpointResolver.resolveSinkDataSourceId(jdbcWriter);
                    }
                }
            }
        }
    }

    private void execOn(String dataSourceId, String... statements) {
        try {
            DynamicDataSourceContextHolder.push(dataSourceId);
            for (String sql : statements) {
                namedParameterJdbcTemplate.getJdbcTemplate().execute(sql);
            }
        }
        finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    private void execOn(String dataSourceId, Runnable action) {
        try {
            DynamicDataSourceContextHolder.push(dataSourceId);
            action.run();
        }
        finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    private static String seedUpsertSourceRows(int rowCount, String namePrefix) {
        StringBuilder insert = new StringBuilder("insert into gf_upsert_source(id, name) values ");
        for (int i = 0; i < rowCount; i++) {
            if (i > 0) {
                insert.append(',');
            }
            insert.append('(').append(i).append(", '").append(namePrefix).append(i).append("')");
        }
        return insert.toString();
    }
}
