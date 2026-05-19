/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

/**
 * Tests for {@link V1IteratorDraftConverter}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class V1IteratorDraftConverterTests {

    private final JacksonParser yamlParser = new JacksonParser();

    @Test
    void convertsIteratorSimpleFixtureToIteratorSourceAndSql() throws Exception {
        TemplateVO v1 = loadFixture("migration/regression/v1-iterator-simple.yaml");

        Assertions.assertTrue(V1IteratorDraftConverter.supports(v1));
        TemplateV2DraftVO draft = V1IteratorDraftConverter.convert(v1);

        Assertions.assertEquals("regression-iterator-simple", draft.getName());
        Assertions.assertEquals(1, draft.getSources().size());
        Assertions.assertInstanceOf(IteratorSourceVO.class, draft.getSources().get("input"));
        Assertions.assertInstanceOf(SqlTransformVO.class, draft.getTransform());
        Assertions.assertEquals("SELECT * FROM input", ((SqlTransformVO) draft.getTransform()).getSql());
        Assertions.assertNotNull(draft.getSink());
        Assertions.assertEquals(1, draft.getSink().getWriters().size());
        Assertions.assertNull(draft.getExecutionPolicy());
    }

    @Test
    void migrationDraftServicePrefersQuerySourceOverIterator() throws Exception {
        TemplateVO v1 = loadFixture("migration/regression/v1-query-lookup.yaml");
        MigrationDraftService service = new MigrationDraftService();
        TemplateV2DraftVO draft = service.buildDraft(v1);

        Assertions.assertInstanceOf(QuerySourceVO.class, draft.getSources().get("district_lookup"));
    }

    private TemplateVO loadFixture(String classpath) throws Exception {
        String yaml = new ClassPathResource(classpath).getContentAsString(StandardCharsets.UTF_8);
        return yamlParser.parse(yaml, TemplateVO.class);
    }
}
