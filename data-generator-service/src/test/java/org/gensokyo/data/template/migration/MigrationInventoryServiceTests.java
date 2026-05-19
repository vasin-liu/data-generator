package org.gensokyo.data.template.migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class MigrationInventoryServiceTests {

    @Test
    void loadsEmptyInventory() throws Exception {
        Path path = Files.createTempFile("inventory", ".yaml");
        Files.writeString(path, "templates: []\nversion: 1\n");
        MigrationInventoryService service = new MigrationInventoryService(path);
        Assertions.assertEquals(0, service.listAll().size());
    }
}
