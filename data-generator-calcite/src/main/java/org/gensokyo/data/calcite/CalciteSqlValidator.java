package org.gensokyo.data.calcite;

import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.plan.Contexts;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlLibrary;
import org.apache.calcite.sql.fun.SqlLibraryOperatorTableFactory;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.babel.SqlBabelParserImpl;
import org.apache.calcite.sql.util.SqlOperatorTables;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql2rel.StandardConvertletTable;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.List;
import java.util.Properties;

public class CalciteSqlValidator {
    private static final SqlConformance SQL_CONFORMANCE = SqlConformanceEnum.BABEL;
    private final SqlOperatorTable operatorTable;

    public CalciteSqlValidator() {
        this(TemplateV2SqlFunctionRegistry.builtIn());
    }

    public CalciteSqlValidator(TemplateV2SqlFunctionRegistry sqlFunctionRegistry) {
        this.operatorTable = SqlOperatorTables.chain(
                sqlFunctionRegistry.operatorTable(),
                SqlLibraryOperatorTableFactory.INSTANCE.getOperatorTable(SqlLibrary.STANDARD),
                SqlLibraryOperatorTableFactory.INSTANCE.getOperatorTable(SqlLibrary.CALCITE),
                SqlLibraryOperatorTableFactory.INSTANCE.getOperatorTable(SqlLibrary.MYSQL)
        );
    }

    public CalciteSqlValidationResult validate(String sql, CalciteExecutionContext context) {
        try {
            FrameworkConfig config = newConfig(context);
            SqlParser parser = SqlParser.create(sql, config.getParserConfig());
            SqlNode parsed = parser.parseQuery();
            validator(config, context).validate(parsed);
            return CalciteSqlValidationResult.success(parsed);
        } catch (SqlParseException e) {
            return CalciteSqlValidationResult.failure(e.getMessage());
        } catch (RuntimeException e) {
            return CalciteSqlValidationResult.failure(e.getMessage());
        }
    }

    private FrameworkConfig newConfig(CalciteExecutionContext context) {
        CalciteSchema root = CalciteSchema.createRootSchema(false, false);
        RelDataTypeFactory typeFactory = CalciteSchemaFactory.typeFactory();
        context.getSchemas().forEach((tableName, schema) -> root.add(tableName, new StaticRowTable(schema, typeFactory)));
        return Frameworks.newConfigBuilder()
                .defaultSchema(root.plus())
                .operatorTable(operatorTable)
                .parserConfig(SqlParser.config()
                        .withParserFactory(SqlBabelParserImpl.FACTORY)
                        .withQuotedCasing(Casing.UNCHANGED)
                        .withUnquotedCasing(Casing.UNCHANGED)
                        .withCaseSensitive(false)
                        .withConformance(SQL_CONFORMANCE))
                .context(Contexts.EMPTY_CONTEXT)
                .build();
    }

    private SqlValidator validator(FrameworkConfig config, CalciteExecutionContext context) {
        CalciteSchema root = CalciteSchema.from(config.getDefaultSchema());
        RelDataTypeFactory typeFactory = CalciteSchemaFactory.typeFactory();
        Properties properties = new Properties();
        properties.setProperty("quotedCasing", Casing.UNCHANGED.name());
        properties.setProperty("unquotedCasing", Casing.UNCHANGED.name());
        properties.setProperty("caseSensitive", Boolean.FALSE.toString());
        CalciteCatalogReader catalogReader = new CalciteCatalogReader(
                root,
                List.of(),
                typeFactory,
                new CalciteConnectionConfigImpl(properties)
        );
        return SqlValidatorUtil.newValidator(
                operatorTable,
                catalogReader,
                typeFactory,
                SqlValidator.Config.DEFAULT.withSqlConformance(SQL_CONFORMANCE)
        );
    }

    private static final class StaticRowTable extends AbstractTable {
        private final RowSchema schema;
        private final RelDataTypeFactory typeFactory;

        private StaticRowTable(RowSchema schema, RelDataTypeFactory typeFactory) {
            this.schema = schema;
            this.typeFactory = typeFactory;
        }

        @Override
        public org.apache.calcite.rel.type.RelDataType getRowType(RelDataTypeFactory ignored) {
            return CalciteSchemaFactory.toRelType(schema, typeFactory);
        }
    }
}
