/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.security;

/**
 * Thread-local console actor for audit and RBAC context.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class ConsoleActorHolder {

    private static final ThreadLocal<String> ACTOR = new ThreadLocal<>();

    private ConsoleActorHolder() {
    }

    /**
     * @param actor operator id or role label
     */
    public static void setActor(String actor) {
        ACTOR.set(actor);
    }

    /** Clears actor binding. */
    public static void clear() {
        ACTOR.remove();
    }

    /**
     * @return current actor or {@code system}
     */
    public static String currentActor() {
        String actor = ACTOR.get();
        return actor == null || actor.isBlank() ? "system" : actor;
    }
}
