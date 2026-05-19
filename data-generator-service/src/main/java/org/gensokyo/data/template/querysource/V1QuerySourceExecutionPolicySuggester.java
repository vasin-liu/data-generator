/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.querysource;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.template.migration.MigrationClassification;
import org.gensokyo.data.template.migration.TemplateMigrationAnalysisDTO;
import org.gensokyo.data.template.migration.V1TemplateMigrationAnalyzer;

import java.util.Map;
import java.util.Objects;

/**
 * Suggests {@link ExecutionPolicyVO} for JDBC query-source migration drafts (Wave 2 chunked export).
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class V1QuerySourceExecutionPolicySuggester {

    /** Default {@code sourceChunkSize} for auto-suggested CHUNKED policy. */
    public static final int DEFAULT_SOURCE_CHUNK_SIZE = 5_000;
    /** Default {@code sinkBatchSize} for auto-suggested CHUNKED policy. */
    public static final int DEFAULT_SINK_BATCH_SIZE = 1_000;
    /** Default {@code maxRowsInMemory} for auto-suggested CHUNKED policy. */
    public static final int DEFAULT_MAX_ROWS_IN_MEMORY = 500_000;
    /**
     * When {@link QuerySourceVO#getMaxRows()} is set below this value, CHUNKED is not suggested
     * (bounded export fits in memory).
     */
    public static final long SMALL_MAX_ROWS_THRESHOLD = DEFAULT_SOURCE_CHUNK_SIZE;

    private V1QuerySourceExecutionPolicySuggester() {
    }

    /**
     * Applies CHUNKED execution defaults to the draft when eligible (single JDBC query source, not
     * compatibility-only, no small {@code maxRows} cap).
     *
     * @param v1    V1 template used for compatibility analysis
     * @param draft query-source migration draft (mutated in place when eligible)
     */
    public static void suggestChunkedPolicyIfEligible(TemplateVO v1, TemplateV2DraftVO draft) {
        Objects.requireNonNull(v1, "v1");
        Objects.requireNonNull(draft, "draft");
        if (!isEligible(v1, draft)) {
            return;
        }
        ExecutionPolicyVO policy = new ExecutionPolicyVO();
        policy.setMode("CHUNKED");
        policy.setSourceChunkSize(DEFAULT_SOURCE_CHUNK_SIZE);
        policy.setSinkBatchSize(DEFAULT_SINK_BATCH_SIZE);
        policy.setMaxRowsInMemory(DEFAULT_MAX_ROWS_IN_MEMORY);
        draft.setExecutionPolicy(policy);
    }

    /**
     * Returns whether CHUNKED policy would be applied for the given V1 template and draft shape.
     *
     * @param v1    V1 template
     * @param draft migration draft
     * @return {@code true} when {@link #suggestChunkedPolicyIfEligible} would set CHUNKED
     */
    public static boolean isEligible(TemplateVO v1, TemplateV2DraftVO draft) {
        TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);
        if (analysis.getSuggestedClass() == MigrationClassification.COMPATIBILITY_ONLY) {
            return false;
        }
        Map<String, SourceVO> sources = draft.getSources();
        if (sources == null || sources.size() != 1) {
            return false;
        }
        SourceVO only = sources.values().iterator().next();
        if (!(only instanceof QuerySourceVO querySource)) {
            return false;
        }
        return !hasSmallMaxRows(querySource);
    }

    private static boolean hasSmallMaxRows(QuerySourceVO querySource) {
        Long maxRows = querySource.getMaxRows();
        if (maxRows == null || maxRows <= 0 || maxRows >= SMALL_MAX_ROWS_THRESHOLD) {
            return false;
        }
        // DatabaseIteratorVO defaults maxRows to Const.AMOUNT when omitted in YAML; not an explicit small cap.
        return maxRows != Const.AMOUNT;
    }
}
