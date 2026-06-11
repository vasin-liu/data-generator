/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Phase B governance validation tests.
 */
class PhaseBGovernanceTests {

    @Test
    void rejectsPlaintextInlinePasswordWhenPolicyEnabled() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("gov-test");
        QuerySourceVO source = new QuerySourceVO();
        source.setType("query");
        source.setSql("select 1");
        InlineDataSourceVO inline = new InlineDataSourceVO();
        inline.setName("inline");
        inline.setUrl("jdbc:h2:mem:x");
        inline.setPassword("secret");
        source.setDataSource(inline);
        template.setSources(new LinkedHashMap<>(java.util.Map.of("s", source)));

        List<String> errors = TemplateGovernanceSupport.collectSecretViolations(template, true);
        Assertions.assertFalse(errors.isEmpty());
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                TemplateV2Validator.validateGovernance(template, true));
    }

    @Test
    void allowsSecretRefInsteadOfPlaintext() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("gov-test");
        QuerySourceVO source = new QuerySourceVO();
        source.setType("query");
        source.setSql("select 1");
        InlineDataSourceVO inline = new InlineDataSourceVO();
        inline.setName("inline");
        inline.setUrl("jdbc:h2:mem:x");
        inline.setPasswordSecretRef("db/demo");
        source.setDataSource(inline);
        template.setSources(new LinkedHashMap<>(java.util.Map.of("s", source)));

        Assertions.assertTrue(TemplateGovernanceSupport.collectSecretViolations(template, true).isEmpty());
    }
}
