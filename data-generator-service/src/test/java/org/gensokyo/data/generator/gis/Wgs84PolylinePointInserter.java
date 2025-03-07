/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.gis;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/12/4 , Version 1.0.0
 */
public class Wgs84PolylinePointInserter {

    public static void main(String[] args) throws Exception {
        // 示例 GeoJSON Polyline
        String geoJson = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "properties": {},
                      "geometry": {
                        "coordinates": [
                          [
                            113.21794911764619,
                            23.161615915382953
                          ],
                          [
                            113.21794545090961,
                            23.159541583179177
                          ],
                          [
                            113.21840677306608,
                            23.158993410126627
                          ],
                          [
                            113.21833746402376,
                            23.15807840234079
                          ],
                          [
                            113.21807265628041,
                            23.156490182353963
                          ],
                          [
                            113.2181415607925,
                            23.15215351212838
                          ],
                          [
                            113.21840554088368,
                            23.151115508158767
                          ],
                          [
                            113.22292542274624,
                            23.145435741808228
                          ],
                          [
                            113.2293717508017,
                            23.14116225141528
                          ],
                          [
                            113.23568180664256,
                            23.14415464556454
                          ],
                          [
                            113.24165865599758,
                            23.147878373961404
                          ],
                          [
                            113.24710101593428,
                            23.15038037195457
                          ],
                          [
                            113.24995578756563,
                            23.150686758897393
                          ],
                          [
                            113.25633004833372,
                            23.14855002658399
                          ],
                          [
                            113.25699029658671,
                            23.150807683602252
                          ],
                          [
                            113.25752287029155,
                            23.155324821974915
                          ],
                          [
                            113.25825033767552,
                            23.157214402360324
                          ],
                          [
                            113.25799080041656,
                            23.160944081390326
                          ],
                          [
                            113.25851969926134,
                            23.161856794146473
                          ],
                          [
                            113.2617686634893,
                            23.161242469580074
                          ],
                          [
                            113.26469060887803,
                            23.16112176469983
                          ],
                          [
                            113.26800087221272,
                            23.168070002772453
                          ],
                          [
                            113.26853587687737,
                            23.16935533647711
                          ],
                          [
                            113.26881144785222,
                            23.171807264884833
                          ]
                        ],
                        "type": "LineString"
                      }
                    }
                  ]
                }
                """;

        // 目标插入距离 (单位: 米)
        double interval = 5.0;

        // 插入点位
        String resultGeoJson = insertPointsOnWgs84Polyline(geoJson, interval);

        System.out.println("Resulting GeoJSON: " + resultGeoJson);
    }

    /**
     * 在WGS84 GeoJSON格式的Polyline上按一定距离插入点位
     *
     * @param geoJson 输入的GeoJSON字符串 (类型为LineString)
     * @param interval 插入点的距离 (单位: 米)
     * @return 插入点后的GeoJSON字符串
     * @throws Exception 异常
     */
    public static String insertPointsOnWgs84Polyline(String geoJson, double interval) throws Exception {
        // 读取 GeoJSON 并解析为 JTS LineString
        GeoJsonReader reader = new GeoJsonReader();
        Geometry geometry = reader.read(geoJson);
        if (geometry instanceof GeometryCollection) {
            geometry = geometry.getGeometryN(0);
        }

        if (!(geometry instanceof LineString)) {
            throw new IllegalArgumentException("Input GeoJSON must be a LineString.");
        }

        LineString line = (LineString) geometry;

        // 准备 GeoTools 的 GeodeticCalculator
        GeodeticCalculator calculator = new GeodeticCalculator();

        // 计算插入点位
        List<Coordinate> newCoordinates = new ArrayList<>();
        newCoordinates.add(line.getCoordinateN(0)); // 起点

        for (int i = 1; i < line.getNumPoints(); i++) {
            Coordinate start = line.getCoordinateN(i - 1);
            Coordinate end = line.getCoordinateN(i);

            // 设置起点和终点
            calculator.setStartingGeographicPoint(start.x, start.y);
            calculator.setDestinationGeographicPoint(end.x, end.y);

            // 计算段距离
            double segmentDistance = calculator.getOrthodromicDistance(); // 段长，单位米
            double azimuth = calculator.getAzimuth(); // 获取方位角

            // 插入点
            double accumulatedDistance = 0.0;
            while (accumulatedDistance + interval < segmentDistance) {
                accumulatedDistance += interval;

                // 根据方位角和距离计算插入点
                calculator.setStartingGeographicPoint(start.x, start.y);
                calculator.setDirection(azimuth, accumulatedDistance);

                Coordinate newPoint = new Coordinate(
                        calculator.getDestinationGeographicPoint().getX(),
                        calculator.getDestinationGeographicPoint().getY()
                );
                newCoordinates.add(newPoint);
            }

            newCoordinates.add(end); // 段尾点
        }

        // 创建新的 LineString
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        LineString newLine = geometryFactory.createLineString(newCoordinates.toArray(new Coordinate[0]));

        // 转换为 GeoJSON
        GeoJsonWriter writer = new GeoJsonWriter();
        return writer.write(newLine);
    }
}
