/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import org.gensokyo.data.secret.SecretResolver;

/**
 * Fallback resolver for slim test contexts without a secret registry.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
final class PassthroughSecretResolver implements SecretResolver {

    @Override
    public String resolveRequired(String secretRef) {
        throw new IllegalArgumentException("Secret registry is not available in this context: " + secretRef);
    }

    @Override
    public String resolveInlinePassword(String password, String passwordSecretRef) {
        if (passwordSecretRef != null && !passwordSecretRef.isBlank()) {
            return resolveRequired(passwordSecretRef);
        }
        return password == null ? "" : password;
    }
}
