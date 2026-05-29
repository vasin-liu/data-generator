/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * V2-native policy for how a source's materialized rows are ordered, expanded, and capped before SQL
 * transforms consume them.
 *
 * <p>These semantics are defined for Template V2 only. They are <strong>not</strong> a reproduction of
 * V1 reader/value selector behavior; migrated templates should treat this policy as an explicit authoring
 * contract rather than a parity shim.</p>
 *
 * <h2>Modes</h2>
 * <ul>
 *   <li>{@code ORDERED} — preserve the underlying source row order; optional {@link #limit} truncates from
 *       the front.</li>
 *   <li>{@code LIMIT} — return the first {@link #limit} rows in source order; {@code limit} is required and
 *       must be positive.</li>
 *   <li>{@code ONCE} — emit each distinct row at most once, preserving first-seen source order; optional
 *       {@link #limit} caps how many unique rows are kept.</li>
 *   <li>{@code EQUAL} — reorder rows with a deterministic uniform shuffle driven by {@link #seed}; optional
 *       {@link #limit} keeps the first rows after shuffle. Every row has equal weight in the shuffle.</li>
 *   <li>{@code WEIGHTED} — expand each source row {@code i} to {@code weights[i]} copies, shuffle the
 *       expanded multiset with {@link #seed}, then optionally cap with {@link #limit}. Weights align with
 *       zero-based materialized row indices and must be positive when this mode is used.</li>
 * </ul>
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
public class MaterializationPolicyVO implements Serializable {
    /** Policy mode: {@code ORDERED}, {@code LIMIT}, {@code ONCE}, {@code EQUAL}, or {@code WEIGHTED}. */
    private String mode;
    /** Maximum number of rows to emit after the mode-specific ordering/expansion step. */
    private Integer limit;
    /** Seed for deterministic shuffle modes ({@code EQUAL}, {@code WEIGHTED}); defaults to {@code 0}. */
    private Long seed;
    /** Per-row weights aligned with pre-policy materialized row order; required for {@code WEIGHTED}. */
    private List<Integer> weights = new ArrayList<>();
}
