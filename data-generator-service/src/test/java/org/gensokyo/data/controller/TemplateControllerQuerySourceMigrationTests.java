package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.dto.QuerySourceCandidatePreflightDTO;
import org.gensokyo.data.model.dto.QuerySourceMigrationAnalysisDTO;
import org.gensokyo.data.model.dto.QuerySourceTransformCandidateDTO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SourcePolicyVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.template.TemplateV2Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TemplateControllerQuerySourceMigrationTests {

    @Autowired
    private TemplateController templateController;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void previewsV1TemplateAsQuerySourceOnlyV2Draft() {
        TemplatePO entity = new TemplatePO();
        entity.setId(91001L);
        entity.setName("v1-preview");
        entity.setContentYaml("""
                name: v1-preview
                iterator:
                  type: database
                  dataSourceId: ds_main
                  sql: select id, tenant_id from t_order where tenant_id = :tenantId
                  pageIndex: 1
                  pageSize: 100
                  maxRows: 300
                  params:
                    - name: tenantId
                      language:
                        type: plain
                        content: tenant-a
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<TemplateV2DraftVO> result = templateController.previewQuerySourceV2ById(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("Preview generated", result.getMessage());
        Assertions.assertNotNull(result.getData());
        Assertions.assertEquals(entity.getId(), result.getData().getId());
        Assertions.assertEquals("v1-preview", result.getData().getName());
        Assertions.assertEquals(1, result.getData().getSources().size());
        Assertions.assertTrue(result.getData().getSources().get("iterator") instanceof QuerySourceVO);

        QuerySourceVO source = (QuerySourceVO) result.getData().getSources().get("iterator");
        Assertions.assertEquals("ds_main", source.getDataSourceId());
        Assertions.assertEquals("select id, tenant_id from t_order where tenant_id = :tenantId", source.getSql());
        Assertions.assertEquals(1, source.getPageIndex());
        Assertions.assertEquals(100, source.getPageSize());
        Assertions.assertEquals(300L, source.getMaxRows());
        Assertions.assertNotNull(result.getData().getSink());
        Assertions.assertEquals(1, result.getData().getSink().getWriters().size());
    }

    @Test
    void rejectsPreviewForExistingV2Template() {
        TemplatePO entity = new TemplatePO();
        entity.setId(91002L);
        entity.setName("already-v2");
        entity.setContentYaml("""
                name: already-v2
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 2
                transform:
                  type: sql
                  sql: SELECT value FROM input
                sink:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<TemplateV2DraftVO> result = templateController.previewQuerySourceV2ById(entity.getId());

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertNotNull(result.getMessage());
        Assertions.assertTrue(result.getMessage().contains("already a V2 template"));
    }

    @Test
    void migratesV1TemplateAndPersistsV2Draft() {
        TemplatePO entity = new TemplatePO();
        entity.setId(91003L);
        entity.setName("v1-migrate");
        entity.setContentYaml("""
                name: v1-migrate
                iterator:
                  type: database
                  dataSourceId: ds_migrate
                  sql: select id from t_demo
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<TemplateV2DraftVO> result = templateController.migrateQuerySourceV2ById(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("Migration completed", result.getMessage());
        Assertions.assertNotNull(result.getData());
        Assertions.assertEquals(1, result.getData().getSources().size());
        Assertions.assertTrue(result.getData().getSources().get("iterator") instanceof QuerySourceVO);
        Assertions.assertInstanceOf(SqlTransformVO.class, result.getData().getTransform());
        Assertions.assertEquals("SELECT * FROM iterator", ((SqlTransformVO) result.getData().getTransform()).getSql());
        Assertions.assertNotNull(result.getData().getSinkExecutionPolicy());
        Assertions.assertEquals("FAIL_FAST", result.getData().getSinkExecutionPolicy().getMode());

        TemplatePO migrated = templateRepository.findById(entity.getId()).orElseThrow();
        TemplateV2DraftVO yamlDraft = new org.gensokyo.data.yaml.JacksonParser().parse(migrated.getContentYaml(), TemplateV2DraftVO.class);
        Assertions.assertNotNull(yamlDraft);
        Assertions.assertEquals("v1-migrate", yamlDraft.getName());
        Assertions.assertEquals(1, yamlDraft.getSources().size());
        Assertions.assertTrue(yamlDraft.getSources().get("iterator") instanceof QuerySourceVO);
        Assertions.assertInstanceOf(SqlTransformVO.class, yamlDraft.getTransform());
        Assertions.assertEquals("SELECT * FROM iterator", ((SqlTransformVO) yamlDraft.getTransform()).getSql());
        Assertions.assertNotNull(yamlDraft.getSinkExecutionPolicy());
        Assertions.assertEquals("FAIL_FAST", yamlDraft.getSinkExecutionPolicy().getMode());
        Assertions.assertNotNull(migrated.getContentYaml());
        Assertions.assertFalse(migrated.getContentYaml().isBlank());

        TemplateV2DraftVO jsonDraft = TemplateJsonCodec.read(migrated.getContentJson(), TemplateV2DraftVO.class);
        Assertions.assertNotNull(jsonDraft);
        Assertions.assertEquals(1, jsonDraft.getSources().size());
        Assertions.assertTrue(jsonDraft.getSources().get("iterator") instanceof QuerySourceVO);
        Assertions.assertInstanceOf(SqlTransformVO.class, jsonDraft.getTransform());
        Assertions.assertEquals("SELECT * FROM iterator", ((SqlTransformVO) jsonDraft.getTransform()).getSql());
        Assertions.assertNotNull(jsonDraft.getSinkExecutionPolicy());
        Assertions.assertEquals("FAIL_FAST", jsonDraft.getSinkExecutionPolicy().getMode());
    }

    @Test
    void analyzesMultiSourceMigrationWithoutGuessingTransform() {
        namedParameterJdbcTemplate.getJdbcTemplate().execute("drop table if exists t_order");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("drop table if exists t_customer");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("create table t_order(id bigint)");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("create table t_customer(id bigint, name varchar(64))");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("insert into t_order(id) values (1)");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("insert into t_customer(id, name) values (1, 'alice')");

        TemplatePO entity = new TemplatePO();
        entity.setId(91004L);
        entity.setName("multi-source-analyze");
        entity.setContentYaml("""
                name: multi-source-analyze
                iterator:
                  type: database
                  dataSourceId: ds_main
                  sql: select id from t_order
                fields:
                  - name: customer_lookup
                    stages:
                      - type: read
                        readers:
                          - type: jdbc
                            dataSourceId: ds_lookup
                            content: select id, name from t_customer
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<QuerySourceMigrationAnalysisDTO> result = templateController.analyzeQuerySourceV2ById(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("Analysis generated", result.getMessage());
        Assertions.assertNotNull(result.getData());
        Assertions.assertFalse(result.getData().isExecutable());
        Assertions.assertNotNull(result.getData().getDraft());
        Assertions.assertEquals(2, result.getData().getDraft().getSources().size());
        Assertions.assertNull(result.getData().getDraft().getTransform());
        Assertions.assertEquals("single-source-baseline", result.getData().getRecommendedScenario());
        Assertions.assertNotNull(result.getData().getWarnings());
        Assertions.assertFalse(result.getData().getWarnings().isEmpty());
        Assertions.assertTrue(result.getData().getWarnings().stream().anyMatch(it -> it.contains("Multiple QuerySourceVO sources")));
        Assertions.assertNotNull(result.getData().getCandidates());
        Assertions.assertEquals(2, result.getData().getCandidates().size());
        QuerySourceTransformCandidateDTO single = result.getData().getCandidates().get(0);
        Assertions.assertEquals("single-source-baseline", single.getScenario());
        Assertions.assertEquals("iterator", single.getPrimarySource());
        Assertions.assertEquals("SELECT s0.* FROM iterator s0", single.getTransform().getSql());
        Assertions.assertNotNull(single.getPreflight());
        Assertions.assertTrue(single.getPreflight().isNormalized());
        Assertions.assertTrue(single.getPreflight().isCalciteValid());
        Assertions.assertEquals(1, single.getProjectionSkeleton().size());
        Assertions.assertEquals("s0.*", single.getProjectionSkeleton().get(0));
        Assertions.assertEquals(1, single.getSourceMetadata().size());
        Assertions.assertEquals("ds_main", single.getSourceMetadata().get(0).getDataSourceId());
        QuerySourceTransformCandidateDTO multi = result.getData().getCandidates().get(1);
        Assertions.assertEquals("multi-source-join-skeleton", multi.getScenario());
        Assertions.assertEquals(2, multi.getSourceOrder().size());
        Assertions.assertEquals(2, multi.getAliases().size());
        Assertions.assertEquals(2, multi.getProjectionSkeleton().size());
        Assertions.assertEquals(1, multi.getJoinHints().size());
        Assertions.assertEquals(2, multi.getSourceMetadata().size());
        Assertions.assertNotNull(multi.getPreflight());
        Assertions.assertTrue(multi.getPreflight().isNormalized());
        Assertions.assertTrue(multi.getPreflight().isCalciteValid());
        Assertions.assertTrue(multi.getTransform().getSql().contains("JOIN"));
        Assertions.assertTrue(multi.getTransform().getSql().startsWith("SELECT s0.*, s1.* FROM"));
        Assertions.assertTrue(multi.getJoinHints().get(0).contains("Replace ON 1 = 1"));
    }

    @Test
    void migratesJdbcReaderSelectHintsIntoSourcePolicy() {
        TemplatePO entity = new TemplatePO();
        entity.setId(91006L);
        entity.setName("policy-migrate");
        entity.setContentYaml("""
                name: policy-migrate
                fields:
                  - name: district_lookup
                    stages:
                      - type: read
                        inMemory: true
                        params:
                          - name: areaCode
                            language:
                              type: plain
                              content: 440100
                        readers:
                          - type: jdbc
                            dataSourceId: ds_lookup
                            content: select code, name from pc_district where parent_code = :areaCode
                      - type: select
                        strategy:
                          type: ONCE_ORDER
                          selectNum: 2
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<TemplateV2DraftVO> result = templateController.previewQuerySourceV2ById(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertNotNull(result.getData());
        Assertions.assertTrue(result.getData().getSources().get("district_lookup") instanceof QuerySourceVO);
        QuerySourceVO source = (QuerySourceVO) result.getData().getSources().get("district_lookup");
        Assertions.assertEquals("ds_lookup", source.getDataSourceId());
        Assertions.assertEquals("select code, name from pc_district where parent_code = :areaCode", source.getSql());
        Assertions.assertNotNull(source.getParams());
        Assertions.assertEquals(1, source.getParams().size());
        SourcePolicyVO policy = source.getPolicy();
        Assertions.assertNotNull(policy);
        Assertions.assertEquals("ONCE_ORDER", policy.getSelectionStrategy());
        Assertions.assertEquals(2, policy.getLimit());
        Assertions.assertEquals(Boolean.TRUE, policy.getInMemory());
    }

    @Test
    void analyzesApproximateV1SelectionAndReaderPoolMigrations() {
        TemplatePO entity = new TemplatePO();
        entity.setId(91010L);
        entity.setName("approximate-policy-analyze");
        entity.setContentYaml("""
                name: approximate-policy-analyze
                fields:
                  - name: weighted_lookup
                    stages:
                      - type: read
                        strategy:
                          type: WEIGHT
                        readers:
                          - type: jdbc
                            dataSourceId: ds_lookup_a
                            content: select id, name from t_customer_a
                          - type: jdbc
                            dataSourceId: ds_lookup_b
                            content: select id, name from t_customer_b
                      - type: select
                        strategy:
                          type: MULTIPLE_ORDER
                          selectNum: 2
                          maxTimes: 3
                  - name: district_lookup
                    stages:
                      - type: read
                        readers:
                          - type: jdbc
                            dataSourceId: ds_lookup_c
                            content: select code, name from t_district
                      - type: select
                        strategy:
                          type: ONCE_RANDOM
                          selectNum: 1
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<QuerySourceMigrationAnalysisDTO> result = templateController.analyzeQuerySourceV2ById(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertNotNull(result.getData());
        Assertions.assertNotNull(result.getData().getWarnings());
        Assertions.assertTrue(result.getData().getWarnings().stream().anyMatch(it -> it.contains("WEIGHT")));
        Assertions.assertTrue(result.getData().getWarnings().stream().anyMatch(it -> it.contains("MULTIPLE_ORDER")));
        Assertions.assertTrue(result.getData().getWarnings().stream().anyMatch(it -> it.contains("ONCE_RANDOM")));
        Assertions.assertTrue(result.getData().getWarnings().stream().anyMatch(it -> it.contains("not preserve")));
    }

    @Test
    void appliesAnalyzedTransformCandidateAndPersistsDraft() {
        TemplatePO entity = new TemplatePO();
        entity.setId(91005L);
        entity.setName("apply-candidate");
        entity.setContentYaml("""
                name: apply-candidate
                iterator:
                  type: database
                  dataSourceId: ds_main
                  sql: select id from t_order
                fields:
                  - name: customer_lookup
                    stages:
                      - type: read
                        readers:
                          - type: jdbc
                            dataSourceId: ds_lookup
                            content: select id, name from t_customer
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<TemplateV2DraftVO> result = templateController.applyQuerySourceCandidateById(entity.getId(), "multi-source-join-skeleton");

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("Transform candidate applied", result.getMessage());
        Assertions.assertNotNull(result.getData());
        Assertions.assertInstanceOf(SqlTransformVO.class, result.getData().getTransform());
        Assertions.assertTrue(((SqlTransformVO) result.getData().getTransform()).getSql().contains("JOIN"));

        TemplatePO migrated = templateRepository.findById(entity.getId()).orElseThrow();
        TemplateV2DraftVO persisted = new org.gensokyo.data.yaml.JacksonParser().parse(migrated.getContentYaml(), TemplateV2DraftVO.class);
        Assertions.assertNotNull(persisted);
        Assertions.assertInstanceOf(SqlTransformVO.class, persisted.getTransform());
        Assertions.assertTrue(((SqlTransformVO) persisted.getTransform()).getSql().contains("JOIN"));
    }

    @Test
    void appliesCandidateAndPersistsNormalizableDraft() {
        namedParameterJdbcTemplate.getJdbcTemplate().execute("drop table if exists t_order");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("drop table if exists t_customer");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("create table t_order(id bigint)");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("create table t_customer(id bigint, name varchar(64))");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("insert into t_order(id) values (1)");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("insert into t_customer(id, name) values (1, 'alice')");

        TemplatePO entity = new TemplatePO();
        entity.setId(91006L);
        entity.setName("apply-normalize");
        entity.setContentYaml("""
                name: apply-normalize
                iterator:
                  type: database
                  dataSourceId: ds_main
                  sql: select id from t_order
                fields:
                  - name: customer_lookup
                    stages:
                      - type: read
                        readers:
                          - type: jdbc
                            dataSourceId: ds_lookup
                            content: select id, name from t_customer
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<TemplateV2DraftVO> result = templateController.applyQuerySourceCandidateAndNormalizeById(entity.getId(), "multi-source-join-skeleton");

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("Transform candidate applied and normalized", result.getMessage());
        Assertions.assertNotNull(result.getData());
        Assertions.assertInstanceOf(SqlTransformVO.class, result.getData().getTransform());

        TemplatePO migrated = templateRepository.findById(entity.getId()).orElseThrow();
        TemplateV2DraftVO persisted = new org.gensokyo.data.yaml.JacksonParser().parse(migrated.getContentYaml(), TemplateV2DraftVO.class);
        Assertions.assertNotNull(persisted);
        Assertions.assertDoesNotThrow(() -> TemplateV2Validator.validate(TemplateV2Normalizer.normalize(persisted)));
    }

    @Test
    void doesNotPersistCandidateWhenCalcitePreflightFails() {
        TemplatePO entity = new TemplatePO();
        entity.setId(91009L);
        entity.setName("apply-normalize-calcite-fail");
        entity.setContentYaml("""
                name: apply-normalize-calcite-fail
                iterator:
                  type: database
                  dataSourceId: data-generator
                  sql: select id from not_exists_table
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<TemplateV2DraftVO> result =
                templateController.applyQuerySourceCandidateAndNormalizeById(entity.getId(), "single-source-baseline");

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("failed Calcite validation"));

        TemplatePO persisted = templateRepository.findById(entity.getId()).orElseThrow();
        Assertions.assertTrue(persisted.getContentYaml().contains("iterator:"));
        Assertions.assertFalse(persisted.getContentYaml().contains("sources:"));
        Assertions.assertFalse(persisted.getContentYaml().contains("transform:"));
    }

    @Test
    void preflightsCandidateWithCalciteAgainstLiveQuerySources() {
        namedParameterJdbcTemplate.getJdbcTemplate().execute("drop table if exists t_order");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("drop table if exists t_customer");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("create table t_order(id bigint, customer_id bigint)");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("create table t_customer(id bigint, name varchar(64))");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("insert into t_order(id, customer_id) values (1, 10)");
        namedParameterJdbcTemplate.getJdbcTemplate().execute("insert into t_customer(id, name) values (10, 'alice')");

        TemplatePO entity = new TemplatePO();
        entity.setId(91007L);
        entity.setName("preflight-success");
        entity.setContentYaml("""
                name: preflight-success
                iterator:
                  type: database
                  dataSourceId: data-generator
                  sql: select id, customer_id from t_order
                fields:
                  - name: customer_lookup
                    stages:
                      - type: read
                        readers:
                          - type: jdbc
                            dataSourceId: data-generator
                            content: select id, name from t_customer
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<QuerySourceCandidatePreflightDTO> result =
                templateController.preflightQuerySourceCandidateById(entity.getId(), "multi-source-join-skeleton");

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("Candidate preflight completed", result.getMessage());
        Assertions.assertNotNull(result.getData());
        Assertions.assertTrue(result.getData().isNormalized());
        Assertions.assertTrue(result.getData().isCalciteValid());
        Assertions.assertEquals("Calcite validation passed", result.getData().getMessage());
        Assertions.assertNotNull(result.getData().getCandidate());
        Assertions.assertEquals("multi-source-join-skeleton", result.getData().getCandidate().getScenario());
    }

    @Test
    void failsPreflightWhenCandidateScenarioDoesNotExist() {
        TemplatePO entity = new TemplatePO();
        entity.setId(91008L);
        entity.setName("preflight-missing-scenario");
        entity.setContentYaml("""
                name: preflight-missing-scenario
                iterator:
                  type: database
                  dataSourceId: ds_main
                  sql: select id from t_order
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<QuerySourceCandidatePreflightDTO> result =
                templateController.preflightQuerySourceCandidateById(entity.getId(), "unknown-scenario");

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("No transform candidate scenario"));
    }
}
