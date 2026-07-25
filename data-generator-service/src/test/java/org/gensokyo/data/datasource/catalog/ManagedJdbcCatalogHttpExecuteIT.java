/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.jayway.jsonpath.JsonPath;
import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.datasource.DataSourceConfigService;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.task.TaskExecutionStatus;
import org.gensokyo.data.task.TaskExecutionSummary;
import org.gensokyo.data.template.TemplateLifecycleService;
import org.gensokyo.data.template.TemplateLifecycleStatus;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves EXEC-01: managed JDBC catalog sink through the real HTTP task spine.
 *
 * <p>Evidence path (in order):
 * <ol>
 *   <li>Entry: MockMvc {@code POST /task/run/{id}}</li>
 *   <li>Catalog: managed {@code dataSourceId} via {@link DataSourceConfigService#save}</li>
 *   <li>Gate: publish before run ({@link TemplateLifecycleService#publish})</li>
 *   <li>Async: poll {@link TaskExecutionService} to {@code SUCCESS}</li>
 *   <li>Rows: managed-pool {@code COUNT(*)}</li>
 * </ol>
 *
 * <p>Non-claims (D-11): this IT is not an in-process-only primary path
 * ({@code TemplateV2Runner.run} is not the enqueue evidence); snap-key assertion is deferred.
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-07-25
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = {
                "spring.config.location=classpath:/application-phase7-test.yaml",
                // Override phase7 default (false) so publish gate is exercised (D-02).
                "data.generator.governance.require-published-for-task-run=true"
        })
class ManagedJdbcCatalogHttpExecuteIT {

    private static final String DS_NAME = "managed-jdbc-catalog-http-execute-ds";

    private static final String TABLE = "managed_http_execute_sink";

    private static final String H2_URL =
            "jdbc:h2:mem:managed-jdbc-catalog-http-execute;MODE=MySQL;DB_CLOSE_DELAY=-1";

    private static final long SEEDED_ROW_COUNT = 2L;

    /** Same parse twin as {@code RunReportPersistenceTests} / {@code TemplateEditorRunSupport} (D-04). */
    private static final Pattern INSTANCE_ID_PATTERN = Pattern.compile("instanceId=(\\d+)");

    /** ~50s budget at 200ms sleep — within D-07 30–60s window. */
    private static final int AWAIT_ATTEMPTS = 250;

    private static final long AWAIT_SLEEP_MS = 200L;

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @Autowired
    private DataSourceConfigRepository dataSourceConfigRepository;

    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateLifecycleService templateLifecycleService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void resetDatasourceRow() {
        dataSourceConfigRepository.findById(DS_NAME).ifPresent(dataSourceConfigRepository::delete);
        if (dynamicRoutingDataSource.getDataSources().containsKey(DS_NAME)) {
            dynamicRoutingDataSource.removeDataSource(DS_NAME);
        }
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * Proves managed catalog → publish → MockMvc {@code POST /task/run/{id}} → SUCCESS + COUNT(*)
     * (D-01..D-07, D-11). Does not call {@code TemplateV2Runner.run} as the primary path.
     *
     * @throws Exception if MockMvc or poll fails
     */
    @Test
    void httpTaskRun_managedCatalogSink_reachesSuccessWithCountableRows() throws Exception {
        dataSourceConfigService.save(DS_NAME, H2_URL, "sa", "", null, "org.h2.Driver", null, null);

        try {
            DynamicDataSourceContextHolder.push(DS_NAME);
            namedParameterJdbcTemplate.getJdbcTemplate().execute(
                    "drop table if exists " + TABLE);
            namedParameterJdbcTemplate.getJdbcTemplate().execute(
                    "create table " + TABLE + " (id int primary key, label varchar(64))");
        } finally {
            DynamicDataSourceContextHolder.clear();
        }

        String contentYaml = buildManagedSinkYaml();
        // Managed-id-only sink: no inline writer dataSource block (DS-02 / EXEC-01).
        TemplateV2DraftVO draft = new JacksonParser().parse(contentYaml, TemplateV2DraftVO.class);
        TemplateV2VO parsed = TemplateV2Normalizer.normalize(draft);
        JdbcWriterVO writer = (JdbcWriterVO) parsed.getSinks().getFirst().getWriters().getFirst();
        assertThat(writer.getDataSourceId()).isEqualTo(DS_NAME);
        assertThat(writer.getDataSource()).isNull();

        TemplatePO entity = new TemplatePO();
        entity.setId(RandomKit.snowFlake().nextId());
        entity.setName("managed-jdbc-catalog-http-execute");
        entity.setArchived(Boolean.FALSE);
        entity.setStatus(TemplateLifecycleStatus.DRAFT.name());
        entity.setContentYaml(contentYaml);
        templateRepository.saveAndFlush(entity);

        templateLifecycleService.publish(entity.getId());

        MvcResult enqueue = mockMvc.perform(post("/task/run/{id}", entity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        String message = JsonPath.read(
                enqueue.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.message");

        Long instanceId = extractInstanceId(message);
        awaitSuccess(instanceId);

        // COUNT after SUCCESS — status alone is insufficient for catalog sink proof (D-06).
        assertThat(countRows(DS_NAME, TABLE)).isGreaterThanOrEqualTo(SEEDED_ROW_COUNT);
    }

    private static String buildManagedSinkYaml() {
        return """
                name: managed-jdbc-catalog-http-execute
                sources:
                  seed:
                    type: inline_rows
                    rows:
                      - { id: 1, label: a }
                      - { id: 2, label: b }
                transform:
                  type: sql
                  sql: SELECT id, label FROM seed
                sink:
                  writers:
                    - type: jdbc
                      dataSourceId: %s
                      target: %s
                """.formatted(DS_NAME, TABLE);
    }

    /**
     * Parses {@code instanceId=} from an {@code R.ok} message (D-04).
     *
     * @param message response message text
     * @return snowflake instance id
     */
    private static Long extractInstanceId(String message) {
        Matcher matcher = INSTANCE_ID_PATTERN.matcher(message);
        assertThat(matcher.find()).isTrue();
        return Long.valueOf(matcher.group(1));
    }

    /**
     * Polls execution status until SUCCESS; fails immediately on FAILED or CANCELLED (D-05, D-07).
     *
     * @param instanceId run instance id from enqueue response
     * @return summary at SUCCESS
     * @throws InterruptedException if sleep is interrupted
     */
    private TaskExecutionSummary awaitSuccess(Long instanceId) throws InterruptedException {
        TaskExecutionSummary summary = null;
        // Poll with status checks — no fixed sleep without observing terminal states.
        for (int attempt = 0; attempt < AWAIT_ATTEMPTS; attempt++) {
            summary = taskExecutionService.getByInstanceId(instanceId);
            if (TaskExecutionStatus.SUCCESS.name().equals(summary.status())) {
                return summary;
            }
            if (TaskExecutionStatus.FAILED.name().equals(summary.status())
                    || TaskExecutionStatus.CANCELLED.name().equals(summary.status())) {
                break;
            }
            TimeUnit.MILLISECONDS.sleep(AWAIT_SLEEP_MS);
        }
        assertThat(summary).isNotNull();
        assertThat(summary.status()).isEqualTo(TaskExecutionStatus.SUCCESS.name());
        return summary;
    }

    /**
     * Counts rows on the managed pool after SUCCESS (D-06).
     *
     * @param dataSourceId managed catalog id
     * @param table        sink table name
     * @return row count
     */
    private long countRows(String dataSourceId, String table) {
        try {
            DynamicDataSourceContextHolder.push(dataSourceId);
            Long count = namedParameterJdbcTemplate.getJdbcTemplate()
                    .queryForObject("select count(*) from " + table, Long.class);
            return count == null ? 0L : count;
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }
}
