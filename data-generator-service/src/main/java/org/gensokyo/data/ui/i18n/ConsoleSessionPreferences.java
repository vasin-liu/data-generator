/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.i18n;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import java.util.Locale;

/**
 * Per-session operator console preferences (locale, dark theme).
 *
 * @author Gensokyo
 * @since 2026-05-25
 */
public final class ConsoleSessionPreferences {

    private static final String ATTR_DARK = "console.ui.dark";
    private static final String ATTR_LOCALE = "console.ui.locale";

    private ConsoleSessionPreferences() {
    }

    /**
     * Applies stored locale and dark-mode preference when a UI is created.
     *
     * @param ui active UI instance
     */
    public static void restore(UI ui) {
        VaadinSession session = ui.getSession();
        Locale locale = (Locale) session.getAttribute(ATTR_LOCALE);
        if (locale == null) {
            locale = Locale.SIMPLIFIED_CHINESE;
            session.setAttribute(ATTR_LOCALE, locale);
        }
        session.setLocale(locale);
        Boolean dark = (Boolean) session.getAttribute(ATTR_DARK);
        applyDarkTheme(ui, dark != null && dark);
    }

    /**
     * @return whether dark theme is enabled for this session
     */
    public static boolean isDarkMode() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return false;
        }
        Boolean dark = (Boolean) session.getAttribute(ATTR_DARK);
        return dark != null && dark;
    }

    /**
     * Persists and applies dark theme.
     *
     * @param dark true for Lumo dark variant
     */
    public static void setDarkMode(boolean dark) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(ATTR_DARK, dark);
        }
        UI ui = UI.getCurrent();
        if (ui != null) {
            applyDarkTheme(ui, dark);
        }
    }

    /**
     * Persists locale and performs a full browser reload so views are recreated with new translations.
     * Uses {@code window.location.reload()} instead of Vaadin's {@code Page.reload()} to avoid
     * corrupting the component tree when route targets were Spring singletons.
     *
     * @param locale English or Simplified Chinese
     */
    public static void setLocale(Locale locale) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(ATTR_LOCALE, locale);
            session.setLocale(locale);
        }
        UI ui = UI.getCurrent();
        if (ui != null) {
            // Full browser reload — safe with prototype-scoped @Route views
            ui.getPage().executeJs("window.location.reload()");
        }
    }

    /**
     * @return persisted locale, or Simplified Chinese as default
     */
    public static Locale currentLocale() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        Locale locale = (Locale) session.getAttribute(ATTR_LOCALE);
        return locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
    }

    private static void applyDarkTheme(UI ui, boolean dark) {
        if (dark) {
            ui.getElement().getThemeList().add("dark");
        } else {
            ui.getElement().getThemeList().remove("dark");
        }
        // Lumo global CSS in styles.css targets html[theme~="dark"]; sync with UI theme list.
        ui.getPage().executeJs(
                "if ($0) { document.documentElement.setAttribute('theme', 'dark'); }"
                        + " else { document.documentElement.removeAttribute('theme'); }",
                dark);
    }
}
