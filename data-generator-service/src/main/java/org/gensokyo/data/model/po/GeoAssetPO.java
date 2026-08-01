/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for one durable GeoJSON asset row in the metadata DB (D-04).
 *
 * <p>Validated GeoJSON text is stored as a CLOB; ingest metadata (bbox, feature count, checksum)
 * supports list APIs without loading the full body. The table is created through Hibernate entity DDL.
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
@Getter
@Setter
@Entity
@Table(name = "geo_asset")
public class GeoAssetPO implements Serializable {

    /**
     * Stable asset identifier assigned at upload.
     */
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Operator-facing display name.
     */
    @Column(name = "name", length = 256, nullable = false)
    private String name;

    /**
     * MIME type of the stored body (defaults to {@code application/geo+json}).
     */
    @Column(name = "content_type", length = 64, nullable = false)
    private String contentType;

    /**
     * Validated GeoJSON text (Feature or FeatureCollection root).
     */
    @Column(columnDefinition = "CLOB", name = "geojson_clob", nullable = false)
    private String geojsonClob;

    /**
     * Number of features parsed at ingest.
     */
    @Column(name = "feature_count", nullable = false)
    private int featureCount;

    @Column(name = "min_lon", nullable = false)
    private double minLon;

    @Column(name = "min_lat", nullable = false)
    private double minLat;

    @Column(name = "max_lon", nullable = false)
    private double maxLon;

    @Column(name = "max_lat", nullable = false)
    private double maxLat;

    /**
     * Optional summary of geometry types (e.g. {@code Polygon(2), Point(1)}).
     */
    @Column(name = "geometry_summary", length = 512)
    private String geometrySummary;

    /**
     * SHA-256 hex digest of the stored GeoJSON bytes.
     */
    @Column(name = "content_sha256", length = 64)
    private String contentSha256;

    /**
     * Console actor that performed the upload.
     */
    @Column(name = "uploaded_by", length = 128)
    private String uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
