/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.i18n;

import com.vaadin.flow.i18n.I18NProvider;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Vaadin {@link I18NProvider} for the operator console (English + Simplified Chinese).
 *
 * @author Gensokyo
 * @since 2026-05-25
 */
@Component
public class OperatorConsoleI18nProvider implements I18NProvider {

    private static final String BUNDLE = "org.gensokyo.data.ui.i18n.messages";

    /** English bundle for fallback when a zh key is missing. */
    private static final ClassLoader BUNDLE_LOADER = OperatorConsoleI18nProvider.class.getClassLoader();

    private static final ResourceBundle EN_BUNDLE =
            ResourceBundle.getBundle(BUNDLE, Locale.ENGLISH, BUNDLE_LOADER);

    @Override
    public List<Locale> getProvidedLocales() {
        return List.of(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE);
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        Locale effective = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, effective, BUNDLE_LOADER);
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException ex) {
            try {
                pattern = EN_BUNDLE.getString(key);
            } catch (MissingResourceException ex2) {
                return key;
            }
        }
        if (params == null || params.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, params);
    }
}
