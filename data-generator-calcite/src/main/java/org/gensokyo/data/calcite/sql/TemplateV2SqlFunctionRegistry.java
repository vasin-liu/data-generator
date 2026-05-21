package org.gensokyo.data.calcite.sql;

import org.gensokyo.data.calcite.*;

import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlOperandCountRanges;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.sql.util.SqlOperatorTables;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TemplateV2SqlFunctionRegistry {
    private final Map<String, TemplateV2SqlFunction> functions;

    public TemplateV2SqlFunctionRegistry(List<TemplateV2SqlFunction> functions) {
        Map<String, TemplateV2SqlFunction> indexed = new LinkedHashMap<>();
        for (TemplateV2SqlFunction function : functions) {
            indexed.put(normalize(function.name()), function);
        }
        this.functions = Map.copyOf(indexed);
    }

    public static TemplateV2SqlFunctionRegistry builtIn() {
        return new TemplateV2SqlFunctionRegistry(List.of(
                new TemplateV2SqlFunction("V2_TO_DATE", ReturnTypes.DATE_NULLABLE, OperandTypes.ANY,
                        context -> context.dateArgument(0)),
                new TemplateV2SqlFunction("V2_FORMAT_DATE", ReturnTypes.VARCHAR_NULLABLE, OperandTypes.ANY_ANY,
                        context -> context.dateArgument(1).format(DateTimeFormatter.ofPattern(
                                TemplateV2SqlFunctionContext.toJavaDatePattern(context.stringArgument(0))))),
                new TemplateV2SqlFunction("V2_DATE_ADD", ReturnTypes.DATE_NULLABLE, OperandTypes.ANY_ANY,
                        context -> context.dateArgument(0).plusDays(context.intArgument(1))),
                new TemplateV2SqlFunction("V2_DATE_SUB", ReturnTypes.DATE_NULLABLE, OperandTypes.ANY_ANY,
                        context -> context.dateArgument(0).minusDays(context.intArgument(1))),
                new TemplateV2SqlFunction("V2_DATE_DIFF", ReturnTypes.BIGINT_NULLABLE, OperandTypes.ANY_ANY,
                        context -> ChronoUnit.DAYS.between(context.dateArgument(1), context.dateArgument(0))),
                new TemplateV2SqlFunction("FAKER_SNOWFLAKE", ReturnTypes.BIGINT_NULLABLE, OperandTypes.NILADIC,
                        context -> TemplateV2FakerFunctions.snowflake()),
                new TemplateV2SqlFunction("FAKER_TEXT", ReturnTypes.VARCHAR_NULLABLE, OperandTypes.ANY_ANY,
                        context -> TemplateV2FakerFunctions.text(context.intArgument(0), context.intArgument(1))),
                new TemplateV2SqlFunction("FAKER_NUMBER_BETWEEN", ReturnTypes.INTEGER_NULLABLE, OperandTypes.ANY_ANY,
                        context -> TemplateV2FakerFunctions.numberBetween(context.intArgument(0), context.intArgument(1))),
                new TemplateV2SqlFunction("FAKER_PHONE_CELL", ReturnTypes.VARCHAR_NULLABLE, OperandTypes.NILADIC,
                        context -> TemplateV2FakerFunctions.phoneCell()),
                new TemplateV2SqlFunction("FAKER_DATE_PAST", ReturnTypes.VARCHAR_NULLABLE, OperandTypes.ANY_ANY,
                        context -> TemplateV2FakerFunctions.datePast(context.intArgument(0), context.stringArgument(1))),
                new TemplateV2SqlFunction("FAKER_DATETIME_NOW", ReturnTypes.VARCHAR_NULLABLE, OperandTypes.NILADIC,
                        context -> TemplateV2FakerFunctions.datetimeNow()),
                new TemplateV2SqlFunction("FAKER_DATETIME_SECONDS", ReturnTypes.BIGINT_NULLABLE, OperandTypes.NILADIC,
                        context -> TemplateV2FakerFunctions.datetimeSeconds()),
                new TemplateV2SqlFunction("FAKER_DATETIME_MINUS_DAYS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.NUMERIC,
                                OperandTypes.ANY_ANY
                        ),
                        context -> context.arguments().size() == 1
                                ? TemplateV2FakerFunctions.datetimeMinusDays(context.intArgument(0))
                                : TemplateV2FakerFunctions.datetimeMinusDays(context.argument(0), context.intArgument(1))),
                new TemplateV2SqlFunction("FAKER_DATETIME_MINUS_HOURS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.NUMERIC,
                                OperandTypes.ANY_ANY
                        ),
                        context -> context.arguments().size() == 1
                                ? TemplateV2FakerFunctions.datetimeMinusHours(context.intArgument(0))
                                : TemplateV2FakerFunctions.datetimeMinusHours(context.argument(0), context.intArgument(1))),
                new TemplateV2SqlFunction("FAKER_DATETIME_MINUS_MINUTES", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.NUMERIC,
                                OperandTypes.ANY_ANY
                        ),
                        context -> context.arguments().size() == 1
                                ? TemplateV2FakerFunctions.datetimeMinusMinutes(context.intArgument(0))
                                : TemplateV2FakerFunctions.datetimeMinusMinutes(context.argument(0), context.intArgument(1))),
                new TemplateV2SqlFunction("FAKER_DATETIME_MINUS_SECONDS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.NUMERIC,
                                OperandTypes.ANY_ANY
                        ),
                        context -> context.arguments().size() == 1
                                ? TemplateV2FakerFunctions.datetimeMinusSeconds(context.intArgument(0))
                                : TemplateV2FakerFunctions.datetimeMinusSeconds(context.argument(0), context.intArgument(1))),
                new TemplateV2SqlFunction("FAKER_DATETIME_PLUS_DAYS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.NUMERIC,
                                OperandTypes.ANY_ANY
                        ),
                        context -> context.arguments().size() == 1
                                ? TemplateV2FakerFunctions.datetimePlusDays(context.intArgument(0))
                                : TemplateV2FakerFunctions.datetimePlusDays(context.argument(0), context.intArgument(1))),
                new TemplateV2SqlFunction("FAKER_DATETIME_PLUS_HOURS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.NUMERIC,
                                OperandTypes.ANY_ANY
                        ),
                        context -> context.arguments().size() == 1
                                ? TemplateV2FakerFunctions.datetimePlusHours(context.intArgument(0))
                                : TemplateV2FakerFunctions.datetimePlusHours(context.argument(0), context.intArgument(1))),
                new TemplateV2SqlFunction("FAKER_DATETIME_PLUS_MINUTES", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.NUMERIC,
                                OperandTypes.ANY_ANY
                        ),
                        context -> context.arguments().size() == 1
                                ? TemplateV2FakerFunctions.datetimePlusMinutes(context.intArgument(0))
                                : TemplateV2FakerFunctions.datetimePlusMinutes(context.argument(0), context.intArgument(1))),
                new TemplateV2SqlFunction("FAKER_DATETIME_PLUS_SECONDS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.NUMERIC,
                                OperandTypes.ANY_ANY
                        ),
                        context -> context.arguments().size() == 1
                                ? TemplateV2FakerFunctions.datetimePlusSeconds(context.intArgument(0))
                                : TemplateV2FakerFunctions.datetimePlusSeconds(context.argument(0), context.intArgument(1))),
                new TemplateV2SqlFunction("FAKER_DATETIME_FORMAT", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.ANY,
                                OperandTypes.ANY_ANY
                        ),
                        context -> context.arguments().size() == 1
                                ? TemplateV2FakerFunctions.datetimeFormat(context.argument(0))
                                : TemplateV2FakerFunctions.datetimeFormat(context.argument(0), context.stringArgument(1))),
                new TemplateV2SqlFunction("FAKER_DATETIME_PARSE", ReturnTypes.VARCHAR_NULLABLE, OperandTypes.ANY,
                        context -> TemplateV2FakerFunctions.datetimeParse(context.argument(0))),
                new TemplateV2SqlFunction("FAKER_DATETIME_BEFORE_DAYS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 2
                                ),
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.ANY, SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 3
                                )
                        ),
                        context -> context.arguments().size() == 2
                                ? TemplateV2FakerFunctions.datetimeBeforeDays(context.intArgument(0), context.intArgument(1))
                                : TemplateV2FakerFunctions.datetimeBeforeDays(context.argument(0), context.intArgument(1), context.intArgument(2))),
                new TemplateV2SqlFunction("FAKER_DATETIME_BEFORE_HOURS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 2
                                ),
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.ANY, SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 3
                                )
                        ),
                        context -> context.arguments().size() == 2
                                ? TemplateV2FakerFunctions.datetimeBeforeHours(context.intArgument(0), context.intArgument(1))
                                : TemplateV2FakerFunctions.datetimeBeforeHours(context.argument(0), context.intArgument(1), context.intArgument(2))),
                new TemplateV2SqlFunction("FAKER_DATETIME_BEFORE_MINUTES", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 2
                                ),
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.ANY, SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 3
                                )
                        ),
                        context -> context.arguments().size() == 2
                                ? TemplateV2FakerFunctions.datetimeBeforeMinutes(context.intArgument(0), context.intArgument(1))
                                : TemplateV2FakerFunctions.datetimeBeforeMinutes(context.argument(0), context.intArgument(1), context.intArgument(2))),
                new TemplateV2SqlFunction("FAKER_DATETIME_BEFORE_SECONDS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 2
                                ),
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.ANY, SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 3
                                )
                        ),
                        context -> context.arguments().size() == 2
                                ? TemplateV2FakerFunctions.datetimeBeforeSeconds(context.intArgument(0), context.intArgument(1))
                                : TemplateV2FakerFunctions.datetimeBeforeSeconds(context.argument(0), context.intArgument(1), context.intArgument(2))),
                new TemplateV2SqlFunction("FAKER_DATETIME_AFTER_DAYS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 2
                                ),
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.ANY, SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 3
                                )
                        ),
                        context -> context.arguments().size() == 2
                                ? TemplateV2FakerFunctions.datetimeAfterDays(context.intArgument(0), context.intArgument(1))
                                : TemplateV2FakerFunctions.datetimeAfterDays(context.argument(0), context.intArgument(1), context.intArgument(2))),
                new TemplateV2SqlFunction("FAKER_DATETIME_AFTER_HOURS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 2
                                ),
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.ANY, SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 3
                                )
                        ),
                        context -> context.arguments().size() == 2
                                ? TemplateV2FakerFunctions.datetimeAfterHours(context.intArgument(0), context.intArgument(1))
                                : TemplateV2FakerFunctions.datetimeAfterHours(context.argument(0), context.intArgument(1), context.intArgument(2))),
                new TemplateV2SqlFunction("FAKER_DATETIME_AFTER_MINUTES", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 2
                                ),
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.ANY, SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 3
                                )
                        ),
                        context -> context.arguments().size() == 2
                                ? TemplateV2FakerFunctions.datetimeAfterMinutes(context.intArgument(0), context.intArgument(1))
                                : TemplateV2FakerFunctions.datetimeAfterMinutes(context.argument(0), context.intArgument(1), context.intArgument(2))),
                new TemplateV2SqlFunction("FAKER_DATETIME_AFTER_SECONDS", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.or(
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 2
                                ),
                                OperandTypes.family(
                                        List.of(SqlTypeFamily.ANY, SqlTypeFamily.NUMERIC, SqlTypeFamily.NUMERIC),
                                        count -> count == 3
                                )
                        ),
                        context -> context.arguments().size() == 2
                                ? TemplateV2FakerFunctions.datetimeAfterSeconds(context.intArgument(0), context.intArgument(1))
                                : TemplateV2FakerFunctions.datetimeAfterSeconds(context.argument(0), context.intArgument(1), context.intArgument(2))),
                new TemplateV2SqlFunction("FAKER_VEHICLE_CN_PLATE_PROVINCE", ReturnTypes.VARCHAR_NULLABLE, OperandTypes.ANY,
                        context -> TemplateV2FakerFunctions.vehicleCnPlateProvince(context.stringArgument(0))),
                new TemplateV2SqlFunction("FAKER_SNOWFLAKE_VIID", ReturnTypes.VARCHAR_NULLABLE,
                        OperandTypes.family(
                                java.util.List.of(
                                        SqlTypeFamily.ANY,
                                        SqlTypeFamily.CHARACTER,
                                        SqlTypeFamily.ANY,
                                        SqlTypeFamily.CHARACTER
                                )
                        ),
                        context -> TemplateV2FakerFunctions.snowflakeViid(
                                context.stringArgument(0),
                                context.stringArgument(1),
                                context.argument(2),
                                context.stringArgument(3)
                        )),
                new TemplateV2SqlFunction("V2_GEO_DISTANCE_METERS", ReturnTypes.DOUBLE_NULLABLE,
                        OperandTypes.family(
                                List.of(
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC
                                ),
                                count -> count == 4
                        ),
                        TemplateV2GeoSqlFunctions::distanceMeters),
                new TemplateV2SqlFunction("V2_GEO_WITHIN_RADIUS", ReturnTypes.BOOLEAN_NULLABLE,
                        OperandTypes.family(
                                List.of(
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC
                                ),
                                count -> count == 5
                        ),
                        TemplateV2GeoSqlFunctions::withinRadius),
                new TemplateV2SqlFunction("V2_GEO_WKT_INTERSECTS", ReturnTypes.BOOLEAN_NULLABLE,
                        OperandTypes.family(
                                List.of(SqlTypeFamily.CHARACTER, SqlTypeFamily.CHARACTER),
                                count -> count == 2
                        ),
                        TemplateV2GeoSqlFunctions::wktIntersects),
                new TemplateV2SqlFunction("V2_GEO_WKT_CONTAINS", ReturnTypes.BOOLEAN_NULLABLE,
                        OperandTypes.family(
                                List.of(SqlTypeFamily.CHARACTER, SqlTypeFamily.CHARACTER),
                                count -> count == 2
                        ),
                        TemplateV2GeoSqlFunctions::wktContains),
                new TemplateV2SqlFunction("V2_GEO_POINT_IN_WKT", ReturnTypes.BOOLEAN_NULLABLE,
                        OperandTypes.family(
                                List.of(
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.CHARACTER
                                ),
                                count -> count == 3
                        ),
                        TemplateV2GeoSqlFunctions::pointInWkt),
                new TemplateV2SqlFunction("V2_GEO_GEOJSON_INTERSECTS", ReturnTypes.BOOLEAN_NULLABLE,
                        OperandTypes.family(
                                List.of(SqlTypeFamily.CHARACTER, SqlTypeFamily.CHARACTER),
                                count -> count == 2
                        ),
                        TemplateV2GeoSqlFunctions::geoJsonIntersects),
                new TemplateV2SqlFunction("V2_GEO_GEOJSON_CONTAINS", ReturnTypes.BOOLEAN_NULLABLE,
                        OperandTypes.family(
                                List.of(SqlTypeFamily.CHARACTER, SqlTypeFamily.CHARACTER),
                                count -> count == 2
                        ),
                        TemplateV2GeoSqlFunctions::geoJsonContains),
                new TemplateV2SqlFunction("V2_GEO_POINT_IN_GEOJSON", ReturnTypes.BOOLEAN_NULLABLE,
                        OperandTypes.family(
                                List.of(
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.CHARACTER
                                ),
                                count -> count == 3
                        ),
                        TemplateV2GeoSqlFunctions::pointInGeoJson)
        ));
    }

    public TemplateV2SqlFunctionRegistry with(TemplateV2SqlFunction function) {
        List<TemplateV2SqlFunction> merged = new ArrayList<>(functions.values());
        merged.add(function);
        return new TemplateV2SqlFunctionRegistry(merged);
    }

    public List<TemplateV2SqlFunction> functions() {
        return List.copyOf(functions.values());
    }

    public Optional<TemplateV2SqlFunction> find(String name) {
        return Optional.ofNullable(functions.get(normalize(name)));
    }

    public SqlOperatorTable operatorTable() {
        return SqlOperatorTables.of(functions.values().stream()
                .map(TemplateV2SqlFunction::toSqlFunction)
                .toList());
    }

    public static String normalize(String name) {
        return name.toUpperCase(Locale.ROOT);
    }
}
