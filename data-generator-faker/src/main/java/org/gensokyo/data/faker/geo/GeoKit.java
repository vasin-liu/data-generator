/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.faker.geo;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 地理信息处理工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/8 , Version 1.0.0
 */
public class GeoKit {

    /**
     * 解析GeoJSON并在指定Feature上生成随机点
     *
     * @param geoJsonPath       GeoJSON文件路径
     * @param featureIndex      Feature索引（FeatureCollection用）
     * @param count             点的数量
     * @param minDistanceMeters 点之间最小距离（米）
     * @param seed              随机种子
     * @return 随机生成的点集合
     */
    public static List<Point> generateRandomPointsFromGeoJson(
            Path geoJsonPath,
            int featureIndex,
            int count,
            double minDistanceMeters,
            long seed
    ) throws IOException {
        Geometry geometry = GeoJsonLoader.loadGeometry(geoJsonPath, featureIndex);
        return RandomPointGenerator.generate(geometry, count, minDistanceMeters, seed, 10000);
    }
}
