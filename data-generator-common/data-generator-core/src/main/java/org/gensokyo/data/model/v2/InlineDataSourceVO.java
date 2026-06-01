/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inline JDBC endpoint embedded in a template (Seatunnel-style portability).
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@Getter
@Setter
public class InlineDataSourceVO implements Serializable {
    private String name;
    private String type;
    private String url;
    private String username;
    /** Plain password; discouraged when governance rejects plaintext secrets. */
    private String password;
    /** Logical secret name resolved via {@link org.gensokyo.data.secret.SecretResolver}. */
    private String passwordSecretRef;
    private String driverClassName;
    private Map<String, String> properties = new LinkedHashMap<>();
}
