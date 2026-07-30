/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Validation rules for {@link GeoGenerationRequest} BBOX and CIRCLE modes.
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
class GeoGenerationRequestValidationTests {

    @Test
    void bboxHappyPathAcceptsValidConfig() {
        GeoGenerationRequest request = validBboxRequest();
        Assertions.assertDoesNotThrow(request::validate);
    }

    @Test
    void circleHappyPathAcceptsValidConfig() {
        GeoGenerationRequest request = validCircleRequest();
        Assertions.assertDoesNotThrow(request::validate);
    }

    @Test
    void bboxRejectsNonPositiveCount() {
        GeoGenerationRequest request = validBboxRequest();
        request.setCount(0);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("count"));
    }

    @Test
    void bboxRejectsNegativeMinDistance() {
        GeoGenerationRequest request = validBboxRequest();
        request.setMinDistanceMeters(-1d);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("minDistanceMeters"));
    }

    @Test
    void bboxRejectsDegenerateLongitudeSpan() {
        GeoGenerationRequest request = validBboxRequest();
        request.setBboxMinLon(113.5d);
        request.setBboxMaxLon(113.5d);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("bboxMinLon"));
        Assertions.assertTrue(error.getMessage().contains("bboxMaxLon"));
    }

    @Test
    void bboxRejectsInvertedLongitudeSpan() {
        GeoGenerationRequest request = validBboxRequest();
        request.setBboxMinLon(114d);
        request.setBboxMaxLon(113d);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("bboxMinLon"));
    }

    @Test
    void bboxRejectsDegenerateLatitudeSpan() {
        GeoGenerationRequest request = validBboxRequest();
        request.setBboxMinLat(22.7d);
        request.setBboxMaxLat(22.6d);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("bboxMinLat"));
        Assertions.assertTrue(error.getMessage().contains("bboxMaxLat"));
    }

    @Test
    void circleRejectsNonPositiveCount() {
        GeoGenerationRequest request = validCircleRequest();
        request.setCount(0);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("count"));
    }

    @Test
    void circleRejectsNonPositiveRadius() {
        GeoGenerationRequest request = validCircleRequest();
        request.setRadiusMeters(0d);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("radiusMeters"));
    }

    @Test
    void circleRejectsNegativeRadius() {
        GeoGenerationRequest request = validCircleRequest();
        request.setRadiusMeters(-100d);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("radiusMeters"));
    }

    @Test
    void circleRejectsLongitudeOutOfRange() {
        GeoGenerationRequest request = validCircleRequest();
        request.setCenterLon(181d);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("centerLon"));
    }

    @Test
    void circleRejectsLatitudeOutOfRange() {
        GeoGenerationRequest request = validCircleRequest();
        request.setCenterLat(91d);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("centerLat"));
    }

    @Test
    void circleRejectsNonFiniteCenterLon() {
        GeoGenerationRequest request = validCircleRequest();
        request.setCenterLon(Double.NaN);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("centerLon"));
    }

    @Test
    void circleRejectsNonFiniteCenterLat() {
        GeoGenerationRequest request = validCircleRequest();
        request.setCenterLat(Double.POSITIVE_INFINITY);
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class, request::validate);
        Assertions.assertTrue(error.getMessage().contains("centerLat"));
    }

    @Test
    void seedZeroIsValidForBbox() {
        GeoGenerationRequest request = validBboxRequest();
        request.setSeed(0L);
        Assertions.assertDoesNotThrow(request::validate);
        Assertions.assertEquals(0L, request.getSeed());
    }

    private static GeoGenerationRequest validBboxRequest() {
        GeoGenerationRequest request = new GeoGenerationRequest();
        request.setMode(GeoGenerationMode.BBOX);
        request.setBboxMinLon(113.2d);
        request.setBboxMinLat(22.5d);
        request.setBboxMaxLon(113.6d);
        request.setBboxMaxLat(22.9d);
        request.setCount(10);
        request.setSeed(42L);
        request.setMinDistanceMeters(0d);
        return request;
    }

    private static GeoGenerationRequest validCircleRequest() {
        GeoGenerationRequest request = new GeoGenerationRequest();
        request.setMode(GeoGenerationMode.CIRCLE);
        request.setCenterLon(113.4d);
        request.setCenterLat(22.7d);
        request.setRadiusMeters(500d);
        request.setCount(10);
        request.setSeed(42L);
        return request;
    }
}
