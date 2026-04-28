/*
 * Copyright 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 */
package org.gensokyo.data.faker.geo;

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
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * GeoJSON loader backed by Jackson 3 and JTS core.
 */
public final class GeoJsonLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private GeoJsonLoader() {
    }

    public static Geometry loadGeometry(Path geoJsonPath, int featureIndex) throws IOException {
        JsonNode root = MAPPER.readTree(Files.readString(geoJsonPath));
        JsonNode geometryNode = extractGeometryNode(root, featureIndex);
        return parseGeometry(geometryNode);
    }

    private static JsonNode extractGeometryNode(JsonNode root, int featureIndex) {
        JsonNode typeNode = root.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            throw new IllegalArgumentException("Invalid GeoJSON format: missing 'type' field");
        }

        return switch (typeNode.asText()) {
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

    private static Geometry parseGeometry(JsonNode geometryNode) {
        String type = requireNode(geometryNode.get("type"), "Geometry type is missing").asText();
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
