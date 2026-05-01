package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.calcite.TemplateV2RunResult;
import org.gensokyo.data.calcite.TemplateV2Runner;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(TaskControllerV2ExecutionTests.TemplateV2RunnerTestConfig.class)
class TaskControllerV2ExecutionTests {

    @Autowired
    private TaskController taskController;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private CapturingTemplateV2Runner templateV2Runner;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
        templateV2Runner.reset();
    }

    @Test
    void runsV2TemplateThroughTaskController() throws InterruptedException {
        TemplatePO entity = new TemplatePO();
        entity.setId(90001L);
        entity.setName("task-controller-v2");
        entity.setContentYaml("""
                name: task-controller-v2
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 2
                      step: 1
                transform:
                  type: sql
                  sql: SELECT value FROM input
                sinkExecutionPolicy:
                  mode: CONTINUE_ON_ERROR
                sink:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<String> result = taskController.runById(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertNotNull(result.getMessage());
        Assertions.assertTrue(result.getMessage().contains("task-controller-v2"));
        Assertions.assertTrue(result.getMessage().contains("templateId=90001"));

        Assertions.assertTrue(templateV2Runner.awaitInvocation(5, TimeUnit.SECONDS));

        TemplateV2VO submitted = templateV2Runner.lastTemplate();
        Assertions.assertNotNull(submitted);
        Assertions.assertEquals(90001L, submitted.getId());
        Assertions.assertEquals("task-controller-v2", submitted.getName());
        Assertions.assertNotNull(submitted.getInstanceId());
        Assertions.assertNotNull(submitted.getSinkExecutionPolicy());
        Assertions.assertEquals("CONTINUE_ON_ERROR", submitted.getSinkExecutionPolicy().getMode());
        Assertions.assertEquals(1, submitted.getSources().size());
        Assertions.assertEquals(1, submitted.getTransformers().size());
        Assertions.assertEquals(1, submitted.getSinks().size());
    }

    @TestConfiguration
    static class TemplateV2RunnerTestConfig {
        @Bean
        @Primary
        CapturingTemplateV2Runner capturingTemplateV2Runner() {
            return new CapturingTemplateV2Runner();
        }
    }

    static class CapturingTemplateV2Runner extends TemplateV2Runner {
        private final AtomicReference<TemplateV2VO> lastTemplate = new AtomicReference<>();
        private volatile CountDownLatch latch = new CountDownLatch(1);

        @Override
        public TemplateV2RunResult run(TemplateV2VO template) {
            lastTemplate.set(template);
            latch.countDown();
            return new TemplateV2RunResult(new RowSchema(), List.of(new Row(Map.of())));
        }

        boolean awaitInvocation(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        TemplateV2VO lastTemplate() {
            return lastTemplate.get();
        }

        void reset() {
            lastTemplate.set(null);
            latch = new CountDownLatch(1);
        }
    }
}
