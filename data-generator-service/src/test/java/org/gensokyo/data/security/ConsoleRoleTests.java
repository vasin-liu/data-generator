/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ConsoleRole} permission matrix (Phase B-lite).
 *
 * @author Gensokyo
 * @since 2026-06-07
 */
class ConsoleRoleTests {

    @Test
    void viewerCanReadAuditButNotEditTemplates() {
        Assertions.assertTrue(ConsoleRole.VIEWER.allows(ConsolePermission.AUDIT_READ));
        Assertions.assertFalse(ConsoleRole.VIEWER.allows(ConsolePermission.TEMPLATE_EDIT));
    }

    @Test
    void editorCannotPublishOrRun() {
        Assertions.assertFalse(ConsoleRole.EDITOR.allows(ConsolePermission.TEMPLATE_PUBLISH));
        Assertions.assertFalse(ConsoleRole.EDITOR.allows(ConsolePermission.TEMPLATE_RUN));
    }

    @Test
    void operatorCanRunAndResumeButNotPublish() {
        Assertions.assertTrue(ConsoleRole.OPERATOR.allows(ConsolePermission.TEMPLATE_RUN));
        Assertions.assertFalse(ConsoleRole.OPERATOR.allows(ConsolePermission.TEMPLATE_PUBLISH));
    }

    @Test
    void adminGrantsAllPermissions() {
        for (ConsolePermission permission : ConsolePermission.values()) {
            Assertions.assertTrue(ConsoleRole.ADMIN.allows(permission));
        }
    }
}
