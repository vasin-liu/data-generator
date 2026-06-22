/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.util.List;

/**
 * One entry in the unified transform catalog (D-06, D-07): a built-in operator or a published UDF.
 *
 * <p>Projects discovery metadata only — type, source kind, description, parameter schema, a usage example,
 * and (for SQL/script UDFs) the SQL-callable name. UDF artifact payload bytes are never exposed
 * (mirror {@link UdfVersionView} D-14). Internal {@code V2_*} scalar functions are not represented here
 * (D-12).</p>
 *
 * @param type        operator type name ({@code json}/{@code mask}/{@code lookup}/{@code sql}/...) or UDF id
 * @param kind        source discriminator: {@code BUILTIN} or {@code UDF}
 * @param description short human-readable description of the operator
 * @param params      parameter schema for the operator's config fields
 * @param example     a short YAML usage snippet
 * @param sqlName     SQL-callable function name for SQL/script UDFs; {@code null} for built-in operators
 * @author Gensokyo
 * @since 2026-06-22
 */
public record TransformCatalogEntryView(String type,
                                        String kind,
                                        String description,
                                        List<TransformCatalogParam> params,
                                        String example,
                                        String sqlName) {
}
