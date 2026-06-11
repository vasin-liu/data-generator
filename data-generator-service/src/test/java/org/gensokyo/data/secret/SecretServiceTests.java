/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.secret;

import org.gensokyo.data.DataGeneratorApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class SecretServiceTests {

    @Autowired
    private SecretService secretService;

    @Test
    @Transactional
    void resolvesPersistedSecret() {
        secretService.upsert("test/demo", "pw-value", "test");
        Assertions.assertEquals("pw-value", secretService.resolveRequired("test/demo"));
        Assertions.assertEquals("pw-value", secretService.resolveInlinePassword(null, "test/demo"));
    }
}
