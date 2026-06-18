/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the UDF publish gate: governance checks, registry transition, audit, and runtime refresh.
 *
 * <p>Governance runs only at publish time (D-21). On success the published entry is appended to the
 * audit log (D-24) and the Template V2 runtime registry is refreshed so the new SQL/script function
 * becomes resolvable immediately (D-08). Governance failures are rejected with structured codes (D-27).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
@Service
public class UdfPublishService {

    private final UdfRegistry registry;
    private final AuditService auditService;
    private final TemplateV2RuntimeRegistryProvider runtimeRegistryProvider;
    private final DataGeneratorProperties properties;

    /**
     * @param registry                backing UDF registry
     * @param auditService            append-only audit log
     * @param runtimeRegistryProvider refreshable runtime registry (nullable in minimal contexts)
     * @param properties              governance configuration
     */
    public UdfPublishService(UdfRegistry registry,
                             AuditService auditService,
                             @Nullable TemplateV2RuntimeRegistryProvider runtimeRegistryProvider,
                             DataGeneratorProperties properties) {
        this.registry = registry;
        this.auditService = auditService;
        this.runtimeRegistryProvider = runtimeRegistryProvider;
        this.properties = properties;
    }

    /**
     * Runs the publish gate for a draft UDF version.
     *
     * @param udfId   reverse-DNS id
     * @param version semver
     * @return published record
     * @throws UdfRegistryException when the entry is missing or fails governance
     */
    public UdfRecord publish(String udfId, String version) {
        UdfRecord draft = registry.find(udfId, version)
                .orElseThrow(() -> new UdfRegistryException("UDF_NOT_FOUND",
                        "No UDF [" + udfId + "@" + version + "] to publish"));

        boolean rejectPlaintext = properties.getGovernance().isRejectPlaintextPasswordsInTemplates();
        List<UdfValidationError> violations = UdfGovernanceSupport.check(draft, rejectPlaintext);
        if (!violations.isEmpty()) {
            throw new UdfRegistryException("UDF_GOVERNANCE_VIOLATION",
                    "UDF [" + udfId + "@" + version + "] failed governance: " + violations.size() + " violation(s)",
                    violations);
        }

        UdfRecord published = registry.publish(udfId, version);
        audit("UDF_PUBLISH", published);
        refreshRuntime();
        return published;
    }

    /**
     * Deprecates a published UDF version and refreshes the runtime.
     *
     * @param udfId   reverse-DNS id
     * @param version semver
     * @return deprecated record
     * @throws UdfRegistryException when the entry is missing or cannot be deprecated
     */
    public UdfRecord deprecate(String udfId, String version) {
        UdfRecord deprecated = registry.deprecate(udfId, version);
        audit("UDF_DEPRECATE", deprecated);
        refreshRuntime();
        return deprecated;
    }

    private void audit(String action, UdfRecord record) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("udfId", record.udfId());
        detail.put("version", record.version());
        detail.put("type", record.type().jsonName());
        // Audit detail intentionally excludes payload bytes to avoid leaking artifact contents.
        auditService.record(action, "udf", record.udfId() + "@" + record.version(), detail);
    }

    private void refreshRuntime() {
        if (runtimeRegistryProvider != null) {
            runtimeRegistryProvider.refresh();
        }
    }
}
