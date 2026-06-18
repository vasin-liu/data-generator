/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring-facing facade for programmatic UDF registry operations.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
@Service
public class UdfRegistryService {

    private final UdfRegistry registry;

    /**
     * @param registry backing registry bean
     */
    public UdfRegistryService(UdfRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param udfId    reverse-DNS id
     * @param version  semver
     * @param type     UDF type
     * @param payload  artifact bytes
     * @param metadata optional metadata
     * @return draft record
     */
    public UdfRecord registerDraft(String udfId, String version, UdfType type, byte[] payload,
                                   Map<String, String> metadata) {
        return registry.registerDraft(udfId, version, type, payload, metadata);
    }

    /**
     * @param udfId   stable id
     * @param version semver
     * @return published record
     */
    public UdfRecord publish(String udfId, String version) {
        return registry.publish(udfId, version);
    }

    /**
     * @param udfId   stable id
     * @param version semver
     * @return deprecated record
     */
    public UdfRecord deprecate(String udfId, String version) {
        return registry.deprecate(udfId, version);
    }

    /**
     * @param typeFilter optional type filter
     * @return matching records
     */
    public List<UdfRecord> list(Optional<UdfType> typeFilter) {
        return registry.list(typeFilter);
    }

    /**
     * @param udfId   stable id
     * @param version semver
     * @return record if present
     */
    public Optional<UdfRecord> find(String udfId, String version) {
        return registry.find(udfId, version);
    }

    /**
     * @param udfId   stable id
     * @param version optional explicit version
     * @return published record
     */
    public UdfRecord resolve(String udfId, Optional<String> version) {
        return registry.resolve(udfId, version);
    }

    /**
     * @return underlying registry (for tests and runtime bridges)
     */
    public UdfRegistry registry() {
        return registry;
    }
}
