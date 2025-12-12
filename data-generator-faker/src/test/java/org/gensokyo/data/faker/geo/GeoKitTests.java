/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.faker.geo;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 地理信息工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/8 , Version 1.0.0
 */
class GeoKitTests {

    @Test
    void case1() throws IOException {
        Path geoJsonPath = Path.of("D:\\Work\\99_Code\\data-generator\\data-generator-faker\\src\\test\\resources\\南沙区边界.geojson");

        List<Point> randomPoints = GeoKit.generateRandomPointsFromGeoJson(
                geoJsonPath,
                0,         // FeatureCollection里第0个Feature
                100,       // 随机生成200个点
                50,        // 每个点至少间距50米
                2024L      // 随机种子（可复现）
        );

        StringBuilder points = new StringBuilder("[");
        for (Point p : randomPoints) {
            points.append("[").append(p.getX()).append(",").append(p.getY()).append("],");
        }
        points.append("]");
        System.out.println(points);
    }
}
