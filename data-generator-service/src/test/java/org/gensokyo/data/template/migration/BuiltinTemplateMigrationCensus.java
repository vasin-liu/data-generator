/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.template.BuiltinClasspathTemplateCatalog;
import org.gensokyo.data.yaml.JacksonParser;
import org.gensokyo.kit.collect.CollectKit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Runs {@link V1TemplateMigrationAnalyzer} over all built-in classpath templates and aggregates results.
 *
 * @author Gensokyo
 * @since 2026-05-22
 */
public final class BuiltinTemplateMigrationCensus {

    private static final JacksonParser YAML_PARSER = new JacksonParser();

    private BuiltinTemplateMigrationCensus() {
    }

    /**
     * Analyzes every built-in template fixture and returns rows plus summary counts.
     *
     * @return census result for reporting
     */
    public static CensusResult run() {
        List<Row> rows = new ArrayList<>();
        for (BuiltinClasspathTemplateCatalog.Fixture fixture : BuiltinClasspathTemplateCatalog.loadAll()) {
            TemplateVO v1 = YAML_PARSER.parse(fixture.yaml(), TemplateVO.class);
            TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);
            rows.add(new Row(fixture.relativePath(), analysis));
        }
        rows.sort((a, b) -> a.relativePath().compareTo(b.relativePath()));
        return new CensusResult(rows, summarize(rows));
    }

    /**
     * Renders the census as markdown for {@code docs/migration/reports/}.
     *
     * @param result census output
     * @return markdown document
     */
    public static String toMarkdown(CensusResult result) {
        Summary summary = result.summary();
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Built-in template migration census\n\n");
        markdown.append("Generated: ").append(LocalDate.now()).append(" (`BuiltinTemplateMigrationCensus`)\n\n");
        markdown.append("> Staging-free evidence for W3 orchestration blocking. Production `db-{id}` census is **M2**.\n\n");

        markdown.append("## Summary\n\n");
        markdown.append("- **Total templates:** ").append(summary.total()).append('\n');
        markdown.append("- **COMPATIBILITY_ONLY:** ")
                .append(summary.compatibilityOnly())
                .append(" (")
                .append(percent(summary.compatibilityOnly(), summary.total()))
                .append("%)\n");
        appendCountSection(markdown, "By scenario family", summary.byFamily());
        appendCountSection(markdown, "By recommended path", summary.byPath());
        appendCountSection(markdown, "By suggested class", summary.byClass());
        appendCountSection(markdown, "Blocker signals (non-exclusive)", summary.byBlockerSignal());

        markdown.append("\n## Detail\n\n");
        markdown.append("| Path | Family | Class | Path | Blockers |\n");
        markdown.append("|------|--------|-------|------|----------|\n");
        for (Row row : result.rows()) {
            TemplateMigrationAnalysisDTO analysis = row.analysis();
            markdown.append("| `").append(escapePipe(row.relativePath())).append("` | ");
            markdown.append(nullToDash(analysis.getScenarioFamily())).append(" | ");
            markdown.append(analysis.getSuggestedClass()).append(" | ");
            markdown.append(nullToDash(analysis.getRecommendedPath())).append(" | ");
            markdown.append(escapePipe(formatBlockers(analysis.getBlockers()))).append(" |\n");
        }
        return markdown.toString();
    }

    private static Summary summarize(List<Row> rows) {
        Map<String, Long> byFamily = new TreeMap<>();
        Map<String, Long> byPath = new TreeMap<>();
        Map<String, Long> byClass = new TreeMap<>();
        Map<String, Long> byBlockerSignal = new TreeMap<>();
        int compatibilityOnly = 0;

        for (Row row : rows) {
            TemplateMigrationAnalysisDTO analysis = row.analysis();
            if (analysis.getSuggestedClass() == MigrationClassification.COMPATIBILITY_ONLY) {
                compatibilityOnly++;
            }
            increment(byFamily, nullToKey(analysis.getScenarioFamily()));
            increment(byPath, nullToKey(analysis.getRecommendedPath()));
            increment(byClass, analysis.getSuggestedClass() == null ? "null" : analysis.getSuggestedClass().name());
            for (String signal : blockerSignals(analysis.getBlockers())) {
                increment(byBlockerSignal, signal);
            }
        }
        return new Summary(rows.size(), compatibilityOnly, byFamily, byPath, byClass, byBlockerSignal);
    }

    private static List<String> blockerSignals(List<String> blockers) {
        List<String> signals = new ArrayList<>();
        if (CollectKit.isEmpty(blockers)) {
            return signals;
        }
        for (String blocker : blockers) {
            if (blocker == null) {
                continue;
            }
            String lower = blocker.toLowerCase(Locale.ROOT);
            if (lower.contains("pause")) {
                signals.add("PAUSE");
            }
            if (lower.contains("log orchestration") || lower.contains("logging side effects")) {
                signals.add("LOG");
            }
            if (lower.contains("shared orchestration") || lower.contains("cross-row sharing")) {
                signals.add("SHARED");
            }
            if (lower.contains("javascript")) {
                signals.add("JAVASCRIPT");
            }
        }
        return signals.stream().distinct().toList();
    }

    private static void appendCountSection(StringBuilder markdown, String title, Map<String, Long> counts) {
        markdown.append("\n### ").append(title).append("\n\n");
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            markdown.append("- **").append(entry.getKey()).append(":** ").append(entry.getValue()).append('\n');
        }
    }

    private static void increment(Map<String, Long> counts, String key) {
        counts.merge(key, 1L, Long::sum);
    }

    private static int percent(int part, int total) {
        if (total == 0) {
            return 0;
        }
        return Math.round(part * 100f / total);
    }

    private static String nullToKey(String value) {
        return value == null || value.isBlank() ? "(none)" : value;
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String formatBlockers(List<String> blockers) {
        if (CollectKit.isEmpty(blockers)) {
            return "—";
        }
        return String.join("; ", blockers);
    }

    private static String escapePipe(String value) {
        return value.replace("|", "\\|");
    }

    /**
     * One analyzed built-in template row.
     *
     * @param relativePath path under {@code template/}
     * @param analysis     analyzer output
     */
    public record Row(String relativePath, TemplateMigrationAnalysisDTO analysis) {
    }

    /**
     * Aggregated census counts.
     *
     * @param total             template count
     * @param compatibilityOnly COMPATIBILITY_ONLY count
     * @param byFamily          counts by scenario family
     * @param byPath            counts by recommended path
     * @param byClass           counts by suggested class
     * @param byBlockerSignal   non-exclusive blocker signal counts
     */
    public record Summary(
            int total,
            int compatibilityOnly,
            Map<String, Long> byFamily,
            Map<String, Long> byPath,
            Map<String, Long> byClass,
            Map<String, Long> byBlockerSignal) {
    }

    /**
     * Full census output.
     *
     * @param rows    detail rows
     * @param summary aggregates
     */
    public record CensusResult(List<Row> rows, Summary summary) {
    }
}
