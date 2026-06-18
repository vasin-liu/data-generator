/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Publish-gate governance checks for UDF registry entries (D-21).
 *
 * <p>Covers plaintext-secret detection (D-20), dangerous script-pattern scanning (D-22), JAR
 * manifest validation for Java plugins (D-23), and script JSON-Schema presence (D-12). All
 * failures surface as structured {@link UdfValidationError} codes (D-27).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
public final class UdfGovernanceSupport {

    /** Plaintext password assigned to a JSON key (e.g. {@code "password": "secret"}). */
    private static final Pattern PLAINTEXT_PASSWORD = Pattern.compile(
            "\"(password|apiKey|secret)\"\\s*:\\s*\"[^\"]+\"", Pattern.CASE_INSENSITIVE);
    /** OpenAI-style secret key embedded in a script or payload. */
    private static final Pattern OPENAI_KEY = Pattern.compile("sk-[A-Za-z0-9]{16,}");
    /** Dangerous JS tokens that escape the sandbox or stall the engine. */
    private static final List<String> DANGEROUS_SCRIPT_TOKENS = List.of(
            "java.", "Java.type", "Polyglot", "eval(", "Function(",
            "require(", "process", "globalThis", "while(true)", "while (true)");

    private UdfGovernanceSupport() {
    }

    /**
     * Runs all publish-gate checks for a registry record.
     *
     * @param record                  draft record being published
     * @param rejectPlaintextSecrets  when {@code true}, plaintext secrets are violations
     * @return ordered list of violations (empty when the record passes)
     */
    public static List<UdfValidationError> check(UdfRecord record, boolean rejectPlaintextSecrets) {
        List<UdfValidationError> errors = new ArrayList<>();
        byte[] payload = record.payload();
        switch (record.type()) {
            case JAVA_PLUGIN -> validateJar(payload, errors);
            case SQL, SCRIPT -> validateScriptPayload(record, payload, rejectPlaintextSecrets, errors);
        }
        return errors;
    }

    private static void validateScriptPayload(UdfRecord record, byte[] payload,
                                              boolean rejectPlaintextSecrets, List<UdfValidationError> errors) {
        ScriptUdfPayload parsed;
        try {
            parsed = ScriptUdfPayload.parse(payload);
        } catch (UdfRegistryException e) {
            // Surface payload-parse failures as governance violations rather than aborting the gate.
            errors.addAll(e.errors());
            return;
        }
        String payloadText = new String(payload, StandardCharsets.UTF_8);
        if (rejectPlaintextSecrets && containsPlaintextSecret(payloadText)) {
            errors.add(new UdfValidationError("UDF_SECRET_PLAINTEXT", "payload",
                    "payload contains a plaintext secret; use a secretRef instead"));
        }
        String forbidden = findForbiddenToken(parsed.script());
        if (forbidden != null) {
            errors.add(new UdfValidationError("UDF_SCRIPT_FORBIDDEN_PATTERN", "script",
                    "script uses forbidden token [" + forbidden + "]"));
        }
        if (record.type() == UdfType.SCRIPT && (!parsed.hasInputSchema() || !parsed.hasOutputSchema())) {
            errors.add(new UdfValidationError("UDF_SCHEMA_MISSING", "schema",
                    "script UDF requires non-empty inputSchema and outputSchema"));
        }
    }

    private static boolean containsPlaintextSecret(String payloadText) {
        return PLAINTEXT_PASSWORD.matcher(payloadText).find() || OPENAI_KEY.matcher(payloadText).find();
    }

    private static String findForbiddenToken(String script) {
        for (String token : DANGEROUS_SCRIPT_TOKENS) {
            if (script.contains(token)) {
                return token;
            }
        }
        return null;
    }

    private static void validateJar(byte[] payload, List<UdfValidationError> errors) {
        if (payload.length < 4 || payload[0] != 'P' || payload[1] != 'K') {
            errors.add(new UdfValidationError("UDF_JAR_INVALID", "payload",
                    "java-plugin payload must be a JAR/ZIP archive"));
            return;
        }
        boolean manifestPresent = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(payload))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    manifestPresent = true;
                    break;
                }
            }
        } catch (IOException e) {
            errors.add(new UdfValidationError("UDF_JAR_INVALID", "payload",
                    "java-plugin payload could not be read as a JAR: " + e.getMessage()));
            return;
        }
        if (!manifestPresent) {
            errors.add(new UdfValidationError("UDF_JAR_MANIFEST_MISSING", "payload",
                    "java-plugin JAR is missing META-INF/MANIFEST.MF"));
        }
    }
}
