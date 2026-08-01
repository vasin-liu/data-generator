/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 元数据信息
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
@Component
@ConfigurationProperties(prefix = DataGeneratorProperties.PREFIX)
@Setter
@Getter
public class DataGeneratorProperties {
    /**
     * 组件配置的前缀
     */
    public static final String PREFIX = "data.generator";

    /**
     * 忽略文件前缀集合
     */
    private String[] ignorePrefix = new String[]{"___", "!"};

    /**
     * 核心线程数
     */
    private Integer corePoolSize = 50;
    /**
     * 最大线程数
     */
    private Integer maxPoolSize = 100;
    /**
     * 队列任务数
     */
    private Integer queueCapacity = 100;
    /**
     * 元数据最大缓存数量
     */
    private Integer metaCacheMaximumSize = 100;

    private List<String> v2PluginDirectories = new ArrayList<>();

    private boolean v2PluginAutoRefresh = true;

    private String v2PluginFramework = "PF4J";

    /**
     * Default row cap for Template V2 control-plane preview when the caller omits {@code maxRows}.
     */
    private Integer previewMaxRows = 100;

    /**
     * Deprecated V1 runtime toggle (maps to {@code data.generator.v1-execution.enabled}).
     * Ignored for task runs after Wave 0 retirement; retained for config binding and console banner.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    private V1Execution v1Execution = new V1Execution();

    /**
     * Template and secret governance (Phase B).
     */
    private Governance governance = new Governance();

