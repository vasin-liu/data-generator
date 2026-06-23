/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.template.TemplateV2Validator.UdfReference;
import org.gensokyo.data.udf.ScriptUdfPayload;
import org.gensokyo.data.udf.UdfLifecycleState;
import org.gensokyo.data.udf.UdfRecord;
import org.gensokyo.data.udf.UdfRegistryException;
import org.gensokyo.data.udf.UdfRegistryService;
import org.gensokyo.data.udf.UdfType;
import org.gensokyo.data.udf.UdfValidationError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Publish-time validator that resolves a template's UDF references against the registry (UDF-06, D-09/D-12).
 *
 * <p>References are extracted by {@link TemplateV2Validator#collectUdfReferences(TemplateV2VO)}: script
 * {@code udfRef:{id,version?}} blocks resolve through {@link UdfRegistryService#resolve} so the registry's own
 * lifecycle checks raise {@code UDF_NOT_FOUND}/{@code UDF_NOT_PUBLISHED}/{@code UDF_DEPRECATED}; SQL
 * {@code sqlName} tokens resolve against the published SQL-type UDFs by their {@code sqlName} metadata, raising
 * {@code UDF_NOT_FOUND} when none matches. Every failure is collected and surfaced as a single structured
 * {@link UdfRegistryException} carrying the offending template path as the error {@code field} (D-12). This is a
 * publish-only gate; draft saves stay lenient toward dangling references (D-11).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
@Component
public class UdfReferenceValidator {

    private final UdfRegistryService udfRegistryService;

    /**
     * @param udfRegistryService registry facade used to resolve template UDF references
     */
    public UdfReferenceValidator(UdfRegistryService udfRegistryService) {
        this.udfRegistryService = udfRegistryService;
    }

    /**
     * Validates that every UDF referenced by the template resolves to a published registry entry.
     *
     * @param normalized normalized Template V2 definition
     * @throws UdfRegistryException when any reference is unknown, unpublished, or deprecated; the exception's
     *                              structured errors list one entry per failed reference with its template path
     */
    public void validate(TemplateV2VO normalized) {
        List<UdfReference> references = TemplateV2Validator.collectUdfReferences(normalized);
        if (references.isEmpty()) {
            return;
        }
        List<UdfValidationError> violations = new ArrayList<>();
        for (UdfReference reference : references) {
            try {
                switch (reference.kind()) {
                    case SCRIPT -> udfRegistryService.resolve(reference.reference(),
                            Optional.ofNullable(reference.version()));
                    case SQL -> resolveSqlName(reference.reference());
                }
            }
            catch (UdfRegistryException exception) {
                // Re-tag with the template path so the operator sees exactly which reference failed (D-12).
                violations.add(new UdfValidationError(exception.code(), reference.path(), exception.getMessage()));
            }
        }
        if (!violations.isEmpty()) {
            String message = "Template references unresolved UDFs: " + violations.stream()
                    .map(UdfValidationError::formatted)
                    .reduce((left, right) -> left + "; " + right)
                    .orElse("");
            throw new UdfRegistryException(violations.get(0).code(), message, violations);
        }
    }

    private void resolveSqlName(String sqlName) {
        // A SQL transform token resolves only to a PUBLISHED SQL-type UDF whose payload sqlName matches.
        // The sqlName is read from the payload envelope — the same source the Calcite runtime registers
        // from (DefaultRegistrySqlFunctionSource) — so publish validation and execution stay consistent.
        List<UdfRecord> sqlUdfs = udfRegistryService.list(Optional.of(UdfType.SQL));
        boolean matched = sqlUdfs.stream()
                .filter(record -> record.state() == UdfLifecycleState.PUBLISHED)
                .anyMatch(record -> sqlName.equalsIgnoreCase(payloadSqlName(record)));
        if (!matched) {
            throw new UdfRegistryException("UDF_NOT_FOUND",
                    "No published SQL UDF registered with sqlName " + sqlName);
        }
    }

    private static String payloadSqlName(UdfRecord record) {
        try {
            return ScriptUdfPayload.parse(record.payload()).sqlName();
        } catch (UdfRegistryException malformed) {
            // A published SQL UDF with an unparseable payload simply cannot match any reference.
            return null;
        }
    }
}
