/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * Unit tests for the {@code /api/console/udfs} RBAC classification in {@link ConsoleAuthorizationFilter}.
 *
 * <p>Asserts UDF mutations require operator-grade {@link ConsolePermission#TEMPLATE_PUBLISH} while reads
 * require viewer-grade {@link ConsolePermission#TEMPLATE_READ} (D-15), and that {@code /udfs/.../publish}
 * is classified by the UDF branch rather than the generic template publish branch.
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
class ConsoleUdfAuthorizationFilterTest {

    @Test
    void postUpload_requiresTemplatePublish() throws Exception {
        Assertions.assertEquals(
                ConsolePermission.TEMPLATE_PUBLISH,
                requiredPermission("POST", "/api/console/udfs"));
    }

    @Test
    void postPublish_requiresTemplatePublish() throws Exception {
        Assertions.assertEquals(
                ConsolePermission.TEMPLATE_PUBLISH,
                requiredPermission("POST", "/api/console/udfs/x/1.0.0/publish"));
    }

    @Test
    void getList_requiresTemplateRead() throws Exception {
        Assertions.assertEquals(
                ConsolePermission.TEMPLATE_READ,
                requiredPermission("GET", "/api/console/udfs"));
    }

    private static ConsolePermission requiredPermission(String method, String path) throws Exception {
        Method m = ConsoleAuthorizationFilter.class
                .getDeclaredMethod("requiredPermission", String.class, String.class);
        m.setAccessible(true);
        return (ConsolePermission) m.invoke(null, method, path);
    }
}
