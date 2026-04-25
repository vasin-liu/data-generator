/*
 * Copyright 婕?2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address閿涙瓍CI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou閿涘瓔hina閿涘湼ip code閿?10653閿?
 */
package org.gensokyo.data.faker.geo;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Geometry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Geojson閺傚洣娆㈤崝鐘烘祰閸?
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/8 , Version 1.0.0
 */
public class GeoJsonLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JtsModule());
    }

    /**
     * 閸旂姾娴嘒eoJSON閺傚洣娆㈤獮鎯扮箲閸ョ偞瀵氱€规eature閻ㄥ嚕eometry
     *
     * @param geoJsonPath GeoJSON閺傚洣娆㈢捄顖氱窞
     * @param featureIndex Feature閸︹€礶atureCollection娑擃厾娈戠槐銏犵穿閿涘牆顩ч弸婊€绗夐弰鐤恊atureCollection閿涘奔绱?閿?
     * @return Geometry
     */
    public static Geometry loadGeometry(Path geoJsonPath, int featureIndex) throws IOException {
        String geoJsonContent = Files.readString(geoJsonPath);
        JsonNode root = MAPPER.readTree(geoJsonContent);

        JsonNode geometryNode;

        if (root.has("type")) {
            String type = root.get("type").asText();
            if ("Feature".equals(type)) {
                geometryNode = root.get("geometry");
            } else if ("FeatureCollection".equals(type)) {
                geometryNode = root.get("features").get(featureIndex).get("geometry");
            } else {
                geometryNode = root;
            }
        } else {
            throw new IllegalArgumentException("Invalid GeoJSON format: missing 'type' field");
        }

        return MAPPER.treeToValue(geometryNode, Geometry.class);
    }
}

