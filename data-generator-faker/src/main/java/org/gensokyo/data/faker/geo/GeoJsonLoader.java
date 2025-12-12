/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
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
 * Geojson文件加载器
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
     * 加载GeoJSON文件并返回指定Feature的Geometry
     *
     * @param geoJsonPath GeoJSON文件路径
     * @param featureIndex Feature在FeatureCollection中的索引（如果不是FeatureCollection，传0）
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
