/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.GeoAssetSummaryView;
import org.gensokyo.data.api.console.dto.GeoAssetUploadView;
import org.gensokyo.data.api.console.dto.GeoPreviewLocationRequest;
import org.gensokyo.data.api.console.dto.GeoSyntheticPreviewRequest;
import org.gensokyo.data.api.console.dto.GeoSyntheticPreviewView;
import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.po.GeoAssetPO;
import org.gensokyo.data.repository.GeoAssetRepository;
import org.gensokyo.data.security.ConsoleActorHolder;
import org.gensokyo.kit.character.StrKit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD and runtime resolution for durable GeoJSON assets (GEO-05, GEO-08, D-04/D-09).
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
@Service
@RequiredArgsConstructor
public class GeoAssetService implements GeoAssetResolver {

    /** Audit action code for successful uploads (GOV-01). */
    public static final String AUDIT_ACTION_UPLOAD = "GEO_ASSET_UPLOAD";

    /** Audit action code for successful hard deletes (GOV-01 / D-11). */
    public static final String AUDIT_ACTION_DELETE = "GEO_ASSET_DELETE";

    /** Audit resource type for geo assets. */
    public static final String AUDIT_RESOURCE_TYPE = "GEO_ASSET";

    /** Hard sample-size cap for console synthetic preview honesty (D-08 / T-22-01). */
    public static final int PREVIEW_MAX_COUNT = 500;

    private final GeoAssetRepository repository;
    private final AuditService auditService;
    private final DataGeneratorProperties properties;
    private final GeoAssetReferenceScanner referenceScanner;

    /**
     * Validates, persists, and audits a multipart GeoJSON upload.
     *
     * @param file  multipart GeoJSON file
     * @param name  optional display name (defaults to original filename)
     * @param actor optional actor override; uses {@link ConsoleActorHolder} when blank
     * @return upload summary with assigned id
     * @throws IOException when the multipart file cannot be read
     * @throws IllegalArgumentException when validation fails
     */
    @Transactional
    public GeoAssetUploadView upload(MultipartFile file, String name, String actor) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("GeoJSON upload requires a non-empty file part");
        }
        DataGeneratorProperties.GeoAssets limits = properties.getGeoAssets();
        GeoAssetIngestSupport.IngestResult ingest = GeoAssetIngestSupport.ingest(
                file.getBytes(), limits.getMaxBytes(), limits.getMaxFeatures());

        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        String displayName = resolveName(name, file.getOriginalFilename());

        GeoAssetPO row = new GeoAssetPO();
        row.setId(id);
        row.setName(displayName);
        row.setContentType(ingest.contentType());
        row.setGeojsonClob(ingest.geoJsonText());
        row.setFeatureCount(ingest.featureCount());
        row.setMinLon(ingest.minLon());
        row.setMinLat(ingest.minLat());
        row.setMaxLon(ingest.maxLon());
        row.setMaxLat(ingest.maxLat());
        row.setGeometrySummary(ingest.geometrySummary());
        row.setContentSha256(ingest.contentSha256());
        row.setUploadedBy(resolveActor(actor));
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        repository.saveAndFlush(row);

        auditService.record(
                AUDIT_ACTION_UPLOAD,
                AUDIT_RESOURCE_TYPE,
                id.toString(),
                Map.of("name", displayName, "featureCount", ingest.featureCount()));
        return GeoAssetUploadView.from(row);
    }

    /**
     * Lists all assets without GeoJSON bodies, newest first.
     *
     * @return summary views
     */
    @Transactional(readOnly = true)
    public List<GeoAssetSummaryView> list() {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(GeoAssetSummaryView::from)
                .toList();
    }

    /**
     * Returns metadata for one asset.
     *
     * @param id asset UUID
     * @return summary view
     * @throws IllegalArgumentException when unknown
     */
    @Transactional(readOnly = true)
    public GeoAssetSummaryView getSummary(UUID id) {
        return GeoAssetSummaryView.from(requireRow(id));
    }

    /**
     * Returns the authoritative GeoJSON body for one asset.
     *
     * @param id asset UUID
     * @return stored GeoJSON text
     * @throws IllegalArgumentException when unknown
     */
    @Transactional(readOnly = true)
    public String getGeoJsonBody(UUID id) {
        return requireRow(id).getGeojsonClob();
    }

    /**
     * Hard-deletes a geo asset when no active template references it (D-08).
     *
     * @param id asset UUID
     * @throws GeoAssetInUseException when templates still reference the asset
     * @throws IllegalArgumentException when unknown
     */
    @Transactional
    public void delete(UUID id) {
        GeoAssetPO row = requireRow(id);
        List<GeoAssetTemplateUsage> usages = referenceScanner.findUsages(id);
        if (!usages.isEmpty()) {
            String names = usages.stream()
                    .map(u -> u.templateName() + " (" + u.templateId() + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            throw new GeoAssetInUseException(
                    "Geo asset is referenced by template(s): " + names,
                    usages);
        }
        repository.deleteById(id);
        auditService.record(
                AUDIT_ACTION_DELETE,
                AUDIT_RESOURCE_TYPE,
                id.toString(),
                Map.of("name", row.getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveUtf8(String assetId) {
        UUID id = parseAssetId(assetId);
        return requireRow(id).getGeojsonClob();
    }

    /**
     * Resolves path/classpath/{@code asset:} GeoJSON for console map underlays via the Phase 21 spine (D-06).
     *
     * @param request location request
     * @return UTF-8 GeoJSON text
     * @throws IOException when the resource cannot be read
     * @throws IllegalArgumentException when location is blank or cannot be resolved
     */
    @Transactional(readOnly = true)
    public String previewLocation(GeoPreviewLocationRequest request) throws IOException {
        // RED stub — Task 1 GREEN wires GeoResourceResolver.readUtf8(location, this).
        throw new UnsupportedOperationException("previewLocation not implemented");
    }

    /**
     * Generates a capped synthetic point sample for console map preview (D-08).
     *
     * @param request mode config + seed + maxCount
     * @return FeatureCollection preview with seed and effective sample count
     * @throws IOException when boundary/network GeoJSON cannot be read
     * @throws IllegalArgumentException when maxCount exceeds {@link #PREVIEW_MAX_COUNT} or config is invalid
     */
    @Transactional(readOnly = true)
    public GeoSyntheticPreviewView previewSynthetic(GeoSyntheticPreviewRequest request) throws IOException {
        // RED stub — Task 1 GREEN wires GeoSyntheticGenerator.generateRows with this resolver.
        throw new UnsupportedOperationException("previewSynthetic not implemented");
    }

    private GeoAssetPO requireRow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown geo asset id: " + id));
    }

    private static UUID parseAssetId(String assetId) {
        if (StrKit.isBlank(assetId)) {
            throw new IllegalArgumentException("Geo asset id must not be blank");
        }
        try {
            return UUID.fromString(assetId.strip());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid geo asset id: " + assetId.strip(), ex);
        }
    }

    private static String resolveName(String name, String originalFilename) {
        if (name != null && !name.isBlank()) {
            return name.strip();
        }
        if (originalFilename != null && !originalFilename.isBlank()) {
            return originalFilename.strip();
        }
        return "geo-asset";
    }

    private static String resolveActor(String actor) {
        if (actor != null && !actor.isBlank()) {
            return actor.strip();
        }
        return ConsoleActorHolder.currentActor();
    }
}
