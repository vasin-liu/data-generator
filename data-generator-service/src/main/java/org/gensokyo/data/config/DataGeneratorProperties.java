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
    }

    /**
     * @return governance settings (never null)
     */
    public Governance getGovernance() {
        return governance == null ? new Governance() : governance;
    }

}

