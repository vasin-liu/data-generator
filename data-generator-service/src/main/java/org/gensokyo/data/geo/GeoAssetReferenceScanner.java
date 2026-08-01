/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.GeoJsonSourceVO;
import org.gensokyo.data.model.v2.GeoSyntheticSourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.character.StrKit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scans stored templates for geo asset references before hard delete (D-08 / research §5).
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
@Component
@RequiredArgsConstructor
public class GeoAssetReferenceScanner {

    private static final String ASSET_PREFIX = "asset:";

    private final TemplateRepository templateRepository;
    private final YamlParser yamlParser;

    /**
     * Finds active catalog templates referencing the given asset id.
     *
     * <p>Preferred path parses {@code content_json} or {@code content_yaml} into {@link TemplateV2VO}
     * and walks geo sources. On parse failure, falls back to substring scan for {@code asset:{uuid}}
     * and JSON field literals.</p>
     *
     * @param assetId geo asset UUID to match
     * @return distinct usages (template id + name); empty when unreferenced
     */
    public List<GeoAssetTemplateUsage> findUsages(UUID assetId) {
        Map<Long, GeoAssetTemplateUsage> hits = new LinkedHashMap<>();
        for (TemplatePO row : templateRepository.findActiveForCatalog()) {
            if (scanStructured(row, assetId)) {
                recordHit(hits, row);
                continue;
            }
            if (scanRawFallback(rawContent(row), assetId)) {
                recordHit(hits, row);
            }
        }
        return List.copyOf(hits.values());
    }

    private boolean scanStructured(TemplatePO row, UUID assetId) {
        TemplateV2VO template = tryParseTemplate(row);
        if (template == null || template.getSources() == null) {
            return false;
        }
        for (SourceVO source : template.getSources().values()) {
            if (source instanceof GeoSyntheticSourceVO geoSynthetic && referencesGeoSynthetic(geoSynthetic, assetId)) {
                return true;
            }
            if (source instanceof GeoJsonSourceVO geoJson && referencesGeoJson(geoJson, assetId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean referencesGeoSynthetic(GeoSyntheticSourceVO source, UUID assetId) {
        return referencesValue(source.getBoundaryAssetId(), assetId)
                || referencesValue(source.getNetworkAssetId(), assetId)
                || referencesValue(source.getBoundaryPath(), assetId)
                || referencesValue(source.getNetworkPath(), assetId);
    }

    private static boolean referencesGeoJson(GeoJsonSourceVO source, UUID assetId) {
        return referencesValue(source.getAssetId(), assetId)
                || referencesValue(source.getPath(), assetId);
    }

    private static boolean referencesValue(String value, UUID assetId) {
        if (StrKit.isBlank(value)) {
            return false;
        }
        String trimmed = value.strip();
        String idText = assetId.toString();
        if (trimmed.equals(idText)) {
            return true;
        }
        if (trimmed.startsWith(ASSET_PREFIX)) {
            return trimmed.substring(ASSET_PREFIX.length()).strip().equals(idText);
        }
        return false;
    }

    private TemplateV2VO tryParseTemplate(TemplatePO row) {
        if (!StrKit.isBlank(row.getContentJson())) {
            try {
                return TemplateJsonCodec.read(row.getContentJson(), TemplateV2VO.class);
            } catch (RuntimeException ignored) {
                // Fall through to YAML or raw scan.
            }
        }
        if (!StrKit.isBlank(row.getContentYaml())) {
            try {
                return yamlParser.parse(row.getContentYaml(), TemplateV2VO.class);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String rawContent(TemplatePO row) {
        if (!StrKit.isBlank(row.getContentJson())) {
            return row.getContentJson();
        }
        if (!StrKit.isBlank(row.getContentYaml())) {
            return row.getContentYaml();
        }
        return "";
    }

    private static boolean scanRawFallback(String raw, UUID assetId) {
        if (StrKit.isBlank(raw)) {
            return false;
        }
        String idText = assetId.toString();
        String wireToken = ASSET_PREFIX + idText;
        if (raw.contains(wireToken)) {
            return true;
        }
        // JSON/YAML field literals for dedicated asset-id columns.
        return raw.contains("\"" + idText + "\"")
                || raw.contains("'" + idText + "'");
    }

    private static void recordHit(Map<Long, GeoAssetTemplateUsage> hits, TemplatePO row) {
        hits.putIfAbsent(row.getId(), new GeoAssetTemplateUsage(row.getId(), row.getName()));
    }
}
