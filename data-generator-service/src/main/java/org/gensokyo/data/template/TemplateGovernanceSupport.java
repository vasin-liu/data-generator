/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Template governance checks (plaintext secrets, publish readiness).
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class TemplateGovernanceSupport {

    private TemplateGovernanceSupport() {
    }

    /**
     * @param template normalized template
     * @param rejectPlaintextPasswords when true, inline plaintext passwords are errors
     * @return validation error messages
     */
    public static List<String> collectSecretViolations(TemplateV2VO template, boolean rejectPlaintextPasswords) {
        List<String> errors = new ArrayList<>();
        if (!rejectPlaintextPasswords || template == null) {
            return errors;
        }
        if (!CollectKit.isEmpty(template.getSources())) {
            for (var entry : template.getSources().entrySet()) {
                collectInlineSecretErrors(errors, entry.getKey(), entry.getValue());
                collectAiSecretErrors(errors, entry.getKey(), entry.getValue());
            }
        }
        if (!CollectKit.isEmpty(template.getSinks())) {
            for (int i = 0; i < template.getSinks().size(); i++) {
                WriteStageVO sink = template.getSinks().get(i);
                if (sink == null || CollectKit.isEmpty(sink.getWriters())) {
                    continue;
                }
                for (WriterVO writer : sink.getWriters()) {
                    if (writer instanceof JdbcWriterVO jdbc) {
                        collectJdbcWriterSecretErrors(errors, "sinks[" + i + "]", jdbc);
                    }
                }
            }
        }
        return errors;
    }

    private static void collectInlineSecretErrors(List<String> errors, String sourceName, SourceVO source) {
        if (source instanceof QuerySourceVO query) {
            collectInline(errors, "sources." + sourceName, query.getDataSource());
        }
        if (source instanceof PostGisQuerySourceVO postGis) {
            collectInline(errors, "sources." + sourceName, postGis.getDataSource());
        }
    }

    private static void collectAiSecretErrors(List<String> errors, String sourceName, SourceVO source) {
        if (!(source instanceof AiSourceVO aiSource)) {
            return;
        }
        collectAiProviderSecretErrors(errors, "sources." + sourceName, aiSource.getProvider());
    }

    private static void collectAiProviderSecretErrors(List<String> errors, String path, AiProviderVO provider) {
        if (provider == null || StrKit.isBlank(provider.getType())) {
            return;
        }
        String providerType = provider.getType().trim().toUpperCase(Locale.ROOT);
        if (!"OPENAI".equals(providerType) && !"AZURE_OPENAI".equals(providerType)) {
            return;
        }
        Map<String, Object> options = provider.getOptions() == null ? Map.of() : provider.getOptions();
        String apiKey = stringOption(options, "apiKey");
        String apiKeySecretRef = stringOption(options, "apiKeySecretRef");
        if (StrKit.isNotBlank(apiKey) && StrKit.isBlank(apiKeySecretRef)) {
            errors.add(path + ".provider.options: plaintext apiKey is not allowed; use apiKeySecretRef");
        }
    }

    private static String stringOption(Map<String, Object> options, String key) {
        Object value = options.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static void collectJdbcWriterSecretErrors(List<String> errors, String path, JdbcWriterVO writer) {
        collectInline(errors, path, writer.getDataSource());
    }

    private static void collectInline(List<String> errors, String path, InlineDataSourceVO inline) {
        if (inline == null) {
            return;
        }
        if (StrKit.isNotBlank(inline.getPassword()) && StrKit.isBlank(inline.getPasswordSecretRef())) {
            errors.add(path + ".dataSource: plaintext password is not allowed; use passwordSecretRef");
        }
    }
}
