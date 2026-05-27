/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.console.migration;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateV1Loader;
import org.gensokyo.data.template.migration.MigrationBacklogFilter;
import org.gensokyo.data.template.migration.MigrationBusinessSignoffRequest;
import org.gensokyo.data.template.migration.MigrationCompareOptions;
import org.gensokyo.data.template.migration.MigrationCompareWorkflow;
import org.gensokyo.data.template.migration.MigrationComparisonReport;
import org.gensokyo.data.template.migration.MigrationDraftService;
import org.gensokyo.data.template.migration.MigrationInventoryBacklogService;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventoryService;
import org.gensokyo.data.template.migration.MigrationInventorySummary;
import org.gensokyo.data.template.migration.MigrationInventorySummaryService;
import org.gensokyo.data.template.migration.MigrationPromoteService;
import org.gensokyo.data.template.migration.TemplateMigrationAnalysisDTO;
import org.gensokyo.data.template.migration.V1TemplateMigrationAnalyzer;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.collect.CollectKit;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Facade for operator-console migration actions (same behavior as {@code TemplateController} migration APIs).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class MigrationConsoleService {

    private final TemplateRepository repository;
    private final MigrationDraftService migrationDraftService;
    private final MigrationCompareWorkflow migrationCompareWorkflow;
    private final MigrationPromoteService migrationPromoteService;
    private final MigrationInventoryService migrationInventoryService;
    private final YamlParser yamlParser;

    /**
     * @return aggregate inventory statistics
     */
    public MigrationInventorySummary summary() {
        return new MigrationInventorySummaryService().summarize(migrationInventoryService);
    }

    /**
     * @param filter optional backlog filter name (e.g. {@code READY})
     * @return filtered inventory rows
     */
    public List<MigrationInventoryEntry> backlog(String filter) {
        MigrationBacklogFilter backlogFilter = MigrationInventoryBacklogService.parseFilter(filter);
        return new MigrationInventoryBacklogService()
                .filter(migrationInventoryService.listAll(), backlogFilter);
    }

    /**
     * @param templateId persisted template id
     * @return V1 migration analysis
     */
    public TemplateMigrationAnalysisDTO analyze(Long templateId) {
        TemplatePO entity = requireEntity(templateId);
        TemplateVO v1 = new TemplateV1Loader(yamlParser).load(entity);
        return V1TemplateMigrationAnalyzer.analyze(v1);
    }

    /**
     * @param templateId persisted template id
     * @return V2 migration draft (not persisted)
     */
    public TemplateV2DraftVO buildDraft(Long templateId) {
        TemplatePO entity = requireEntity(templateId);
        TemplateV2DraftVO draft = migrationDraftService.buildDraft(new TemplateV1Loader(yamlParser).load(entity));
        if (Objects.isNull(draft) || CollectKit.isEmpty(draft.getSources())) {
            throw new IllegalArgumentException(String.format(
                    "Template '%s' could not be converted into a V2 draft", templateId));
        }
        return draft;
    }

    /**
     * @param templateId persisted template id
     * @param options    optional compare options
     * @return comparison report
     */
    public MigrationComparisonReport compare(Long templateId, MigrationCompareOptions options) {
        TemplatePO entity = requireEntity(templateId);
        return migrationCompareWorkflow.compareAndPersist(templateId, entity, options);
    }

    /**
     * @param templateId persisted template id
     * @param request    sign-off details
     * @return updated inventory row
     */
    public MigrationInventoryEntry signoff(Long templateId, MigrationBusinessSignoffRequest request) {
        return migrationInventoryService.recordBusinessSignoff(inventoryId(templateId), request);
    }

    /**
     * @param templateId persisted template id
     * @return promoted draft
     */
    public TemplateV2DraftVO promote(Long templateId) {
        return migrationPromoteService.promote(templateId);
    }

    /**
     * @param templateId persisted template id
     * @return inventory row when {@code db-{id}} exists
     */
    public Optional<MigrationInventoryEntry> inventoryForTemplate(Long templateId) {
        return migrationInventoryService.findById(inventoryId(templateId));
    }

    private TemplatePO requireEntity(Long templateId) {
        return repository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Template '%s' does not exist", templateId)));
    }

    private static String inventoryId(Long templateId) {
        return "db-" + templateId;
    }
}
