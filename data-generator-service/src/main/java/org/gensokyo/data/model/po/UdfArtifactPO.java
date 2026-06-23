/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * JPA entity for one versioned UDF artifact row (D-01).
 *
 * <p>Each row captures a single {@code udfId + version} entry of the unified UDF registry: its type,
 * lifecycle state, inline artifact bytes, serialized metadata, and lifecycle timestamps. The
 * {@code udf_id + version} pair is uniquely constrained so duplicate registration is rejected at the
 * persistence layer (D-08); a surrogate generated {@code id} is the primary key. The registry stays
 * global — there is intentionally no tenant column (D-03). The table is created through the existing
 * Hibernate {@code @Entity}-driven DDL path; no hand-written schema is required.
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
@Getter
@Setter
@Entity
@Table(name = "udf_artifact",
        uniqueConstraints = @UniqueConstraint(name = "uk_udf_artifact_id_version", columnNames = {"udf_id", "version"}))
public class UdfArtifactPO implements Serializable {

    /**
     * Surrogate primary key (composite identity is {@code udf_id + version}, enforced by a unique constraint).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reverse-DNS stable UDF identifier (e.g. {@code com.example.my_udf}).
     */
    @Column(name = "udf_id", nullable = false)
    private String udfId;

    /**
     * Semver version of this artifact (e.g. {@code 1.0.0}).
     */
    @Column(name = "version", nullable = false)
    private String version;

    /**
     * UDF type wire value: {@code java-plugin}, {@code script}, or {@code sql}.
     */
    @Column(name = "type", length = 16, nullable = false)
    private String type;

    /**
     * Lifecycle state: {@code DRAFT}, {@code PUBLISHED}, or {@code DEPRECATED}.
     */
    @Column(name = "state", length = 16, nullable = false)
    private String state;

    /**
     * Inline artifact bytes (JAR / script / SQL payload). Never echoed in logs or list DTOs.
     */
    // bytea (not @Lob/BLOB): H2 in PostgreSQL mode rejects BLOB, so the column type is pinned via
    // columnDefinition (same approach as metadata_json's CLOB) to keep Hibernate's create-drop DDL in
    // lockstep with db/schema.sql; H2 treats bytea as a large VARBINARY for the inline artifact bytes.
    @Column(columnDefinition = "bytea", name = "payload")
    private byte[] payload;

    /**
     * Serialized JSON of the metadata map (e.g. {@code sqlName}, schemas for SQL/script UDFs).
     */
    @Column(columnDefinition = "CLOB", name = "metadata_json")
    private String metadataJson;

    /**
     * Timestamp when the draft was registered.
     */
    @Column(name = "registered_at")
    private Instant registeredAt;

    /**
     * Timestamp when the artifact was published; null while in draft.
     */
    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Timestamp when the artifact was deprecated; null until deprecated.
     */
    @Column(name = "deprecated_at")
    private Instant deprecatedAt;
}