    /**
     * Returns whether legacy V1 template execution is advertised as enabled.
     * Task runs always reject V1 templates after Wave 0 retirement; this reflects config only.
     *
     * @return always {@code false} (V1 execution path removed from {@link org.gensokyo.data.controller.TaskController})
     * @deprecated V1 task execution was retired in Wave 0; property is ignored for runs.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public boolean isV1ExecutionEnabled() {
        return false;
    }

    /**
     * Nested binding for {@code data.generator.v1-execution.*} (deprecated; ignored for task runs).
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    @Getter
    @Setter
    public static class V1Execution {
        /**
         * Legacy flag; no longer gates {@link org.gensokyo.data.controller.TaskController} V1 runs.
         */
        @Deprecated(since = "3.0.0", forRemoval = true)
        private boolean enabled = false;
    }

    /**
     * Nested binding for {@code data.generator.governance.*}.
     */
    @Getter
    @Setter
    public static class Governance {
        /**
         * When {@code true}, templates with inline plaintext JDBC passwords fail validation/publish.
         */
        private boolean rejectPlaintextPasswordsInTemplates = true;

        /**
         * When {@code true}, {@code /task/run} requires template status {@code PUBLISHED}.
         */
        private boolean requirePublishedForTaskRun = true;

        /**
         * When {@code true}, templates must use managed catalog refs instead of inline connection blocks (D-13).
         * Binds to {@code require-managed-connections}.
         */
        private boolean requireManagedConnections = false;

        /**
         * When {@code true}, datasource save requires a recent successful connectivity test (D-19).
         * Binds to {@code require-connectivity-test-before-save}.
         */
        private boolean requireConnectivityTestBeforeSave = false;

        /**
         * When {@code true}, template publish requires referenced connections to have passed connectivity test (D-19).
         * Binds to {@code require-connectivity-test-before-publish}.
         */
        private boolean requireConnectivityTestBeforePublish = false;

        /**
         * When {@code true}, templates may reference yaml-seeded BOOTSTRAP catalog entries (D-15).
         * Binds to {@code allow-bootstrap-references}.
         */
        private boolean allowBootstrapReferences = true;
    }

    /**
     * @return governance settings (never null)
     */
    public Governance getGovernance() {
        return governance == null ? new Governance() : governance;
    }

    /**
     * Upload validation limits for durable GeoJSON assets (Phase 21).
     */
    private GeoAssets geoAssets = new GeoAssets();

    /**
     * Platform defaults for remote AI runtime throttling.
     */
    private AiRuntime aiRuntime = new AiRuntime();

    /**
     * @return geo asset upload limits (never null)
     */
    public GeoAssets getGeoAssets() {
        return geoAssets == null ? new GeoAssets() : geoAssets;
    }

    /**
     * @return AI runtime settings (never null)
     */
    public AiRuntime getAiRuntime() {
        return aiRuntime == null ? new AiRuntime() : aiRuntime;
    }

    /**
     * Nested binding for {@code data.generator.geo-assets.*}.
     */
    @Getter
    @Setter
    public static class GeoAssets {
        /**
         * Maximum allowed GeoJSON upload size in bytes (default 50 MiB).
         */
        private long maxBytes = 52_428_800L;

        /**
         * Maximum allowed feature count per asset.
         */
        private int maxFeatures = 10_000;
    }

    /**
     * Nested binding for {@code data.generator.ai-runtime.*}.
     */
    @Getter
    @Setter
    public static class AiRuntime {
        /**
         * Default minimum gap between AI calls per limiter key (0 = disabled).
         */
        private Long defaultMinIntervalMs = 0L;

        /**
         * Default max AI calls per rolling minute per limiter key (0 = disabled).
         */
        private Integer defaultRequestsPerMinute = 0;

        /**
         * Optional per-model USD pricing overrides (USD per 1M tokens).
         */
        private java.util.List<AiModelPricingEntry> modelPricing = new java.util.ArrayList<>();

        /**
         * When {@code true}, AI rate limits are coordinated via {@code ai_rate_limit_state} (multi-JVM).
         */
        private boolean distributedRateLimitEnabled = false;

        /**
         * Platform daily AI quota limits and enforcement toggle.
         */
        private AiRuntimeQuota quota = new AiRuntimeQuota();
    }

    /**
     * Nested binding for {@code data.generator.ai-runtime.quota.*}.
     */
    @Getter
    @Setter
    public static class AiRuntimeQuota {
        private boolean enabled = false;
        private Long maxCallsPerDay = 0L;
        private Long maxTokensPerDay = 0L;
        private Double maxCostUsdPerDay = 0.0D;
        /** When {@code true}, quota warn/exceed events are appended to the audit log. */
        private boolean alertsEnabled = false;
        /** Warn threshold percent (1–100); 0 disables warn alerts. */
        private Integer warnAtPercent = 80;
        /** Optional per-provider, per-template, or per-tenant daily quota overrides. */
        private java.util.List<AiQuotaScopeOverride> scopeOverrides = new java.util.ArrayList<>();
        /** When {@code true}, quota warn/exceed events are POSTed to configured webhook URLs. */
        private boolean webhooksEnabled = false;
        /** Outbound webhook endpoints for quota notifications. */
        private java.util.List<AiQuotaWebhookEndpoint> webhooks = new java.util.ArrayList<>();
    }

    /**
     * Outbound webhook endpoint for {@code data.generator.ai-runtime.quota.webhooks}.
     */
    @Getter
    @Setter
    public static class AiQuotaWebhookEndpoint {
        private String url;
        /** Optional header name for shared-secret authentication. */
        private String secretHeaderName;
        /** Optional header value; never returned by console APIs. */
        private String secretValue;
        /** Subscribed events: {@code WARN}, {@code EXCEEDED} (default both). */
        private java.util.List<String> events = new java.util.ArrayList<>(java.util.List.of("WARN", "EXCEEDED"));
    }

    /**
     * Scoped AI quota override for {@code data.generator.ai-runtime.quota.scope-overrides}.
     */
    @Getter
    @Setter
    public static class AiQuotaScopeOverride {
        /** {@code PROVIDER}, {@code TEMPLATE}, or {@code TENANT}. */
        private String scopeType;
        /** Provider type (e.g. {@code OPENAI}) or template snowflake id. */
        private String scopeKey;
        private Long maxCallsPerDay = 0L;
        private Long maxTokensPerDay = 0L;
        private Double maxCostUsdPerDay = 0.0D;
    }

    /**
     * Configurable model pricing entry for {@code data.generator.ai-runtime.model-pricing}.
     */
    @Getter
    @Setter
    public static class AiModelPricingEntry {
        private String providerType;
        /** Model name or {@code *} for provider default. */
        private String model;
        private Double promptUsdPer1M;
        private Double completionUsdPer1M;
    }

}

