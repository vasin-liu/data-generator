package org.gensokyo.data.template.migration;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class MigrationInventoryBootstrapTests {

    @Autowired
    private TemplateRepository templateRepository;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void refreshFromRepositoryAddsDbEntry() throws Exception {
        TemplatePO entity = new TemplatePO();
        entity.setId(92001L);
        entity.setName("bootstrap-v1");
        entity.setContentYaml("""
                name: bootstrap-v1
                iterator:
                  type: database
                  dataSourceId: ds_main
                  sql: select id from t_demo
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        Path inventoryPath = Files.createTempFile("inventory-bootstrap", ".yaml");
        Files.writeString(inventoryPath, "version: 1\ntemplates: []\n");
        MigrationInventoryService service = new MigrationInventoryService(inventoryPath);

        service.refreshFromRepository(templateRepository);

        String expectedId = "db-" + entity.getId();
        MigrationInventoryEntry dbEntry = service.findById(expectedId).orElseThrow();
        Assertions.assertEquals("database", dbEntry.getOrigin());
        Assertions.assertEquals(entity.getId(), dbEntry.getDbTemplateId());
        Assertions.assertEquals("synthetic", dbEntry.getScenarioFamily());
        Assertions.assertEquals("bootstrap-v1", dbEntry.getName());
    }
}
