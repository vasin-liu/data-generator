/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.iterator.DatabaseIteratorVO;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.GeoJsonSourceVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link MigrationPlanExplainService}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class MigrationPlanExplainServiceTests {

    private final MigrationPlanExplainService service = new MigrationPlanExplainService();

    @Test
    void explainRowLocalChunkedSqlIncludesShapeAndDiffNote() {
        TemplateVO v1 = new TemplateVO();
        v1.setName("jdbc-export");
        DatabaseIteratorVO database = new DatabaseIteratorVO();
        database.setType("DATABASE");
        database.setDataSourceId("ds_main");
        database.setSql("SELECT id, name FROM t_export");
        v1.setIterator(database);

        TemplateV2VO v2 = new TemplateV2VO();
        v2.setName("jdbc-export-v2");
        IteratorSourceVO input = new IteratorSourceVO();
        Map<String, org.gensokyo.data.model.v2.SourceVO> sources = new LinkedHashMap<>();
        sources.put("input", input);
        v2.setSources(sources);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT id, name FROM input");
        v2.setTransformers(List.of(transform));

        ExecutionPolicyVO policy = new ExecutionPolicyVO();
        policy.setMode("CHUNKED");
        v2.setExecutionPolicy(policy);

        MigrationPlanExplain explain = service.explain(v1, v2);

        Assertions.assertEquals("ROW_LOCAL", explain.getExecutionShape());
        Assertions.assertEquals("CHUNKED", explain.getEffectiveExecutionMode());
        Assertions.assertTrue(explain.getCalciteValidation().contains("OK"));
        Assertions.assertTrue(explain.getDiffNotes().stream()
                .anyMatch(note -> note.contains("CHUNKED row-local")));
    }

    @Test
    void explainIncludesGeoSourceSummaries() {
        TemplateV2VO v2 = new TemplateV2VO();
        v2.setName("geo-mix");

        GeoJsonSourceVO geoJson = new GeoJsonSourceVO();
        geoJson.setPath("classpath:geo/sites.geojson");

        PostGisQuerySourceVO postGis = new PostGisQuerySourceVO();
        postGis.setTable("sites");
        postGis.setGeometryColumn("geom");

        IteratorSourceVO geoIter = new IteratorSourceVO();
        org.gensokyo.data.model.vo.iterator.IteratorVO iterator = new org.gensokyo.data.model.vo.iterator.IteratorVO();
        iterator.setType("GEO");
        geoIter.setIterator(iterator);

        v2.setSources(Map.of(
                "assets", geoJson,
                "sites_db", postGis,
                "synthetic", geoIter));

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT lat, lon FROM synthetic");
        v2.setTransformers(List.of(transform));

        MigrationPlanExplain explain = service.explain(new TemplateVO(), v2);

        Assertions.assertTrue(explain.getSourceSummaries().stream()
                .anyMatch(s -> s.contains("GeoJsonSourceVO") && s.contains("sites.geojson")));
        Assertions.assertTrue(explain.getSourceSummaries().stream()
                .anyMatch(s -> s.contains("PostGisQuerySourceVO") && s.contains("sites")));
        Assertions.assertTrue(explain.getSourceSummaries().stream()
                .anyMatch(s -> s.contains("IteratorSourceVO type=GEO")));
    }
}
