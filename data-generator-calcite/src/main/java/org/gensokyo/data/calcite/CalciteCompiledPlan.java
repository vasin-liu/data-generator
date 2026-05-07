package org.gensokyo.data.calcite;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;

@Getter
@RequiredArgsConstructor
public class CalciteCompiledPlan {
    private final String sql;
    private final SqlSelect select;
    private final SqlNodeList orderBy;
    private final SqlNode offset;
    private final SqlNode fetch;
}
