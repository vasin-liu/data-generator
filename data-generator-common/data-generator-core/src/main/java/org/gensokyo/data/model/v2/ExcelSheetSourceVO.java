package org.gensokyo.data.model.v2;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class ExcelSheetSourceVO implements Serializable {
    private String name = "Sheet1";
    private List<List<String>> headers = new ArrayList<>();
    private int startRow = 1;
    private int endRow = Integer.MAX_VALUE;

    public ExcelSheetSourceVO() {
    }

    public ExcelSheetSourceVO(String name) {
        this.name = name;
    }
}
