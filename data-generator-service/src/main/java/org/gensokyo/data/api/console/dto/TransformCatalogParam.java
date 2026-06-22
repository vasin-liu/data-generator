/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

/**
 * One configuration field of a transform catalog entry (D-07).
 *
 * <p>Describes a single YAML parameter an operator can set on a built-in transform: its name, declared
 * type, whether it is required, and a short human-readable description. Carries metadata only — never any
 * value, payload, or secret.</p>
 *
 * @param name        parameter name as written in the template YAML
 * @param type        declared parameter type (e.g. {@code string}, {@code boolean}, {@code list})
 * @param required    whether the parameter must be supplied
 * @param description short human-readable description of the parameter
 * @author Gensokyo
 * @since 2026-06-22
 */
public record TransformCatalogParam(String name, String type, boolean required, String description) {
}
