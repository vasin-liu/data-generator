/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.GeoAssetSummaryView;
import org.gensokyo.data.api.console.dto.GeoAssetUploadView;
import org.gensokyo.data.api.console.dto.GeoPreviewLocationRequest;
import org.gensokyo.data.api.console.dto.GeoSyntheticPreviewRequest;
import org.gensokyo.data.api.console.dto.GeoSyntheticPreviewView;
import org.gensokyo.data.geo.GeoAssetService;
import org.gensokyo.data.model.vo.R;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Operator-facing REST surface for durable GeoJSON assets (D-05/D-10) and Phase 22 preview helpers (D-06/D-08).
 *
 * <p>Multipart upload, list, metadata get, and raw GeoJSON body fetch. Preview endpoints reuse
 * {@link GeoAssetService} resolve/generate spine — never a parallel classpath/filesystem client.
 * Failures bubble to {@link ConsoleApiAdvice}; delete returns 409 when templates still reference the asset (D-08).
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
@RestController
@RequestMapping("/api/console/geo-assets")
@RequiredArgsConstructor
public class ConsoleGeoAssetController {

    private final GeoAssetService geoAssetService;

    /**
     * Uploads and registers a validated GeoJSON asset.
     *
     * @param file multipart GeoJSON ({@code Feature} or {@code FeatureCollection} root)
     * @param name optional display name
     * @return upload summary with assigned UUID
     * @throws IOException when the file cannot be read
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<GeoAssetUploadView> upload(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String name) throws IOException {
        return R.ok(geoAssetService.upload(file, name, null));
    }

    /**
     * Lists registered assets without GeoJSON bodies.
     *
     * @return summary views newest first
     */
    @GetMapping
    public R<List<GeoAssetSummaryView>> list() {
        return R.ok(geoAssetService.list());
    }

    /**
     * Returns metadata for one asset.
     *
     * @param id asset UUID
     * @return summary view
     */
    @GetMapping("/{id}")
    public R<GeoAssetSummaryView> get(@PathVariable UUID id) {
        return R.ok(geoAssetService.getSummary(id));
    }

    /**
     * Returns the authoritative GeoJSON body for map layers and runtime preview (D-10).
     *
     * @param id asset UUID
     * @return raw {@code application/geo+json} body (not {@link R}-wrapped)
     */
    @GetMapping(value = "/{id}/geojson", produces = "application/geo+json")
    public ResponseEntity<byte[]> geoJson(@PathVariable UUID id) {
        String body = geoAssetService.getGeoJsonBody(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/geo+json"))
                .body(body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Resolves path/classpath/{@code asset:} GeoJSON for map underlays via the Phase 21 spine (D-06).
     *
     * @param request location body
     * @return raw {@code application/geo+json} body (not {@link R}-wrapped)
     * @throws IOException when the resource cannot be read
     */
    @PostMapping(value = "/preview/location", produces = "application/geo+json")
    public ResponseEntity<byte[]> previewLocation(@RequestBody GeoPreviewLocationRequest request) throws IOException {
        String body = geoAssetService.previewLocation(request);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/geo+json"))
                .body(body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns a capped synthetic point sample for console map honesty preview (D-08).
     *
     * @param request mode config, seed, and maxCount ({@code ≤ 500})
     * @return {@link R}-wrapped FeatureCollection with seed and effective sample count
     * @throws IOException when boundary/network GeoJSON cannot be read
     */
    @PostMapping("/preview/synthetic")
    public R<GeoSyntheticPreviewView> previewSynthetic(@RequestBody GeoSyntheticPreviewRequest request)
            throws IOException {
        return R.ok(geoAssetService.previewSynthetic(request));
    }

    /**
     * Hard-deletes a geo asset when no template references it (GEO-09 / D-08).
     *
     * @param id asset UUID
     * @return success confirmation
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable UUID id) {
        geoAssetService.delete(id);
        return R.ok("Geo asset deleted");
    }
}
