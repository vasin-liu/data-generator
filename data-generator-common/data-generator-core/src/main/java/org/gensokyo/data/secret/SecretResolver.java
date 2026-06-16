/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.secret;

/**
 * Resolves logical secret references to credential values at runtime.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public interface SecretResolver {

    /**
     * Resolves a secret reference to its value.
     *
     * @param secretRef logical secret name (non-blank)
     * @return resolved secret value
     * @throws IllegalArgumentException when the reference is unknown
     */
    String resolveRequired(String secretRef);

    /**
     * Resolves inline JDBC password from plaintext or secret reference.
     *
     * @param password          optional plaintext
     * @param passwordSecretRef optional secret ref
     * @return resolved password or empty string
     */
    default String resolveInlinePassword(String password, String passwordSecretRef) {
        if (passwordSecretRef != null && !passwordSecretRef.isBlank()) {
            return resolveRequired(passwordSecretRef);
        }
        return password == null ? "" : password;
    }

    /**
     * Resolves a credential from plaintext or secret reference (secret ref wins when present).
     *
     * @param plaintext optional inline value
     * @param secretRef optional logical secret name
     * @param fieldName operator-facing field label for error messages
     * @return resolved credential
     * @throws IllegalArgumentException when neither value is present
     */
    default String resolvePlaintextOrSecretRef(String plaintext, String secretRef, String fieldName) {
        if (secretRef != null && !secretRef.isBlank()) {
            return resolveRequired(secretRef);
        }
        if (plaintext != null && !plaintext.isBlank()) {
            return plaintext;
        }
        throw new IllegalArgumentException(fieldName + " requires a plaintext value or secretRef");
    }
}
