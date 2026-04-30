package org.gensokyo.data.calcite;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.calcite.sql.SqlNode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CalciteSqlValidationResult {
    private final boolean valid;
    private final String message;
    private final SqlNode sqlNode;

    public static CalciteSqlValidationResult success(SqlNode sqlNode) {
        return new CalciteSqlValidationResult(true, null, sqlNode);
    }

    public static CalciteSqlValidationResult failure(String message) {
        return new CalciteSqlValidationResult(false, message, null);
    }
}
