package org.gensokyo.data.calcite;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ExcelRowSinkAdapterTests {

    @Test
    void writesExcelSheetWithDefaultHeaders() throws Exception {
        Path excel = Files.createTempFile("template-v2-excel-sink", ".xlsx");

        new ExcelRowSinkAdapter(writer(excel, Map.of())).write(schema(), rows());

        try (var input = Files.newInputStream(excel); var workbook = new XSSFWorkbook(input)) {
            var sheet = workbook.getSheet("Sheet1");
            Assertions.assertNotNull(sheet);
            Assertions.assertEquals("name", sheet.getRow(0).getCell(0).getStringCellValue());
            Assertions.assertEquals("score", sheet.getRow(0).getCell(1).getStringCellValue());
            Assertions.assertEquals("alpha", sheet.getRow(1).getCell(0).getStringCellValue());
            Assertions.assertEquals(10, (int) sheet.getRow(1).getCell(1).getNumericCellValue());
            Assertions.assertEquals("beta", sheet.getRow(2).getCell(0).getStringCellValue());
        }
    }

    @Test
    void writesConfiguredSheetNameAndHeaders() throws Exception {
        Path excel = Files.createTempFile("template-v2-excel-sink-custom", ".xlsx");
        WriterVO writer = writer(excel, Map.of(
                "name", "Output",
                "headers", List.of(List.of("User Name"), List.of("User Score"))
        ));

        new ExcelRowSinkAdapter(writer).write(schema(), rows());

        try (var input = Files.newInputStream(excel); var workbook = new XSSFWorkbook(input)) {
            var sheet = workbook.getSheet("Output");
            Assertions.assertNotNull(sheet);
            Assertions.assertEquals("User Name", sheet.getRow(0).getCell(0).getStringCellValue());
            Assertions.assertEquals("User Score", sheet.getRow(0).getCell(1).getStringCellValue());
            Assertions.assertEquals("alpha", sheet.getRow(1).getCell(0).getStringCellValue());
        }
    }

    @Test
    void rejectsHeaderCountMismatch() throws Exception {
        Path excel = Files.createTempFile("template-v2-excel-sink-bad-header", ".xlsx");
        WriterVO writer = writer(excel, Map.of("headers", List.of(List.of("name"))));

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ExcelRowSinkAdapter(writer).write(schema(), rows()));

        Assertions.assertTrue(failure.getMessage().contains("Excel sink header count mismatch"));
        Assertions.assertTrue(failure.getMessage().contains(excel.toString()));
    }

    private WriterVO writer(Path path, Map<String, Object> options) {
        WriterVO writer = new WriterVO();
        writer.setType("EXCEL");
        writer.setTarget(path.toString());
        writer.setOptions(options);
        return writer;
    }

    private RowSchema schema() {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("name", "VARCHAR", true),
                new ColumnDef("score", "BIGINT", true)
        ));
        return schema;
    }

    private List<Row> rows() {
        return List.of(
                row("alpha", 10),
                row("beta", 20)
        );
    }

    private Row row(String name, int score) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", name);
        values.put("score", score);
        return new Row(values);
    }
}
