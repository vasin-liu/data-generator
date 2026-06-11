/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sql;

import net.datafaker.Faker;
import org.gensokyo.data.calcite.V2TransformFactory;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SpelColumnMapping;
import org.gensokyo.data.model.v2.SpelTransformVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Row-local SpEL transform: reads table {@code input}, evaluates column mappings, merges into output rows.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public class SpelTransformFactory implements V2TransformFactory {

    private static final String INPUT_TABLE = "input";
    private static final String ROW_VARIABLE = "row";

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    // Thread-local faker matches V1 script variable semantics without shared mutable RNG races.
    private static final ThreadLocal<Faker> FAKER = ThreadLocal.withInitial(
            () -> new Faker(Locale.CHINA, ThreadLocalRandom.current()));

    /**
     * Returns whether this factory handles {@link SpelTransformVO}.
     *
     * @param transform transform configuration
     * @return {@code true} for SpEL transforms
     */
    @Override
    public boolean supports(TransformVO transform) {
        return transform instanceof SpelTransformVO;
    }

    /**
     * Evaluates each configured SpEL column mapping against every row in table {@code input}.
     *
     * @param transform SpEL transform definition
     * @param context execution context containing table {@code input}
     * @return merged schema and rows with computed columns
     */
    @Override
    public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
        SpelTransformVO spelTransform = (SpelTransformVO) transform;
        RowSchema inputSchema = context.getSchemas().get(INPUT_TABLE);
        List<Row> inputRows = context.getData().get(INPUT_TABLE);
        if (inputSchema == null || inputRows == null) {
            throw new IllegalArgumentException("SpEL transform requires table '" + INPUT_TABLE + "' in execution context");
        }

        List<ParsedMapping> mappings = parseMappings(spelTransform.getColumns());
        RowSchema outputSchema = mergeSchema(inputSchema, mappings);
        List<Row> outputRows = new ArrayList<>(inputRows.size());
        for (Row inputRow : inputRows) {
            outputRows.add(applyMappings(inputRow, mappings));
        }
        return new CalciteRowTransformer.TransformResult(outputSchema, outputRows);
    }

    private static List<ParsedMapping> parseMappings(List<SpelColumnMapping> columns) {
        List<ParsedMapping> parsed = new ArrayList<>(columns.size());
        for (SpelColumnMapping column : columns) {
            // Parse once per transform; expressions are evaluated per row.
            Expression expression = PARSER.parseExpression(column.getExpression());
            parsed.add(new ParsedMapping(column.getName(), expression));
        }
        return parsed;
    }

    private static RowSchema mergeSchema(RowSchema inputSchema, List<ParsedMapping> mappings) {
        Map<String, ColumnDef> columns = new LinkedHashMap<>();
        if (inputSchema.getColumns() != null) {
            for (ColumnDef column : inputSchema.getColumns()) {
                columns.put(column.getName().toLowerCase(Locale.ROOT), column);
            }
        }
        for (ParsedMapping mapping : mappings) {
            String name = mapping.name();
            columns.put(name.toLowerCase(Locale.ROOT), new ColumnDef(name, "ANY", true));
        }
        RowSchema schema = new RowSchema();
        schema.setColumns(List.copyOf(columns.values()));
        return schema;
    }

    private static Row applyMappings(Row inputRow, List<ParsedMapping> mappings) {
        Map<String, Object> values = new LinkedHashMap<>(inputRow.values());
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        evaluationContext.addPropertyAccessor(new MapAccessor());
        evaluationContext.setVariable(ROW_VARIABLE, values);
        evaluationContext.setVariable(Const.SCRIPT_VAR_FAKER, FAKER.get());
        for (ParsedMapping mapping : mappings) {
            Object value = mapping.expression().getValue(evaluationContext);
            // Lowercase keys match QueryRowSourceSupport and migration #row['col'] references.
            values.put(mapping.name().toLowerCase(Locale.ROOT), value);
        }
        return new Row(values);
    }

    private record ParsedMapping(String name, Expression expression) {
    }
}
