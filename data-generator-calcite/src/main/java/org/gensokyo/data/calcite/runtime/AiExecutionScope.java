/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

/**
 * Thread-local binding of the active template while a Template V2 run materializes AI sources.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public final class AiExecutionScope {

    private static final ThreadLocal<Binding> CURRENT = new ThreadLocal<>();

    private AiExecutionScope() {
    }

    /**
     * Binds the template identity for the current thread.
     *
     * @param templateId   template snowflake id, may be {@code null}
     * @param templateName human-readable template name
     */
    public static void bind(Long templateId, String templateName) {
        bind(templateId, templateName, null);
    }

    /**
     * Binds template and optional tenant identity for the current thread.
     *
     * @param templateId   template snowflake id, may be {@code null}
     * @param templateName human-readable template name
     * @param tenantId     tenant id from template metadata, may be {@code null}
     */
    public static void bind(Long templateId, String templateName, String tenantId) {
        CURRENT.set(new Binding(templateId, templateName, blankToNull(tenantId)));
    }

    /** Clears the binding for the current thread. */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * @return bound template id or {@code null}
     */
    public static Long templateId() {
        Binding binding = CURRENT.get();
        return binding == null ? null : binding.templateId();
    }

    /**
     * @return bound template name or {@code null}
     */
    public static String templateName() {
        Binding binding = CURRENT.get();
        return binding == null ? null : binding.templateName();
    }

    /**
     * @return bound tenant id or {@code null}
     */
    public static String tenantId() {
        Binding binding = CURRENT.get();
        return binding == null ? null : binding.tenantId();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record Binding(Long templateId, String templateName, String tenantId) {
    }
}
