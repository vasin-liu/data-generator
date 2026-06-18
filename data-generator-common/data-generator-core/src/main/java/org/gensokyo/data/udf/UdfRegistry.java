/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Programmatic UDF registry contract (in-memory in Phase 2).
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
public interface UdfRegistry {

    /**
     * Registers a draft UDF version with inline payload.
     *
     * @param udfId    reverse-DNS id
     * @param version  semver
     * @param type     UDF type
     * @param payload  artifact bytes
     * @param metadata optional metadata (sqlName, schemas, etc.)
     * @return created draft record
     * @throws UdfRegistryException when validation fails or duplicate version exists
     */
    UdfRecord registerDraft(String udfId, String version, UdfType type, byte[] payload, Map<String, String> metadata);

    /**
     * Marks a draft entry as published.
     *
     * @param udfId   stable id
     * @param version semver
     * @return updated record
     * @throws UdfRegistryException when not found or invalid transition
     */
    UdfRecord publish(String udfId, String version);

    /**
     * Marks a published entry as deprecated.
     *
     * @param udfId   stable id
     * @param version semver
     * @return updated record
     * @throws UdfRegistryException when not found or invalid transition
     */
    UdfRecord deprecate(String udfId, String version);

    /**
     * @param typeFilter optional type filter
     * @return all matching records
     */
    List<UdfRecord> list(Optional<UdfType> typeFilter);

    /**
     * @param udfId   stable id
     * @param version semver
     * @return record if present
     */
    Optional<UdfRecord> find(String udfId, String version);

    /**
     * Resolves a published record; uses latest published when version empty.
     *
     * @param udfId          stable id
     * @param versionOptional explicit version or empty for latest published
     * @return published record
     * @throws UdfRegistryException when not found or not published
     */
    UdfRecord resolve(String udfId, Optional<String> versionOptional);
}
