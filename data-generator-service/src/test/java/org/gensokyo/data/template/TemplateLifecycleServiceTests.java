/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.util.RandomKit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = {
                "spring.config.location=classpath:/application-phase7-test.yaml",
                "data.generator.governance.require-published-for-task-run=true"
        })
class TemplateLifecycleServiceTests {

    @Autowired
    private TemplateLifecycleService templateLifecycleService;

    @Autowired
    private TemplateRepository templateRepository;

    @Test
    @Transactional
    void requirePublishedBlocksDraftTemplate() {
        TemplatePO row = new TemplatePO();
        row.setId(RandomKit.snowFlake().nextId());
        row.setName("draft-only");
        row.setStatus(TemplateLifecycleStatus.DRAFT.name());
        row.setArchived(Boolean.FALSE);
        row.setContentYaml("name: draft-only\nsources: {}\n");
        templateRepository.saveAndFlush(row);

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                templateLifecycleService.requirePublishedForTaskRun(row));
    }
}
