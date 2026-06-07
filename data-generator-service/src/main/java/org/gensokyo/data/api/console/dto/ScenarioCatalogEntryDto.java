/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

/**
 * Official V2 scenario catalog row for the console create-from-scenario wizard.
 *
 * @param scenarioId   stable catalog id (e.g. {@code GF-A})
 * @param family       roadmap family code ({@code A}, {@code B}, {@code WF}, {@code JS})
 * @param name         template name from scenario YAML
 * @param catalogRef   human-readable catalog reference from scenario header comments
 * @param resourceFile classpath file name under {@code template/v2-scenarios/}
 * @author Gensokyo
 * @since 2026-06-02
 */
public record ScenarioCatalogEntryDto(
        String scenarioId,
        String family,
        String name,
        String catalogRef,
        String resourceFile) {
}
