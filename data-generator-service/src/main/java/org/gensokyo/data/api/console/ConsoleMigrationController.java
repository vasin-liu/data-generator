/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.template.migration.MigrationBusinessSignoffRequest;
import org.gensokyo.data.template.migration.MigrationCompareOptions;
import org.gensokyo.data.template.migration.MigrationComparisonReport;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventorySummary;
import org.gensokyo.data.template.migration.TemplateMigrationAnalysisDTO;
import org.gensokyo.data.console.migration.MigrationConsoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Console facade for migration inventory and per-template actions.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
public class ConsoleMigrationController {

    private final MigrationConsoleService migrationConsoleService;

    /**
     * @return aggregate KPI statistics
     */
    @GetMapping("/summary")
    public R<MigrationInventorySummary> summary() {
        return R.ok(migrationConsoleService.summary());
    }

    /**
     * @param filter optional backlog filter name (e.g. {@code READY})
     * @return filtered inventory rows
     */
    @GetMapping("/backlog")
    public R<List<MigrationInventoryEntry>> backlog(
            @RequestParam(name = "filter", required = false) String filter) {
        return R.ok(migrationConsoleService.backlog(filter));
    }

    /**
     * @param templateId persisted template id
     * @return V1 migration analysis
     */
    @GetMapping("/templates/{templateId}/analyze")
    public R<TemplateMigrationAnalysisDTO> analyze(@NotNull @PathVariable Long templateId) {
        return R.ok(migrationConsoleService.analyze(templateId));
    }

    /**
     * @param templateId persisted template id
     * @return V2 migration draft (not persisted)
     */
    @PostMapping("/templates/{templateId}/draft")
    public R<TemplateV2DraftVO> draft(@NotNull @PathVariable Long templateId) {
        return R.ok(migrationConsoleService.buildDraft(templateId));
    }

    /**
     * @param templateId persisted template id
     * @param options    optional compare tuning
     * @return comparison report
     */
    @PostMapping("/templates/{templateId}/compare")
    public R<MigrationComparisonReport> compare(
            @NotNull @PathVariable Long templateId,
            @RequestBody(required = false) MigrationCompareOptions options) {
        MigrationCompareOptions effective = options != null ? options : new MigrationCompareOptions();
        return R.ok(migrationConsoleService.compare(templateId, effective));
    }

    /**
     * @param templateId persisted template id
     * @param request    sign-off details
     * @return updated inventory row
     */
    @PostMapping("/templates/{templateId}/signoff")
    public R<MigrationInventoryEntry> signoff(
            @NotNull @PathVariable Long templateId,
            @RequestBody MigrationBusinessSignoffRequest request) {
        return R.ok(migrationConsoleService.signoff(templateId, request));
    }

    /**
     * @param templateId persisted template id
     * @return promoted V2 draft
     */
    @PostMapping("/templates/{templateId}/promote")
    public R<TemplateV2DraftVO> promote(@NotNull @PathVariable Long templateId) {
        return R.ok(migrationConsoleService.promote(templateId));
    }

    /**
     * @param templateId persisted template id
     * @return inventory row when {@code db-{id}} exists
     */
    @GetMapping("/templates/{templateId}/inventory")
    public R<MigrationInventoryEntry> inventory(@NotNull @PathVariable Long templateId) {
        Optional<MigrationInventoryEntry> entry = migrationConsoleService.inventoryForTemplate(templateId);
        if (entry.isPresent()) {
            return R.ok(entry.get());
        }
        return R.fail("No inventory row for db-" + templateId);
    }
}
