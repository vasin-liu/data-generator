/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.util.List;

/**
 * Runtime flags shown in the console shell (navbar banner and home status).
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
public record ConsoleRuntimeDto(
        boolean v1ExecutionEnabled,
        boolean scheduleEnabled,
        boolean distributedEnabled,
        boolean consoleSecurityEnabled,
        String consoleRoleHeader,
        List<String> consoleRoles) {
}
