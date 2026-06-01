/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

/**
 * Thread-local binding of run instance id and {@link WorkflowRunControl} for workflow execution.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class WorkflowRunContext {

    private static final ThreadLocal<Binding> CURRENT = new ThreadLocal<>();

    private WorkflowRunContext() {
    }

    /**
     * @param instanceId run snowflake id
     * @param control    cancel/pause hooks (may be {@link WorkflowRunControl#NO_OP})
     */
    public static void bind(Long instanceId, WorkflowRunControl control) {
        CURRENT.set(new Binding(instanceId, control == null ? WorkflowRunControl.NO_OP : control));
    }

    /** Clears the binding for the current thread. */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * @return bound control or {@link WorkflowRunControl#NO_OP}
     */
    public static WorkflowRunControl control() {
        Binding binding = CURRENT.get();
        return binding == null ? WorkflowRunControl.NO_OP : binding.control();
    }

    /**
     * @return bound instance id or {@code null}
     */
    public static Long instanceId() {
        Binding binding = CURRENT.get();
        return binding == null ? null : binding.instanceId();
    }

    private record Binding(Long instanceId, WorkflowRunControl control) {
    }
}
