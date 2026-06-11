/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.TemplateSummaryDto;
import org.gensokyo.data.api.console.dto.TemplateTaxonomyDto;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.editor.TemplateEditorService;
import org.gensokyo.data.template.editor.TemplateMetadataSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * Template catalog for the React templates grid.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class ConsoleTemplateController {

    private final TemplateRepository templateRepository;
    private final TemplateEditorService templateEditorService;

    /**
     * @param includeArchived when true, includes archived templates
     * @param q             optional filter on name or id substring
     * @return catalog rows
     */
    @GetMapping
    public R<List<TemplateSummaryDto>> list(
            @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "tag", required = false) String tag) {
        List<TemplatePO> rows = includeArchived
                ? templateRepository.findAll()
                : templateRepository.findByArchivedFalse();
        if (q != null && !q.isBlank()) {
            String lower = q.toLowerCase(Locale.ROOT);
            rows = rows.stream().filter(row -> matchesFilter(row, lower)).toList();
        }
        if (category != null && !category.isBlank() || tag != null && !tag.isBlank()) {
            rows = rows.stream()
                    .filter(row -> TemplateMetadataSupport.matchesTaxonomy(row, category, tag))
                    .toList();
        }
        return R.ok(rows.stream()
                .filter(row -> templateEditorService.detectDefinitionKind(row) != TemplateDefinitionKind.V1)
                .map(TemplateSummaryDto::from)
                .toList());
    }

    /**
     * @return distinct categories and tags for catalog filters
     */
    @GetMapping("/taxonomy")
    public R<TemplateTaxonomyDto> taxonomy() {
        List<TemplatePO> rows = templateRepository.findByArchivedFalse();
        return R.ok(new TemplateTaxonomyDto(
                TemplateMetadataSupport.distinctCategories(rows),
                TemplateMetadataSupport.distinctTags(rows)));
    }

    private static boolean matchesFilter(TemplatePO row, String lower) {
        if (row.getName() != null && row.getName().toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        return row.getId() != null && String.valueOf(row.getId()).contains(lower);
    }
}
