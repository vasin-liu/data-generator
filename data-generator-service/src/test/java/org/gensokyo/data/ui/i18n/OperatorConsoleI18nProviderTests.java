/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.i18n;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/**
 * Unit tests for operator console translations.
 *
 * @author Gensokyo
 * @since 2026-05-25
 */
class OperatorConsoleI18nProviderTests {

    private final OperatorConsoleI18nProvider provider = new OperatorConsoleI18nProvider();

    @Test
    void translatesChineseNavHome() {
        String text = provider.getTranslation(ConsoleI18n.Key.NAV_HOME, Locale.SIMPLIFIED_CHINESE);
        Assertions.assertEquals("首页", text);
    }

    @Test
    void translatesEnglishNavHome() {
        String text = provider.getTranslation(ConsoleI18n.Key.NAV_HOME, Locale.ENGLISH);
        Assertions.assertEquals("Home", text);
    }

    @Test
    void providesEnAndZhLocales() {
        Assertions.assertTrue(provider.getProvidedLocales().contains(Locale.SIMPLIFIED_CHINESE));
        Assertions.assertTrue(provider.getProvidedLocales().contains(Locale.ENGLISH));
    }
}
