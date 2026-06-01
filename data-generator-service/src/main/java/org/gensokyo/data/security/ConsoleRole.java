/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.security;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Console RBAC roles (Phase B).
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public enum ConsoleRole {
    VIEWER(ConsolePermission.TEMPLATE_READ, ConsolePermission.JOB_READ),
    EDITOR(ConsolePermission.TEMPLATE_READ, ConsolePermission.TEMPLATE_EDIT, ConsolePermission.JOB_READ),
    OPERATOR(
            ConsolePermission.TEMPLATE_READ,
            ConsolePermission.TEMPLATE_EDIT,
            ConsolePermission.TEMPLATE_RUN,
            ConsolePermission.JOB_READ),
    DATASOURCE_ADMIN(ConsolePermission.DATASOURCE_ADMIN, ConsolePermission.TEMPLATE_READ),
    ADMIN(EnumSet.allOf(ConsolePermission.class));

    private final Set<ConsolePermission> permissions;

    ConsoleRole(Set<ConsolePermission> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

    ConsoleRole(ConsolePermission... permissions) {
        this.permissions = Set.of(permissions);
    }

    /**
     * @param permission required permission
     * @return whether this role grants it
     */
    public boolean allows(ConsolePermission permission) {
        return permissions.contains(permission);
    }

    /**
     * @param headerValue value of {@code X-Console-Role}
     * @return parsed role or {@code null}
     */
    public static ConsoleRole fromHeader(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        try {
            return ConsoleRole.valueOf(headerValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
