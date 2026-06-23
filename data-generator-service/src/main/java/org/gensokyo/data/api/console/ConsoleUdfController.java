/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.UdfGroupView;
import org.gensokyo.data.api.console.dto.UdfVersionView;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.udf.UdfPublishService;
import org.gensokyo.data.udf.UdfRecord;
import org.gensokyo.data.udf.UdfRegistryService;
import org.gensokyo.data.udf.UdfType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Operator-facing REST surface for the unified UDF registry lifecycle (D-05).
 *
 * <p>Exposes a single multipart upload that creates DRAFT entries of any type (java-plugin / script / sql,
 * D-06), plus publish / deprecate / list / version-history endpoints. Publish and deprecate route through
 * {@link UdfPublishService} so the Phase 2 governance gate, audit log, and runtime refresh all run (D-05);
 * the controller never touches the registry directly for lifecycle transitions. All responses use the
 * {@link R} envelope and project records through payload-free DTOs (D-14). Structured failures bubble to
 * {@code ConsoleApiAdvice} (no controller-side try/catch).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
@RestController
@RequestMapping("/api/console/udfs")
@RequiredArgsConstructor
public class ConsoleUdfController {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final UdfRegistryService udfRegistryService;
    private final UdfPublishService udfPublishService;

    /**
     * Unified multipart upload that registers a DRAFT UDF of any type (D-06/D-07).
     *
     * <p>For {@code java-plugin} the JAR bytes come from {@code file}. For {@code script}/{@code sql} the
     * controller assembles the {@code ScriptUdfPayload} JSON envelope
     * ({@code {sqlName, argCount, returnType, script, inputSchema?, outputSchema?}}) that the publish gate
     * and Calcite runtime both consume — the raw body alone is not a valid payload. Governance defers to
     * publish, so this only creates a draft; {@code script} UDFs additionally require non-empty input/output
     * schemas to pass publish (D-12), supplied here as JSON.
     *
     * @param udfId        reverse-DNS identifier
     * @param version      semver version
     * @param type         UDF type ({@code java-plugin}/{@code script}/{@code sql})
     * @param file         JAR artifact for {@code java-plugin}; ignored for text types
     * @param scriptBody   script source for {@code script} type
     * @param sql          SQL definition for {@code sql} type
     * @param sqlName      Calcite-resolvable function name for {@code script}/{@code sql} types (required)
     * @param argCount     argument count for {@code script}/{@code sql} (0-2; defaults to 1)
     * @param returnType   declared SQL return type for {@code script}/{@code sql} (defaults to VARCHAR)
     * @param inputSchema  JSON Schema for the script input (required at publish for {@code script}, D-12)
     * @param outputSchema JSON Schema for the script output (required at publish for {@code script}, D-12)
     * @return draft view
     * @throws IOException when the multipart file cannot be read
     * @throws IllegalArgumentException when the artifact body or required fields for the type are missing
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<UdfVersionView> upload(
            @RequestParam String udfId,
            @RequestParam String version,
            @RequestParam String type,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String scriptBody,
            @RequestParam(required = false) String sql,
            @RequestParam(required = false) String sqlName,
            @RequestParam(required = false) Integer argCount,
            @RequestParam(required = false) String returnType,
            @RequestParam(required = false) String inputSchema,
            @RequestParam(required = false) String outputSchema) throws IOException {
        UdfType udfType = UdfType.fromValue(type);
        byte[] payload = switch (udfType) {
            case JAVA_PLUGIN -> {
                if (file == null || file.isEmpty()) {
                    throw new IllegalArgumentException("java-plugin upload requires a non-empty JAR file part");
                }
                yield file.getBytes();
            }
            case SCRIPT -> {
                if (scriptBody == null || scriptBody.isBlank()) {
                    throw new IllegalArgumentException("script upload requires a non-blank scriptBody field");
                }
                yield scriptEnvelope(sqlName, argCount, returnType, scriptBody, inputSchema, outputSchema);
            }
            case SQL -> {
                if (sql == null || sql.isBlank()) {
                    throw new IllegalArgumentException("sql upload requires a non-blank sql field");
                }
                yield scriptEnvelope(sqlName, argCount, returnType, sql, null, null);
            }
            default -> throw new IllegalArgumentException("Unsupported UDF type: " + type);
        };
        UdfRecord draft = udfRegistryService.registerDraft(udfId, version, udfType, payload, Map.of());
        return R.ok(UdfVersionView.from(draft));
    }

    /**
     * Builds the {@code ScriptUdfPayload} JSON envelope consumed by the publish gate and the Calcite
     * runtime so SQL and script UDFs share one payload shape. {@code sqlName} is the function name
     * referenced from templates (Phase 2 D-18); the optional schemas are embedded verbatim for script UDFs.
     */
    private static byte[] scriptEnvelope(String sqlName, Integer argCount, String returnType, String script,
                                         String inputSchema, String outputSchema) {
        if (sqlName == null || sqlName.isBlank()) {
            throw new IllegalArgumentException("script/sql upload requires a non-blank sqlName field");
        }
        ObjectNode envelope = MAPPER.createObjectNode();
        envelope.put("sqlName", sqlName.trim());
        envelope.put("argCount", argCount == null ? 1 : argCount);
        envelope.put("returnType", returnType == null || returnType.isBlank() ? "VARCHAR" : returnType.trim());
        envelope.put("script", script);
        // Schemas are optional at draft time but required for SCRIPT publish (D-12); embed when supplied.
        if (inputSchema != null && !inputSchema.isBlank()) {
            envelope.set("inputSchema", parseSchema(inputSchema, "inputSchema"));
        }
        if (outputSchema != null && !outputSchema.isBlank()) {
            envelope.set("outputSchema", parseSchema(outputSchema, "outputSchema"));
        }
        return MAPPER.writeValueAsString(envelope).getBytes(StandardCharsets.UTF_8);
    }

