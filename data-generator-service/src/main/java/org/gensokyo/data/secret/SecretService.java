/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.secret;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.po.SecretEntryPO;
import org.gensokyo.data.repository.SecretEntryRepository;
import org.gensokyo.kit.character.StrKit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * CRUD and resolution for logical secret references.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Service
@RequiredArgsConstructor
public class SecretService implements SecretResolver {

    private final SecretEntryRepository repository;

    /**
     * @return all secret names (values are never returned in list APIs)
     */
    public List<SecretSummary> listSummaries() {
        return repository.findAll().stream()
                .map(row -> new SecretSummary(row.getName(), row.getDescription(), row.getUpdatedAt()))
                .toList();
    }

    /**
     * Creates or updates a secret entry.
     *
     * @param name        logical name
     * @param value       secret value
     * @param description optional note
     */
    @Transactional
    public void upsert(String name, String value, String description) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Secret name must not be blank");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Secret value must not be blank");
        }
        Instant now = Instant.now();
        SecretEntryPO row = repository.findById(name).orElseGet(SecretEntryPO::new);
        boolean isNew = row.getName() == null;
        row.setName(name.trim());
        row.setSecretValue(value);
        row.setDescription(description);
        if (isNew) {
            row.setCreatedAt(now);
        }
        row.setUpdatedAt(now);
        repository.saveAndFlush(row);
    }

    /**
     * Removes a secret entry.
     *
     * @param name logical name
     */
    @Transactional
    public void delete(String name) {
        if (StrKit.isBlank(name)) {
            throw new IllegalArgumentException("Secret name must not be blank");
        }
        repository.deleteById(name.trim());
    }

    @Override
    public String resolveRequired(String secretRef) {
        if (StrKit.isBlank(secretRef)) {
            throw new IllegalArgumentException("secretRef must not be blank");
        }
        String normalized = secretRef.trim();
        String envKey = toEnvKey(normalized);
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return repository.findById(normalized)
                .map(SecretEntryPO::getSecretValue)
                .orElseThrow(() -> new IllegalArgumentException("Unknown secretRef: " + normalized));
    }

    private static String toEnvKey(String secretRef) {
        String slug = secretRef.replace('/', '_').replace('-', '_').toUpperCase(Locale.ROOT);
        return "DG_SECRET_" + slug;
    }

    /**
     * Secret metadata without the value.
     *
     * @param name        logical name
     * @param description optional description
     * @param updatedAt   last update
     */
    public record SecretSummary(String name, String description, Instant updatedAt) {
    }
}
