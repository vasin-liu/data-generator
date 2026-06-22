/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.TransformCatalogEntryView;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.udf.TransformCatalogSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Operator-facing read-only catalog of available Template V2 transforms (XFORM-01, D-05).
 *
 * <p>Exposes a single discovery endpoint that lists built-in operators and published UDFs in one unified
 * response (D-06), each entry carrying rich authoring metadata (D-07). API-only this phase — no console-web
 * surface. Responses use the {@link R} envelope; an unknown {@code kind} throws
 * {@link IllegalArgumentException} which {@code ConsoleApiAdvice} maps to 400 (no controller-side try/catch).</p>
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
@RestController
@RequestMapping("/api/console/transforms")
@RequiredArgsConstructor
public class ConsoleTransformCatalogController {

    private final TransformCatalogSource transformCatalogSource;

    /**
     * Lists the unified transform catalog, optionally filtered by source kind.
     *
     * @param kind optional source filter ({@code BUILTIN}/{@code UDF})
     * @return catalog entries for built-in operators and published UDFs
     */
    @GetMapping
    public R<List<TransformCatalogEntryView>> list(@RequestParam(required = false) String kind) {
        Optional<String> kindFilter = (kind == null || kind.isBlank()) ? Optional.empty() : Optional.of(kind);
        return R.ok(transformCatalogSource.entries(kindFilter));
    }
}
