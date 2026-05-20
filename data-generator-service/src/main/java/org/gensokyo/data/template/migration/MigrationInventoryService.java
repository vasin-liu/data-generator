/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.repository.TemplateRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Marks a database template as promoted (V2 draft persisted); retains {@code migrationClass} from the last compare when set.
     *
     * @param templateId persisted template id
     */
    public void updatePromoteResult(Long templateId) {
        if (templateId == null) {
            return;
        }
        String inventoryId = "db-" + templateId;
        MigrationInventoryEntry entry = findById(inventoryId).orElseGet(() -> {
            MigrationInventoryEntry created = new MigrationInventoryEntry();
            created.setId(inventoryId);
            created.setOrigin("database");
            created.setDbTemplateId(templateId);
            entries.add(created);
            return created;
        });
        entry.setV2DraftPresent(true);
        writeToDisk();
    }

    /**
     * Updates inventory for a database template after compare (classification and report path).
     *
     * @param templateId persisted template id
     * @param report     compare report with classification set
     * @param reportPath relative report path written under {@code docs/migration/reports/}
     */
    public void updateCompareResult(Long templateId, MigrationComparisonReport report, String reportPath) {
        if (templateId == null || report == null) {
            return;
        }
        String inventoryId = "db-" + templateId;
        MigrationInventoryEntry entry = findById(inventoryId).orElseGet(() -> {
            MigrationInventoryEntry created = new MigrationInventoryEntry();
            created.setId(inventoryId);
            created.setOrigin("database");
            created.setDbTemplateId(templateId);
            entries.add(created);
            return created;
        });
        if (report.getClassification() != null) {
            entry.setMigrationClass(report.getClassification());
        }
        entry.setLastCompareReportPath(reportPath);
        entry.setV2DraftPresent(true);
        writeToDisk();
    }

    /**
     * Records business sign-off on an inventory row (P3 gate before promote).
     *
     * @param inventoryId stable inventory id
     * @param request       sign-off details
     * @return updated entry
     * @throws IllegalArgumentException when the inventory id is unknown
     */
    public MigrationInventoryEntry recordBusinessSignoff(String inventoryId, MigrationBusinessSignoffRequest request) {
        if (inventoryId == null || inventoryId.isBlank()) {
            throw new IllegalArgumentException("inventoryId must be set");
        }
        MigrationBusinessSignoffRequest body = request != null ? request : new MigrationBusinessSignoffRequest();
        MigrationInventoryEntry entry = findById(inventoryId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown inventory id: " + inventoryId));
        entry.setBusinessSignoffApproved(body.isApproved());
        entry.setBusinessSignoffBy(body.getApprovedBy());
        entry.setBusinessSignoffAt(Instant.now().toString());
        if (body.getNotes() != null && !body.getNotes().isBlank()) {
            String existing = entry.getNotes();
            entry.setNotes(existing == null || existing.isBlank()
                    ? body.getNotes().strip()
                    : existing + " | signoff: " + body.getNotes().strip());
        }
        writeToDisk();
        return entry;
    }

    /**
     * Returns the path to the on-disk scenario inventory file.
     *
     * @return inventory YAML path
     */
    public String inventoryPathString() {
        return inventoryPath.toString();
    }

    /**
     * Merges V1 database templates into the inventory (ids {@code db-{templateId}}), skipping ids already present.
     *
     * @param repository template persistence
     * @return counts and whether the inventory file was updated
     */
    public MigrationInventoryRefreshResult refreshFromRepository(TemplateRepository repository) {
        MigrationInventorySeeder seeder = new MigrationInventorySeeder();
        Set<String> existingIds = new HashSet<>();
        for (MigrationInventoryEntry entry : entries) {
            if (entry.getId() != null) {
                existingIds.add(entry.getId());
            }
        }
        int added = 0;
        for (MigrationInventoryEntry dbEntry : seeder.entriesFromDatabase(repository)) {
            if (!existingIds.contains(dbEntry.getId())) {
                entries.add(dbEntry);
                existingIds.add(dbEntry.getId());
                added++;
            }
        }
        boolean persisted = added > 0;
        if (persisted) {
            writeToDisk();
        }
        return MigrationInventoryRefreshResult.of(
                added, entries.size(), inventoryPathString(), persisted);
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
