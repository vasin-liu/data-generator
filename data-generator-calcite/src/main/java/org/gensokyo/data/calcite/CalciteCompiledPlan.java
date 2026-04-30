package org.gensokyo.data.calcite;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.calcite.sql.SqlSelect;

@Getter
@RequiredArgsConstructor
public class CalciteCompiledPlan {
    private final String sql;
    private final SqlSelect select;
}
