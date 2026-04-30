package org.gensokyo.data.model.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class ColumnDef implements Serializable {
    private final String name;
    private final String logicalType;
    private final boolean nullable;
}
