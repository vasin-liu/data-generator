/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.querysource;

import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

/**
 * Tests for {@link V1QuerySourceExecutionPolicySuggester} via {@link V1QuerySourceDraftConverter}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class V1QuerySourceExecutionPolicySuggesterTests {

    private final JacksonParser yamlParser = new JacksonParser();

    @Test
    void suggestsChunkedPolicyForSingleJdbcReaderWithoutSmallMaxRows() throws Exception {
        TemplateVO v1 = loadFixture("migration/regression/v1-query-lookup.yaml");
        TemplateV2DraftVO draft = V1QuerySourceDraftConverter.convert(v1);

        Assertions.assertNotNull(draft.getExecutionPolicy());
        ExecutionPolicyVO policy = draft.getExecutionPolicy();
        Assertions.assertEquals("CHUNKED", policy.getMode());
        Assertions.assertEquals(V1QuerySourceExecutionPolicySuggester.DEFAULT_SOURCE_CHUNK_SIZE, policy.getSourceChunkSize());
        Assertions.assertEquals(V1QuerySourceExecutionPolicySuggester.DEFAULT_SINK_BATCH_SIZE, policy.getSinkBatchSize());
        Assertions.assertEquals(V1QuerySourceExecutionPolicySuggester.DEFAULT_MAX_ROWS_IN_MEMORY, policy.getMaxRowsInMemory());
    }

    @Test
    void skipsChunkedPolicyWhenMaxRowsIsSmall() {
        TemplateVO v1 = yamlParser.parse("""
                name: small-cap
                iterator:
                  type: database
                  dataSourceId: ds_main
                  sql: select id from t_demo
                  maxRows: 300
                output:
                  writers:
                    - type: console
                """, TemplateVO.class);

        TemplateV2DraftVO draft = V1QuerySourceDraftConverter.convert(v1);

        Assertions.assertNull(draft.getExecutionPolicy());
    }

    @Test
    void skipsChunkedPolicyForCompatibilityOnlyTemplate() {
        TemplateVO v1 = yamlParser.parse("""
                name: jdbc-with-pause
                iterator:
                  type: database
                  dataSourceId: ds_main
                  sql: select id from t_demo
                  stages:
                    - type: PAUSE
                      duration: 1
                      unit: SECONDS
                output:
                  writers:
                    - type: console
                """, TemplateVO.class);
        TemplateV2DraftVO draft = V1QuerySourceDraftConverter.convert(v1);

        Assertions.assertNull(draft.getExecutionPolicy());
        Assertions.assertFalse(V1QuerySourceExecutionPolicySuggester.isEligible(v1, draft));
    }

    private TemplateVO loadFixture(String classpath) throws Exception {
        String yaml = new ClassPathResource(classpath).getContentAsString(StandardCharsets.UTF_8);
        return yamlParser.parse(yaml, TemplateVO.class);
    }
}
