/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import org.gensokyo.data.datasource.api.ConnectionKind;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks recent successful connectivity tests for save/publish gates (D-19).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
@Component
public class ConnectivityTestGate {

    private static final long MAX_AGE_SECONDS = 3600;

    private final ConcurrentHashMap<String, Instant> lastSuccess = new ConcurrentHashMap<>();

    /**
     * Records a successful connectivity test keyed by config fingerprint (draft/new-row flow).
     *
     * @param kind                connection kind
     * @param configFingerprint   config parameters used for the test
     */
    public void recordFingerprintSuccess(ConnectionKind kind, Map<String, Object> configFingerprint) {
        lastSuccess.put(fingerprintKey(kind, configFingerprint), Instant.now());
    }

    /**
     * Records a successful connectivity test for a named connection.
     *
     * @param kind connection kind
     * @param name connection name
     * @param configFingerprint optional config map used to invalidate stale passes
     */
    public void recordSuccess(ConnectionKind kind, String name, Map<String, Object> configFingerprint) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
        lastSuccess.put(cacheKey(kind, name, configFingerprint), Instant.now());
        if (configFingerprint != null && !configFingerprint.isEmpty()) {
            lastSuccess.put(fingerprintKey(kind, configFingerprint), Instant.now());
        }
    }

    /**
     * Ensures a recent successful test exists before persisting a connection.
     *
     * @param kind connection kind
     * @param name connection name
     * @param configFingerprint config used for the test
     * @throws IllegalArgumentException when no recent successful test is recorded
     */
    public void requireRecentSuccess(ConnectionKind kind, String name, Map<String, Object> configFingerprint) {
        String nameKey = cacheKey(kind, name, configFingerprint);
        String fpKey = fingerprintKey(kind, configFingerprint);
        Instant at = lastSuccess.get(nameKey);
        if (at == null) {
            at = lastSuccess.get(fpKey);
        }
        if (at == null || at.isBefore(Instant.now().minusSeconds(MAX_AGE_SECONDS))) {
            throw new IllegalArgumentException(
                    "Connectivity test required before save — run test for " + kind + " connection '" + name + "'");
        }
    }

    private static String fingerprintKey(ConnectionKind kind, Map<String, Object> fingerprint) {
        String hash = fingerprint == null || fingerprint.isEmpty()
                ? "default"
                : sha256Short(fingerprint.toString());
        return kind.name() + ":fp:" + hash;
    }

    private static String cacheKey(ConnectionKind kind, String name, Map<String, Object> fingerprint) {
        String hash = fingerprint == null || fingerprint.isEmpty()
                ? "default"
                : sha256Short(fingerprint.toString());
        return kind.name() + ":" + name.trim() + ":" + hash;
    }

    private static String sha256Short(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 8);
        } catch (Exception ex) {
            return "unknown";
        }
    }
}
