/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.exception.DataGeneratorException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Writes migration comparison reports as markdown under {@code docs/migration/reports/}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class MigrationReportWriter {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path reportsDirectory;

    /**
     * Creates a writer that persists reports under the given directory.
     *
     * @param reportsDirectory directory for markdown reports (created when missing)
     */
    public MigrationReportWriter(Path reportsDirectory) {
        this.reportsDirectory = Objects.requireNonNull(reportsDirectory, "reportsDirectory");
    }

    /**
     * Writes a comparison report and sets {@link MigrationComparisonReport#setReportPath(String)}.
     *
     * @param report comparison result; {@code templateId} must be set
     * @return relative path to the written file (POSIX-style separators)
     */
    public String write(MigrationComparisonReport report) {
        Objects.requireNonNull(report, "report");
        if (report.getTemplateId() == null) {
            throw new IllegalArgumentException("report.templateId must be set");
        }

        String timestamp = TIMESTAMP.format(LocalDateTime.now());
        String fileName = "db-" + report.getTemplateId() + "-" + timestamp + ".md";
        Path target = reportsDirectory.resolve(fileName);

        try {
            Files.createDirectories(reportsDirectory);
            Files.writeString(target, renderMarkdown(report), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new DataGeneratorException("Failed to write migration report [" + target + "]", e);
        }

        String relative = Path.of("docs", "migration", "reports", fileName).toString().replace('\\', '/');
        report.setReportPath(relative);
        return relative;
    }

    private static String renderMarkdown(MigrationComparisonReport report) {
        StringBuilder md = new StringBuilder();
        md.append("# Migration compare report\n\n");
        md.append("## Summary\n\n");
        md.append("- Template id: ").append(report.getTemplateId()).append('\n');
        md.append("- Classification: ").append(report.getClassification()).append('\n');
        md.append("- Recommendation: ").append(report.getRecommendation()).append('\n');
        md.append("\n## Counts\n\n");
        md.append("| Pipeline | Row count |\n");
        md.append("|----------|----------:|\n");
        md.append("| V1 | ").append(report.getV1RowCount()).append(" |\n");
        md.append("| V2 | ").append(report.getV2RowCount()).append(" |\n");
        md.append("\n## Sample match\n\n");
        md.append("- Sample size: ").append(report.getSampleSize()).append('\n');
        md.append("- Sample match rate: ").append(String.format("%.4f", report.getSampleMatchRate())).append('\n');
        md.append("\n## Warnings\n\n");
        appendWarnings(md, report.getWarnings());
        appendPlanExplain(md, report.getPlanExplain());
        md.append("\n## Classification\n\n");
        md.append("Final class **").append(report.getClassification()).append("** — promote guidance: `")
                .append(report.getRecommendation()).append("`.\n");
        return md.toString();
    }

    private static void appendPlanExplain(StringBuilder md, MigrationPlanExplain plan) {
        md.append("\n## Plan explain (bounded)\n\n");
        if (plan == null) {
            md.append("_Not available_\n");
            return;
        }
        if (plan.getV2Sql() != null) {
            md.append("### V2 SQL\n\n```sql\n").append(plan.getV2Sql()).append("\n```\n\n");
        }
        md.append("- Execution shape: ").append(nullToDash(plan.getExecutionShape())).append('\n');
        md.append("- Effective mode: ").append(nullToDash(plan.getEffectiveExecutionMode())).append('\n');
        md.append("- Calcite validation: ").append(nullToDash(plan.getCalciteValidation())).append('\n');
        appendBulletSection(md, "Sources", plan.getSourceSummaries());
        appendBulletSection(md, "SQL features (keyword scan)", plan.getPlanFeatures());
        appendBulletSection(md, "V1 hints", plan.getV1Hints());
        appendBulletSection(md, "Plan diff notes", plan.getDiffNotes());
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static void appendBulletSection(StringBuilder md, String title, List<String> items) {
        md.append("\n### ").append(title).append("\n\n");
        if (items == null || items.isEmpty()) {
            md.append("_None_\n");
            return;
        }
        for (String item : items) {
            md.append("- ").append(item == null ? "" : item.strip()).append('\n');
        }
    }

    private static void appendWarnings(StringBuilder md, List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            md.append("_None_\n");
            return;
        }
        for (String warning : warnings) {
            md.append("- ").append(warning == null ? "" : warning.strip()).append('\n');
        }
    }
}
