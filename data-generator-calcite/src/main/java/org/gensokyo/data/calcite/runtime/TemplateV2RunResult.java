package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class TemplateV2RunResult {
    private final RowSchema schema;
    private final List<Row> rows;
}
