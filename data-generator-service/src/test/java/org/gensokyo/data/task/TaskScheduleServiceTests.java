/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.api.console.dto.TaskScheduleUpsertRequest;
import org.gensokyo.data.api.console.dto.TaskScheduleView;
import org.gensokyo.data.model.po.TaskSchedulePO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TaskScheduleRepository;
import org.gensokyo.data.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

/**
 * Integration tests for {@link TaskScheduleService}.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TaskScheduleServiceTests {

    @Autowired
    private TaskScheduleService taskScheduleService;

    @Autowired
    private TaskScheduleRepository taskScheduleRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @AfterEach
    void cleanup() {
        taskScheduleRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    void createComputesNextTriggerAndFindDue() {
        TemplatePO template = new TemplatePO();
        template.setId(77001L);
        template.setName("schedule-target");
        template.setStatus("PUBLISHED");
        template.setContentYaml("name: schedule-target\nsources: {}\n");
        templateRepository.saveAndFlush(template);

        TaskScheduleView created = taskScheduleService.create(
                new TaskScheduleUpsertRequest(template.getId(), "0 * * * * *", true, "every minute"));
        Assertions.assertNotNull(created.nextTriggerAt());

        Instant past = Instant.now().minusSeconds(120);
        TaskSchedulePO row = taskScheduleRepository.findById(created.id()).orElseThrow();
        row.setNextTriggerAt(past);
        taskScheduleRepository.saveAndFlush(row);

        List<TaskSchedulePO> due = taskScheduleService.findDue(Instant.now());
        Assertions.assertFalse(due.isEmpty());
        Assertions.assertEquals(created.id(), due.getFirst().getId());
    }

    @Test
    void invalidCronRejected() {
        TemplatePO template = new TemplatePO();
        template.setId(77002L);
        template.setName("schedule-invalid");
        template.setStatus("PUBLISHED");
        template.setContentYaml("name: x\n");
        templateRepository.saveAndFlush(template);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> taskScheduleService.create(
                        new TaskScheduleUpsertRequest(template.getId(), "not-a-cron", true, null)));
    }
}
