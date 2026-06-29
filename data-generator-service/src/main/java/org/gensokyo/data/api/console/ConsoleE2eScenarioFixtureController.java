/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.template.E2eV2ScenarioFixtureService;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotNull;

/**
 * E2E-only scenario fixture hooks for Playwright RW streaming/upsert specs (Phase 8, D-23).
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/api/e2e/scenarios")
@Profile("e2e")
@Validated
@RequiredArgsConstructor
public class ConsoleE2eScenarioFixtureController {

    private final E2eV2ScenarioFixtureService fixtureService;

    /**
     * @return whether embedded H2 supports PostgreSQL upsert SQL for GF-GP scenarios (W-01)
     */
    @GetMapping("/postgres-upsert-supported")
    public R<Boolean> postgresUpsertSupported() {
        return R.ok(fixtureService.h2SupportsPostgresUpsert());
    }

    /**
     * Mutates upsert source rows before a second GF-GP/GF-GM run (D-15 idempotency proof).
     *
     * @param scenarioId official catalog id ({@code GF-GP} or {@code GF-GM})
     * @return empty success envelope
     */
    @PostMapping("/{scenarioId}/mutate-upsert-source")
    public R<Void> mutateUpsertSource(@NotNull @PathVariable String scenarioId) {
        fixtureService.mutateUpsertSourceForSecondRun(scenarioId);
        return R.ok(null);
    }
}
