/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import org.gensokyo.data.model.po.UdfArtifactPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UdfArtifactPO} rows.
 *
 * <p>Provides the finders the JDBC-backed registry needs: version history for an id (D-08), the
 * by-state scan used to rehydrate published UDFs on startup (D-02), and the point lookup that backs
 * the duplicate-version guard and lifecycle transitions (D-08).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
public interface UdfArtifactRepository extends JpaRepository<UdfArtifactPO, Long> {

    /**
     * Returns the full version history for one UDF id, ordered by version ascending (D-08).
     *
     * @param udfId reverse-DNS UDF identifier
     * @return all artifact rows for the id, oldest version first
     */
    List<UdfArtifactPO> findByUdfIdOrderByVersionAsc(String udfId);

    /**
     * Returns all artifact rows in a given lifecycle state (used to reload {@code PUBLISHED} rows, D-02).
     *
     * @param state lifecycle state name ({@code DRAFT}/{@code PUBLISHED}/{@code DEPRECATED})
     * @return matching artifact rows
     */
    List<UdfArtifactPO> findByState(String state);

    /**
     * Point lookup for the duplicate-version guard and lifecycle transitions (D-08).
     *
     * @param udfId   reverse-DNS UDF identifier
     * @param version semver version
     * @return the matching artifact row, if present
     */
    Optional<UdfArtifactPO> findByUdfIdAndVersion(String udfId, String version);
}
