/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.exception.DataGeneratorException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static tools.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

/**
 * Loads and persists the committed migration scenario inventory YAML file.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class MigrationInventoryService {

    private static final int INVENTORY_VERSION = 1;

    private final Path inventoryPath;
    private final ObjectMapper yamlMapper;
    private List<MigrationInventoryEntry> entries;

    /**
     * Opens the inventory at the given path, loading existing content when present.
     *
     * @param inventoryPath path to {@code scenario-inventory.yaml}
     */
    public MigrationInventoryService(Path inventoryPath) {
        this.inventoryPath = inventoryPath;
        this.yamlMapper = new ObjectMapper(new YAMLFactory())
                .rebuild()
                .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
        this.entries = loadFromDisk();
    }

    /**
     * Returns all inventory entries (defensive copy).
     *
     * @return inventory rows
     */
    public List<MigrationInventoryEntry> listAll() {
        return List.copyOf(entries);
    }

    /**
     * Replaces the in-memory inventory and writes it to disk.
     *
     * @param templates new inventory rows
     */
    public void saveAll(List<MigrationInventoryEntry> templates) {
        this.entries = new ArrayList<>(templates);
        writeToDisk();
    }

    /**
     * Finds an entry by stable inventory id.
     *
     * @param id inventory id
     * @return matching entry, if any
     */
    public Optional<MigrationInventoryEntry> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return entries.stream()
                .filter(entry -> id.equals(entry.getId()))
                .findFirst();
    }

    private List<MigrationInventoryEntry> loadFromDisk() {
        if (!Files.exists(inventoryPath)) {
            return new ArrayList<>();
        }
        try {
            MigrationInventoryFile file = yamlMapper.readValue(inventoryPath.toFile(), MigrationInventoryFile.class);
            if (file.templates == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(file.templates);
        }
        catch (JacksonException e) {
            throw new DataGeneratorException(
                    "Failed to load migration inventory [" + inventoryPath + "]", e);
        }
    }

    private void writeToDisk() {
        try {
            if (inventoryPath.getParent() != null) {
                Files.createDirectories(inventoryPath.getParent());
            }
            MigrationInventoryFile file = new MigrationInventoryFile();
            file.version = INVENTORY_VERSION;
            file.templates = new ArrayList<>(entries);
            yamlMapper.writeValue(inventoryPath.toFile(), file);
        }
        catch (JacksonException | IOException e) {
            throw new DataGeneratorException(
                    "Failed to save migration inventory [" + inventoryPath + "]", e);
        }
    }

    private static final class MigrationInventoryFile {
        public int version = INVENTORY_VERSION;
        public List<MigrationInventoryEntry> templates = Collections.emptyList();
    }
}
