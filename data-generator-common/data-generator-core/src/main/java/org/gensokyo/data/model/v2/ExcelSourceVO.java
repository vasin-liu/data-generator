package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AutoService(SourceVO.class)
@JsonSubType("EXCEL")
public class ExcelSourceVO extends SourceVO {
    public ExcelSourceVO() {
        setType("excel");
    }

    private String path;
    private List<ExcelSheetSourceVO> sheets = defaultSheets();
    private Long maxRows;
    private RowSchema schema;

    private static List<ExcelSheetSourceVO> defaultSheets() {
        List<ExcelSheetSourceVO> sheets = new ArrayList<>();
        sheets.add(new ExcelSheetSourceVO("Sheet1"));
        return sheets;
    }
}
