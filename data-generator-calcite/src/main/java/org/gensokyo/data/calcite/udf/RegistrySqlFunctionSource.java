/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.udf;

import org.gensokyo.data.calcite.TemplateV2SqlFunction;

import java.util.List;

/**
 * Supplies SQL-callable functions contributed by published UDF registry entries.
 *
 * <p>Implementations live outside the calcite module (the registry is owned by the service
 * layer); this seam keeps the calcite runtime bridge decoupled from registry internals.
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
@FunctionalInterface
public interface RegistrySqlFunctionSource {

    /**
     * @return SQL functions derived from currently published SQL and script UDFs (never {@code null})
     */
    List<TemplateV2SqlFunction> publishedSqlFunctions();
}
