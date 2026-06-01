/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Optional RBAC for console {@code /api/**} endpoints (disabled by default).
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Component
@ConfigurationProperties(prefix = ConsoleSecurityProperties.PREFIX)
@Getter
@Setter
public class ConsoleSecurityProperties {

    /** Property prefix for console security. */
    public static final String PREFIX = DataGeneratorProperties.PREFIX + ".console-security";

    /**
     * When {@code false} (default), all console APIs are open (trusted intranet).
     */
    private boolean enabled = false;

    /**
     * Header carrying the operator role ({@link org.gensokyo.data.security.ConsoleRole} name).
     */
    private String roleHeader = "X-Console-Role";

    /**
     * Optional header for audit actor identity.
     */
    private String actorHeader = "X-Console-Actor";
}
