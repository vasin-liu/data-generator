/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.pipeline.DefaultDataPipelineTaskFactory;
import org.gensokyo.data.template.TemplateV2Validator;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Production {@link TemplateRunExecutor} using the V1 task pipeline and {@link TemplateV2Runner}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class PipelineTemplateRunExecutor implements TemplateRunExecutor {

    private final DefaultDataPipelineTaskFactory v1TaskFactory;
    private final TemplateV2Runner templateV2Runner;
    private final V1MigrationRunSampler v1Sampler;

    /**
     * Creates an executor wired to the service task factory and V2 runner.
     *
     * @param v1TaskFactory          V1 pipeline task factory
     * @param templateV2Runner       V2 template runner
     * @param jdbcTemplate           JDBC access for V1 database iterator sampling
     * @param jdbcEndpointResolver   resolves inline JDBC endpoints
     */
    public PipelineTemplateRunExecutor(
            DefaultDataPipelineTaskFactory v1TaskFactory,
            TemplateV2Runner templateV2Runner,
            NamedParameterJdbcTemplate jdbcTemplate,
            RuntimeJdbcEndpointResolver jdbcEndpointResolver) {
        this.v1TaskFactory = Objects.requireNonNull(v1TaskFactory, "v1TaskFactory");
        this.templateV2Runner = Objects.requireNonNull(templateV2Runner, "templateV2Runner");
        this.v1Sampler = new V1MigrationRunSampler(v1TaskFactory, jdbcTemplate, jdbcEndpointResolver);
    }

    @Override
    public RunOutcome runV1(TemplateVO v1, Map<String, Object> params, MigrationCompareOptions options) {
        Objects.requireNonNull(v1, "v1");
        int sampleLimit = resolveSampleLimit(options);
        Optional<RunOutcome> sampled = v1Sampler.trySample(v1, sampleLimit);
        return sampled.orElseGet(() -> v1Sampler.runPipelineFallback(v1));
    }

    @Override
    public RunOutcome runV2(TemplateV2VO v2, Map<String, Object> params, MigrationCompareOptions options) {
        Objects.requireNonNull(v2, "v2");
        int sampleLimit = resolveSampleLimit(options);
        TemplateV2VO runnable = prepareV2ForCompare(v2, options);
        TemplateV2Validator.validate(runnable);
        TemplateV2RunResult result = templateV2Runner.run(runnable);
        List<Row> rows = result.getRows() == null ? List.of() : result.getRows();
        long rowCount = rows.size();
        List<Map<String, Object>> sample = new ArrayList<>();
        int limit = (int) Math.min(sampleLimit, rowCount);
        for (int i = 0; i < limit; i++) {
            sample.add(new LinkedHashMap<>(rows.get(i).values()));
        }
        return new RunOutcome(rowCount, sample);
    }

    private static TemplateV2VO prepareV2ForCompare(TemplateV2VO template, MigrationCompareOptions options) {
        TemplateV2VO copy = copyTemplate(template);
        // Dual-run compare needs materialized rows; CHUNKED runs do not retain them in TemplateV2RunResult.
        ExecutionPolicyVO policy = copy.getExecutionPolicy();
        if (policy == null) {
            policy = new ExecutionPolicyVO();
            copy.setExecutionPolicy(policy);
        }
        policy.setMode("IN_MEMORY");
        if (options != null && options.isPreferChunked()) {
            // preferChunked applies to promoted templates; compare sampling stays in-memory.
        }
        return copy;
    }

    private static TemplateV2VO copyTemplate(TemplateV2VO template) {
        TemplateV2VO copy = new TemplateV2VO();
        copy.setId(template.getId());
        copy.setInstanceId(template.getInstanceId());
        copy.setName(template.getName());
        copy.setGenerator(template.getGenerator());
        copy.setExecutionPolicy(template.getExecutionPolicy());
        copy.setSinkExecutionPolicy(template.getSinkExecutionPolicy());
        if (template.getSources() != null) {
            copy.setSources(new java.util.LinkedHashMap<>(template.getSources()));
        }
        copy.setTransformers(new ArrayList<>(template.getTransformers()));
        copy.setSinks(new ArrayList<>(template.getSinks()));
        return copy;
    }

    private static int resolveSampleLimit(MigrationCompareOptions options) {
        if (options == null || options.getSampleSize() <= 0) {
            return 500;
        }
        return options.getSampleSize();
    }
}
