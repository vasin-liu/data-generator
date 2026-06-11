/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.config.ConsoleSecurityProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces {@link ConsoleRole} permissions on {@code /api/**} when security is enabled.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class ConsoleAuthorizationFilter extends OncePerRequestFilter {

    private final ConsoleSecurityProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        ConsoleRole role = ConsoleRole.fromHeader(request.getHeader(properties.getRoleHeader()));
        if (role == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing or invalid console role header");
            return;
        }
        String actor = request.getHeader(properties.getActorHeader());
        if (actor != null && !actor.isBlank()) {
            ConsoleActorHolder.setActor(actor.trim());
        } else {
            ConsoleActorHolder.setActor(role.name());
        }
        try {
            ConsolePermission required = requiredPermission(request.getMethod(), request.getRequestURI());
            if (required != null && !role.allows(required)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Role " + role + " cannot " + required);
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            ConsoleActorHolder.clear();
        }
    }

    private static ConsolePermission requiredPermission(String method, String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("/api/secrets")) {
            return ConsolePermission.SECRET_ADMIN;
        }
        if (path.startsWith("/api/console/audit")) {
            return ConsolePermission.AUDIT_READ;
        }
        if (path.startsWith("/api/datasources")) {
            if (HttpMethod.POST.matches(method) || HttpMethod.DELETE.matches(method) || HttpMethod.PUT.matches(method)) {
                return ConsolePermission.DATASOURCE_ADMIN;
            }
            return ConsolePermission.TEMPLATE_READ;
        }
        if (path.contains("/publish")) {
            return ConsolePermission.TEMPLATE_PUBLISH;
        }
        if (path.startsWith("/api/templates")) {
            if (path.contains("/draft/run") || path.contains("/run")) {
                return ConsolePermission.TEMPLATE_RUN;
            }
            if (HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method) || HttpMethod.DELETE.matches(method)) {
                return ConsolePermission.TEMPLATE_EDIT;
            }
            return ConsolePermission.TEMPLATE_READ;
        }
        if (path.startsWith("/api/jobs")) {
            if (path.contains("/cancel")) {
                return ConsolePermission.JOB_CANCEL;
            }
            if (path.contains("/resume")) {
                return ConsolePermission.TEMPLATE_RUN;
            }
            return ConsolePermission.JOB_READ;
        }
        if (path.startsWith("/api/console/schedules")) {
            if (HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method) || HttpMethod.DELETE.matches(method)) {
                return ConsolePermission.TEMPLATE_RUN;
            }
            return ConsolePermission.JOB_READ;
        }
        return null;
    }
}
