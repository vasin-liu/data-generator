package org.gensokyo.data.calcite;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.gensokyo.data.model.v2.ExcelSheetSourceVO;
import org.gensokyo.data.model.v2.ExcelSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ExcelRowSourceTests {

    @Test
    void readsConfiguredSheetsWithRowWindowAndGlobalLimit() throws Exception {
        Path excel = Files.createTempFile("template-v2-excel-source", ".xlsx");
        writeSheet(excel, "Sheet1", List.of(
                row("name", "score"),
                row("alpha", "10"),
                row("beta", "20")
        ));
        writeSheet(excel, "Sheet2", List.of(
                row("name", "score"),
                row("gamma", "30"),
                row("delta", "40")
        ));

        ExcelSheetSourceVO first = new ExcelSheetSourceVO("Sheet1");
        first.setStartRow(1);
        first.setEndRow(3);
        ExcelSheetSourceVO second = new ExcelSheetSourceVO("Sheet2");
        second.setStartRow(1);
        second.setEndRow(3);

        ExcelSourceVO source = new ExcelSourceVO();
        source.setPath(excel.toString());
        source.setSheets(List.of(first, second));
        source.setMaxRows(3L);

        ExcelRowSource rowSource = new ExcelRowSource("people", source);
        List<Row> rows = rowSource.rows();

        Assertions.assertEquals(List.of("name", "score"),
                rowSource.schema().getColumns().stream().map(column -> column.getName()).toList());
        Assertions.assertEquals(3, rows.size());
        Assertions.assertEquals("alpha", rows.get(0).getString("name"));
        Assertions.assertEquals("20", rows.get(1).getString("score"));
        Assertions.assertEquals("gamma", rows.get(2).getString("name"));
    }

    @Test
    void usesExplicitHeadersWhenConfigured() throws Exception {
        Path excel = Files.createTempFile("template-v2-excel-source-explicit-header", ".xlsx");
        writeSheet(excel, "Sheet1", List.of(
                row("skip", "skip"),
                row("alpha", "10")
        ));

        ExcelSheetSourceVO sheet = new ExcelSheetSourceVO("Sheet1");
        sheet.setStartRow(1);
        sheet.setEndRow(2);
        sheet.setHeaders(List.of(List.of("name"), List.of("score")));

        ExcelSourceVO source = new ExcelSourceVO();
        source.setPath(excel.toString());
        source.setSheets(List.of(sheet));

        ExcelRowSource rowSource = new ExcelRowSource("people", source);
        List<Row> rows = rowSource.rows();

        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals("alpha", rows.getFirst().getString("name"));
        Assertions.assertEquals("10", rows.getFirst().getString("score"));
    }

    private void writeSheet(Path excel, String sheetName, List<Map<String, String>> rows) throws Exception {
        XSSFWorkbook workbook;
        if (Files.exists(excel) && Files.size(excel) > 0) {
            try (var input = Files.newInputStream(excel)) {
                workbook = new XSSFWorkbook(input);
            }
        } else {
            workbook = new XSSFWorkbook();
        }
        try (workbook) {
            var existing = workbook.getSheet(sheetName);
            if (existing != null) {
                workbook.removeSheetAt(workbook.getSheetIndex(existing));
            }
            var sheet = workbook.createSheet(sheetName);
            for (int i = 0; i < rows.size(); i++) {
                var row = sheet.createRow(i);
                int cellIndex = 0;
                for (String value : rows.get(i).values()) {
                    row.createCell(cellIndex++).setCellValue(value);
                }
            }
            try (OutputStream output = Files.newOutputStream(excel)) {
                workbook.write(output);
            }
        }
    }

    private Map<String, String> row(String first, String second) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("c1", first);
        row.put("c2", second);
        return row;
    }
}
