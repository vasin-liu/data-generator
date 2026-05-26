/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.i18n;

import com.vaadin.flow.component.UI;

/**
 * Translation keys and helpers for operator console UI strings.
 *
 * @author Gensokyo
 * @since 2026-05-25
 */
public final class ConsoleI18n {

    /** Message keys — see {@code messages*.properties}. */
    public static final class Key {
        public static final String APP_TITLE = "app.title";
        public static final String NAV_DRAWER = "nav.drawer";
        public static final String NAV_HOME = "nav.home";
        public static final String NAV_TEMPLATES = "nav.templates";
        public static final String NAV_DATASOURCES = "nav.datasources";
        public static final String NAV_JOBS = "nav.jobs";
        public static final String NAV_MIGRATION = "nav.migration";
        public static final String V1_ENABLED = "v1.enabled";
        public static final String V1_DISABLED = "v1.disabled";
        public static final String PREFS_DARK = "prefs.dark";
        public static final String PREFS_LOCALE = "prefs.locale";
        public static final String PREFS_LANG_EN = "prefs.locale.en";
        public static final String PREFS_LANG_ZH = "prefs.locale.zh";

        private Key() {
        }
    }

    private ConsoleI18n() {
    }

    /**
     * Resolves a message for the current UI locale.
     *
     * @param key    property key
     * @param params optional {@link java.text.MessageFormat} parameters
     * @return translated text, or the key if no UI context
     */
    public static String tr(String key, Object... params) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return key;
        }
        return ui.getTranslation(key, params);
    }
}
