/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.TemplateSummaryDto;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
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

    /**
     * @param includeArchived when true, includes archived templates
     * @param q             optional filter on name or id substring
     * @return catalog rows
     */
    @GetMapping
    public R<List<TemplateSummaryDto>> list(
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(required = false) String q) {
        List<TemplatePO> rows = includeArchived
                ? templateRepository.findAll()
                : templateRepository.findByArchivedFalse();
        if (q != null && !q.isBlank()) {
            String lower = q.toLowerCase(Locale.ROOT);
            rows = rows.stream().filter(row -> matchesFilter(row, lower)).toList();
        }
        return R.ok(rows.stream().map(TemplateSummaryDto::from).toList());
    }

    private static boolean matchesFilter(TemplatePO row, String lower) {
        if (row.getName() != null && row.getName().toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        return row.getId() != null && String.valueOf(row.getId()).contains(lower);
    }
}