    private static JsonNode parseSchema(String json, String field) {
        try {
            return MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(field + " must be valid JSON: " + e.getMessage());
        }
    }

    /**
     * Runs the Phase 2 publish gate for a draft version (D-05).
     *
     * @param udfId   reverse-DNS identifier
     * @param version semver version
     * @return published view
     */
    @PostMapping("/{udfId}/{version}/publish")
    public R<UdfVersionView> publish(@PathVariable String udfId, @PathVariable String version) {
        return R.ok(UdfVersionView.from(udfPublishService.publish(udfId, version)));
    }

    /**
     * Deprecates a published version (D-05).
     *
     * @param udfId   reverse-DNS identifier
     * @param version semver version
     * @return deprecated view
     */
    @PostMapping("/{udfId}/{version}/deprecate")
    public R<UdfVersionView> deprecate(@PathVariable String udfId, @PathVariable String version) {
        return R.ok(UdfVersionView.from(udfPublishService.deprecate(udfId, version)));
    }

    /**
     * Lists UDFs grouped by {@code udfId} with full version history (D-14/D-08).
     *
     * @param type optional type filter ({@code java-plugin}/{@code script}/{@code sql})
     * @return grouped views, never exposing payload bytes
     */
    @GetMapping
    public R<List<UdfGroupView>> list(@RequestParam(required = false) String type) {
        Optional<UdfType> typeFilter = (type == null || type.isBlank())
                ? Optional.empty()
                : Optional.of(UdfType.fromValue(type));
        return R.ok(groupByUdfId(udfRegistryService.list(typeFilter)));
    }

    /**
     * Returns the version history for a single {@code udfId} (D-08).
     *
     * @param udfId reverse-DNS identifier
     * @return grouped view for that id (empty versions when unknown)
     */
    @GetMapping("/{udfId}")
    public R<UdfGroupView> history(@PathVariable String udfId) {
        List<UdfRecord> records = udfRegistryService.list(Optional.empty()).stream()
                .filter(record -> record.udfId().equals(udfId))
                .toList();
        return R.ok(UdfGroupView.of(udfId, records));
    }

    private static List<UdfGroupView> groupByUdfId(List<UdfRecord> records) {
        // Preserve registry ordering of ids while collecting each id's versions together.
        Map<String, List<UdfRecord>> byId = new LinkedHashMap<>();
        for (UdfRecord record : records) {
            byId.computeIfAbsent(record.udfId(), key -> new ArrayList<>()).add(record);
        }
        List<UdfGroupView> groups = new ArrayList<>(byId.size());
        byId.forEach((id, group) -> groups.add(UdfGroupView.of(id, group)));
        return groups;
    }
}
