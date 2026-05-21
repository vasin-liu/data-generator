/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.io;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * GeoJSON loader backed by Jackson 3 and JTS core.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class GeoJsonLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private GeoJsonLoader() {
    }

    /**
     * Loads a geometry from a GeoJSON resource path.
     *
     * @param location     classpath or file path
     * @param featureIndex feature index when root is a FeatureCollection
     * @return JTS geometry
     * @throws IOException when the file cannot be read
     */
    public static Geometry loadGeometry(String location, int featureIndex) throws IOException {
        JsonNode root = MAPPER.readTree(GeoResourceResolver.readUtf8(location));
        return parseGeometry(extractGeometryNode(root, featureIndex));
    }

    /**
     * Loads one feature (geometry + properties) from a GeoJSON resource.
     *
     * @param location      classpath or file path
     * @param featureIndex  index when not random
     * @param randomFeature when true, pick a random feature using {@code seed}
     * @param seed          random seed for feature selection
     * @return feature payload
     * @throws IOException when the file cannot be read
     */
    public static GeoFeature loadFeature(String location, int featureIndex, boolean randomFeature, long seed)
            throws IOException {
        JsonNode root = MAPPER.readTree(GeoResourceResolver.readUtf8(location));
        JsonNode featureNode = extractFeatureNode(root, featureIndex, randomFeature, seed);
        return parseGeoFeature(featureNode);
    }

    /**
     * Loads every {@code Feature} from a GeoJSON root {@code Feature} or {@code FeatureCollection}.
     *
     * @param location classpath or filesystem location understood by {@link GeoResourceResolver}
     * @return features in GeoJSON array order
     * @throws IOException when the resource cannot be read
     */
    public static List<GeoFeature> loadFeatureCollection(String location) throws IOException {
        JsonNode root = MAPPER.readTree(GeoResourceResolver.readUtf8(location));
        JsonNode typeNode = root.get("type");
        if (typeNode == null || !typeNode.isString()) {
            throw new IllegalArgumentException("Invalid GeoJSON format: missing 'type' field");
        }
        return switch (typeNode.asString()) {
            case "Feature" -> List.of(parseGeoFeature(root));
            case "FeatureCollection" -> {
                JsonNode features = requireNode(root.get("features"), "FeatureCollection features are missing");
                List<GeoFeature> list = new ArrayList<>(features.size());
                // Preserve FeatureCollection index order when iterating Jackson array nodes.
                for (int i = 0; i < features.size(); i++) {
                    list.add(parseGeoFeature(features.get(i)));
                }
                yield list;
            }
            default -> throw new IllegalArgumentException(
                    "Expected Feature or FeatureCollection root for loadFeatureCollection, got: " + typeNode.asString());
        };
    }

    private static GeoFeature parseGeoFeature(JsonNode featureNode) {
        JsonNode typeNode = featureNode.get("type");
        if (typeNode == null || !typeNode.isString() || !"Feature".equals(typeNode.asString())) {
            throw new IllegalArgumentException("GeoJSON Feature expected, got type: "
                    + (typeNode == null ? "null" : typeNode.asString()));
        }
        Geometry geometry = parseGeometry(requireNode(featureNode.get("geometry"), "Feature geometry is missing"));
        Map<String, Object> properties = parseProperties(featureNode.get("properties"));
        return new GeoFeature(geometry, properties);
    }

    private static JsonNode extractFeatureNode(JsonNode root, int featureIndex, boolean randomFeature, long seed) {
        JsonNode typeNode = root.get("type");
        if (typeNode == null || !typeNode.isString()) {
            throw new IllegalArgumentException("Invalid GeoJSON format: missing 'type' field");
        }
        return switch (typeNode.asString()) {
            case "Feature" -> root;
            case "FeatureCollection" -> {
                JsonNode features = requireNode(root.get("features"), "FeatureCollection features are missing");
                if (features.isEmpty()) {
                    throw new IllegalArgumentException("FeatureCollection is empty");
                }
                int index = randomFeature
                        ? new Random(seed).nextInt(features.size())
                        : featureIndex;
                JsonNode feature = features.get(index);
                if (feature == null) {
                    throw new IllegalArgumentException("Feature index out of range: " + index);
                }
                yield feature;
            }
            default -> throw new IllegalArgumentException("Expected Feature or FeatureCollection, got: " + typeNode.asString());
        };
    }

    private static JsonNode extractGeometryNode(JsonNode root, int featureIndex) {
        JsonNode typeNode = root.get("type");
        if (typeNode == null || !typeNode.isString()) {
            throw new IllegalArgumentException("Invalid GeoJSON format: missing 'type' field");
        }

        return switch (typeNode.asString()) {
            case "Feature" -> requireNode(root.get("geometry"), "Feature geometry is missing");
            case "FeatureCollection" -> {
                JsonNode features = requireNode(root.get("features"), "FeatureCollection features are missing");
                JsonNode feature = features.get(featureIndex);
                if (feature == null) {
                    throw new IllegalArgumentException("Feature index out of range: " + featureIndex);
                }
                yield requireNode(feature.get("geometry"), "Feature geometry is missing");
            }
            default -> root;
        };
    }

    private static Map<String, Object> parseProperties(JsonNode propertiesNode) {
        if (propertiesNode == null || propertiesNode.isNull() || !propertiesNode.isObject()) {
            return Map.of();
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        // Use forEachEntry (Jackson 3) instead of deprecated properties() iteration on JsonNode.
        propertiesNode.forEachEntry((fieldName, value) -> {
            if (value == null || value.isNull()) {
                properties.put(fieldName, null);
            } else if (value.isString()) {
                properties.put(fieldName, value.asString());
            } else if (value.isBoolean()) {
                properties.put(fieldName, value.asBoolean());
            } else if (value.isNumber()) {
                properties.put(fieldName, value.asDouble());
            } else {
                properties.put(fieldName, value.toString());
            }
        });
        return properties;
    }

    private static Geometry parseGeometry(JsonNode geometryNode) {
        String type = requireNode(geometryNode.get("type"), "Geometry type is missing").asString();
        JsonNode coordinates = geometryNode.get("coordinates");

        return switch (type) {
            case "Point" -> GEOMETRY_FACTORY.createPoint(parseCoordinate(requireNode(coordinates, "Point coordinates are missing")));
            case "MultiPoint" -> GEOMETRY_FACTORY.createMultiPointFromCoords(parseCoordinateArray(requireNode(coordinates, "MultiPoint coordinates are missing")));
            case "LineString" -> GEOMETRY_FACTORY.createLineString(parseCoordinateArray(requireNode(coordinates, "LineString coordinates are missing")));
            case "MultiLineString" -> parseMultiLineString(requireNode(coordinates, "MultiLineString coordinates are missing"));
            case "Polygon" -> parsePolygon(requireNode(coordinates, "Polygon coordinates are missing"));
            case "MultiPolygon" -> parseMultiPolygon(requireNode(coordinates, "MultiPolygon coordinates are missing"));
            case "GeometryCollection" -> parseGeometryCollection(geometryNode);
            default -> throw new IllegalArgumentException("Unsupported GeoJSON geometry type: " + type);
        };
    }

    private static MultiLineString parseMultiLineString(JsonNode coordinatesNode) {
        LineString[] lineStrings = new LineString[coordinatesNode.size()];
        for (int i = 0; i < coordinatesNode.size(); i++) {
            lineStrings[i] = GEOMETRY_FACTORY.createLineString(parseCoordinateArray(coordinatesNode.get(i)));
        }
        return GEOMETRY_FACTORY.createMultiLineString(lineStrings);
    }

    private static Polygon parsePolygon(JsonNode coordinatesNode) {
        if (coordinatesNode.isEmpty()) {
            throw new IllegalArgumentException("Polygon coordinates are empty");
        }

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(parseCoordinateArray(coordinatesNode.get(0)));
        LinearRing[] holes = new LinearRing[Math.max(0, coordinatesNode.size() - 1)];
        for (int i = 1; i < coordinatesNode.size(); i++) {
            holes[i - 1] = GEOMETRY_FACTORY.createLinearRing(parseCoordinateArray(coordinatesNode.get(i)));
        }
        return GEOMETRY_FACTORY.createPolygon(shell, holes);
    }

    private static MultiPolygon parseMultiPolygon(JsonNode coordinatesNode) {
        Polygon[] polygons = new Polygon[coordinatesNode.size()];
        for (int i = 0; i < coordinatesNode.size(); i++) {
            polygons[i] = parsePolygon(coordinatesNode.get(i));
        }
        return GEOMETRY_FACTORY.createMultiPolygon(polygons);
    }

    private static Geometry parseGeometryCollection(JsonNode geometryNode) {
        JsonNode geometriesNode = requireNode(geometryNode.get("geometries"), "GeometryCollection geometries are missing");
        Geometry[] geometries = new Geometry[geometriesNode.size()];
        for (int i = 0; i < geometriesNode.size(); i++) {
            geometries[i] = parseGeometry(geometriesNode.get(i));
        }
        return GEOMETRY_FACTORY.createGeometryCollection(geometries);
    }

    private static Coordinate[] parseCoordinateArray(JsonNode coordinatesNode) {
        Coordinate[] coordinates = new Coordinate[coordinatesNode.size()];
        for (int i = 0; i < coordinatesNode.size(); i++) {
            coordinates[i] = parseCoordinate(coordinatesNode.get(i));
        }
        return coordinates;
    }

    private static Coordinate parseCoordinate(JsonNode coordinateNode) {
        if (coordinateNode == null || coordinateNode.size() < 2) {
            throw new IllegalArgumentException("Invalid GeoJSON coordinate");
        }

        double x = coordinateNode.get(0).asDouble();
        double y = coordinateNode.get(1).asDouble();
        if (coordinateNode.size() > 2) {
            return new Coordinate(x, y, coordinateNode.get(2).asDouble());
        }
        return new Coordinate(x, y);
    }

    private static JsonNode requireNode(JsonNode node, String message) {
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException(message);
        }
        return node;
    }
}
