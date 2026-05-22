/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.v2.SpelColumnMapping;
import org.gensokyo.data.model.v2.SpelTransformVO;
import org.gensokyo.data.model.vo.FieldVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.scripter.ScriptVO;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.template.BuiltinClasspathTemplateCatalog;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests for {@link V1ScriptToSpelDraftConverter}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class V1ScriptToSpelDraftConverterTests {

    private final JacksonParser yamlParser = new JacksonParser();

    @Test
    void convertsSpelFieldFromBuiltinParkingFixture() {
        BuiltinClasspathTemplateCatalog.Fixture fixture = BuiltinClasspathTemplateCatalog.loadAll().stream()
                .filter(f -> f.relativePath().contains("11_parking_online_space_record"))
                .findFirst()
                .orElseThrow();

        TemplateVO v1 = yamlParser.parse(fixture.yaml(), TemplateVO.class);
        SpelTransformVO spel = V1ScriptToSpelDraftConverter.convert(v1);

        Assertions.assertNotNull(spel);
        SpelColumnMapping lotId = spel.getColumns().stream()
                .filter(c -> "PARKING_LOT_ID".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("#row['ID']", lotId.getExpression());
    }

    @Test
    void ordersFieldsByDependsOnBeforeDependent() {
        TemplateVO v1 = new TemplateVO();
        v1.setName("depends-order");

        FieldVO base = scriptField("BASE", "1");
        FieldVO derived = scriptField("DERIVED", "#dataset.BASE");
        derived.setDependsOn(List.of("BASE"));
        v1.setFields(List.of(derived, base));

        SpelTransformVO spel = V1ScriptToSpelDraftConverter.convert(v1);
        Assertions.assertNotNull(spel);
        Assertions.assertEquals("BASE", spel.getColumns().get(0).getName());
        Assertions.assertEquals("DERIVED", spel.getColumns().get(1).getName());
    }

    private static FieldVO scriptField(String name, String content) {
        ScriptVO language = new ScriptVO();
        language.setType("SPEL");
        language.setContent(content);
        ScriptStageVO stage = new ScriptStageVO();
        stage.setLanguage(language);
        FieldVO field = new FieldVO();
        field.setName(name);
        field.setStages(List.of(stage));
        return field;
    }
}
