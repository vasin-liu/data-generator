/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.gis;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/12/4 , Version 1.0.0
 */
public class SplineInterpolationWithDistanceControl {

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
                    },
                    {
                      "type": "Feature",
                      "properties": {},
                      "geometry": {
                        "coordinates": [
                          [
                            113.1943042334554,
                            23.159614873328437
                          ],
                          [
                            113.19550716091885,
                            23.161371429217027
                          ],
                          [
                            113.1967631587122,
                            23.16275388755831
                          ],
                          [
                            113.1986560004558,
                            23.1643640269304
                          ],
                          [
                            113.20084957406596,
                            23.165583816596126
                          ],
                          [
                            113.20359154107939,
                            23.166559640328785
                          ],
                          [
                            113.20576742457956,
                            23.166982495070684
                          ],
                          [
                            113.20797868829965,
                            23.167031285917133
                          ],
                          [
                            113.21105676739671,
                            23.166722276926066
                          ],
                          [
                            113.21466554978713,
                            23.166283157660033
                          ],
                          [
                            113.2175751149872,
                            23.166233505729878
                          ],
                          [
                            113.21801736773062,
                            23.166542515848903
                          ],
                          [
                            113.21826502926734,
                            23.16854293252301
                          ],
                          [
                            113.21863652157236,
                            23.170462003355695
                          ],
                          [
                            113.2190433940973,
                            23.171372739209488
                          ],
                          [
                            113.21913184464569,
                            23.171860630868252
                          ],
                          [
                            113.21886649299904,
                            23.171893156916013
                          ],
                          [
                            113.21884880288911,
                            23.172137102019548
                          ],
                          [
                            113.21930874574394,
                            23.173503186382362
                          ],
                          [
                            113.2193795061824,
                            23.17373086575421
                          ],
                          [
                            113.22052630960161,
                            23.179623433552962
                          ],
                          [
                            113.22088011179807,
                            23.181704963910533
                          ]
                        ],
                        "type": "LineString"
                      }
                    }
                  ]
                }
                """;

        // 控制生成点的距离
        double targetDistance = 4.0;

        // 插值和平滑折线
        String smoothedGeoJson = smoothPolylineWithDistanceControl(geoJson, targetDistance);

        System.out.println("Smoothed GeoJSON: " + smoothedGeoJson);
    }

    /**
     * 使用样条插值对折线进行平滑处理，带距离控制
     *
     * @param geoJson        输入的 GeoJSON 字符串（类型为 LineString）
     * @param targetDistance 控制生成点之间的距离
     * @return 平滑后的 GeoJSON 字符串
     * @throws Exception 异常
     */
    public static String smoothPolylineWithDistanceControl(String geoJson, double targetDistance) throws Exception {
        // 读取 GeoJSON 并解析为 JTS LineString
        GeoJsonReader reader = new GeoJsonReader();
        Geometry geometry = reader.read(geoJson);
        if (geometry instanceof GeometryCollection) {
            geometry = geometry.getGeometryN(0);
        }

        if (!(geometry instanceof LineString line)) {
            throw new IllegalArgumentException("Input GeoJSON must be a LineString.");
        }

        // 获取原始点
        Coordinate[] originalCoordinates = line.getCoordinates();

        // 使用样条插值生成平滑点
        List<Coordinate> smoothedCoordinates = interpolateWithDistanceControl(originalCoordinates, targetDistance);

        // 使用插值点生成新的 LineString
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        LineString smoothedLine = geometryFactory.createLineString(smoothedCoordinates.toArray(new Coordinate[0]));

        // 转换为 GeoJSON
        GeoJsonWriter writer = new GeoJsonWriter();
        return writer.write(smoothedLine);
    }

    /**
     * 使用 Apache Commons Math 的样条插值生成平滑点，带距离控制
     *
     * @param originalCoordinates 原始点数组
     * @param targetDistance      控制生成点之间的距离
     * @return 平滑后的点列表
     */
    private static List<Coordinate> interpolateWithDistanceControl(Coordinate[] originalCoordinates, double targetDistance) {
        int n = originalCoordinates.length;

        // 提取 x 和 y 坐标
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = originalCoordinates[i].x;
            y[i] = originalCoordinates[i].y;
        }

        // 确保 x 坐标严格递增
        final double[] finalX = x;
        final double[] finalY = y;
        int[] indices = IntStream.range(0, finalX.length)
                .boxed()
                .sorted((i, j) -> Double.compare(finalX[i], finalX[j]))
                .mapToInt(i -> i)
                .toArray();

        x = Arrays.stream(indices).mapToDouble(i -> finalX[i]).distinct().toArray();
        y = Arrays.stream(indices).mapToDouble(i -> finalY[i]).toArray();

        if (x.length < 2) {
            throw new IllegalArgumentException("Not enough unique points for interpolation.");
        }

        // 使用 Apache Commons Math 进行样条插值
        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction splineX = interpolator.interpolate(x, x); // x 对应自身
        PolynomialSplineFunction splineY = interpolator.interpolate(x, y); // y 对应 x

        // 根据距离控制生成插值点
        List<Coordinate> interpolatedCoordinates = new ArrayList<>();
        interpolatedCoordinates.add(new Coordinate(x[0], y[0])); // 添加第一个点

        double previousX = x[0];
        double previousY = y[0];
        double accumulatedDistance = 0.0;
        double t = x[0];
        double maxT = x[x.length - 1];

        // 初始化步长
        double step = 0.01;
        double currentX, currentY, distance;

        while (t < maxT) {
            // 插值计算当前点位
            currentX = splineX.value(t);
            currentY = splineY.value(t);

            // 计算与上一个点的距离
            distance = calculateDistance(previousX, previousY, currentX, currentY);

            accumulatedDistance += distance;

            // 如果累计的距离超过目标距离，则添加该点
            if (accumulatedDistance >= targetDistance) {
                interpolatedCoordinates.add(new Coordinate(currentX, currentY));
                accumulatedDistance = 0.0; // 重置累计距离
            }

            // 步进到下一个点
            t += step;

            // 如果接近最大值，则减少步长
            if (t + step > maxT) {
                step = maxT - t; // 动态调整步长，避免超出范围
            }

            previousX = currentX;
            previousY = currentY;
        }

        // 确保最后一个点被添加
        if (!interpolatedCoordinates.contains(new Coordinate(x[x.length - 1], y[y.length - 1]))) {
            interpolatedCoordinates.add(new Coordinate(x[x.length - 1], y[y.length - 1]));
        }

        return interpolatedCoordinates;
    }

    /**
     * 计算两点之间的距离
     *
     * @param x1 点1的x坐标
     * @param y1 点1的y坐标
     * @param x2 点2的x坐标
     * @param y2 点2的y坐标
     * @return 两点之间的距离
     */
    private static double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
}

