/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.DataSourceConfigPO;
import org.gensokyo.data.model.po.TemplatePO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Computes lineage snapshots stored on {@code task_execution} rows.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class RunLineageSupport {

    private RunLineageSupport() {
    }

    /**
     * @param template template row at run start
     * @return content hash for template version lineage
     */
    public static String templateVersion(TemplatePO template) {
        if (template == null) {
            return null;
        }
        if (template.getContentMd5() != null && !template.getContentMd5().isBlank()) {
            return template.getContentMd5();
        }
        return sha256Short(template.getContentYaml());
    }

    /**
     * @param properties generator properties
     * @return JSON describing plugin framework configuration
     */
    public static String pluginSetJson(DataGeneratorProperties properties) {
        Map<String, Object> snapshot = new TreeMap<>();
        snapshot.put("framework", properties.getV2PluginFramework());
        snapshot.put("autoRefresh", properties.isV2PluginAutoRefresh());
        snapshot.put("directories", properties.getV2PluginDirectories());
        return TemplateJsonCodec.write(snapshot);
    }

    /**
     * @param rows enabled datasource configs
     * @return short hash of datasource registry snapshot
     */
    public static String datasourceConfigHash(List<DataSourceConfigPO> rows) {
        StringBuilder builder = new StringBuilder();
        rows.stream()
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .forEach(row -> builder.append(row.getName())
                        .append('|')
                        .append(row.getUrl())
                        .append('|')
                        .append(row.getUpdatedAt())
                        .append('\n'));
        return sha256Short(builder.toString());
    }

    private static String sha256Short(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
