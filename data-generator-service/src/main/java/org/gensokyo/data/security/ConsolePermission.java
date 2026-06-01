/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.security;

/**
 * Fine-grained console API permissions.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public enum ConsolePermission {
    TEMPLATE_READ,
    TEMPLATE_EDIT,
    TEMPLATE_PUBLISH,
    TEMPLATE_RUN,
    JOB_READ,
    JOB_CANCEL,
    DATASOURCE_ADMIN,
    SECRET_ADMIN
}
