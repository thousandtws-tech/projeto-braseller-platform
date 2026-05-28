package com.example.infrastructure.export;

import com.example.application.port.out.ReportExportRenderer;
import com.example.domain.model.PlatformComparisonPoint;
import com.example.domain.model.ReportEntry;
import com.example.domain.model.ReportExportData;
import com.example.domain.model.ReportExportFormat;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ApplicationScoped
public class XlsxReportExportRenderer implements ReportExportRenderer {
    @Override
    public ReportExportFormat format() {
        return ReportExportFormat.XLSX;
    }

    @Override
    public byte[] render(ReportExportData data) {
        List<Sheet> sheets = new ArrayList<>();
        sheets.add(summarySheet(data));
        data.entriesByPlatform().forEach((platform, entries) -> sheets.add(entriesSheet(platform, entries)));
        if (data.entries().isEmpty()) {
            sheets.add(entriesSheet("Sem dados", List.of()));
        }
        return workbook(uniqueSheets(sheets));
    }

    private Sheet summarySheet(ReportExportData data) {
        List<Row> rows = new ArrayList<>();
        rows.add(row("Relatorio", data.title()));
        rows.add(row("Tenant", data.tenantId()));
        rows.add(row("Periodo", data.periodLabel()));
        rows.add(row("Gerado em", data.generatedAt().toString()));
        rows.add(row(""));
        rows.add(row("Resumo", ""));
        rows.add(row("Faturado", data.summary().grossValue()));
        rows.add(row("Recebido", data.summary().receivedValue()));
        rows.add(row("Taxas", data.summary().feeValue()));
        rows.add(row("A receber", data.summary().receivableValue()));
        rows.add(row("Lancamentos", data.summary().entryCount()));
        rows.add(row(""));
        rows.add(row("Marketplace", "Faturado", "Recebido", "Taxas", "A receber", "Lancamentos"));
        for (PlatformComparisonPoint point : data.platformSummaries()) {
            rows.add(row(
                    point.platform(),
                    point.grossValue(),
                    point.receivedValue(),
                    point.feeValue(),
                    point.receivableValue(),
                    point.entryCount()
            ));
        }
        return new Sheet("Resumo", rows);
    }

    private Sheet entriesSheet(String platform, List<ReportEntry> entries) {
        List<Row> rows = new ArrayList<>();
        rows.add(row("Data", "Pedido", "Comprador", "Nota fiscal", "Bruto", "Recebido", "Taxa",
                "A receber", "Pagamento", "Status", "Liberacao"));
        for (ReportEntry entry : entries) {
            rows.add(row(
                    entry.saleDate(),
                    entry.orderId(),
                    entry.buyerName(),
                    entry.invoiceNumber(),
                    entry.grossValue(),
                    entry.receivedValue(),
                    entry.feeValue(),
                    entry.receivableValue(),
                    entry.paymentMethod(),
                    entry.status(),
                    entry.releaseDate()
            ));
        }
        if (entries.isEmpty()) {
            rows.add(row("Sem lancamentos no periodo"));
        }
        return new Sheet(platform, rows);
    }

    private byte[] workbook(List<Sheet> sheets) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            write(zip, "[Content_Types].xml", contentTypes(sheets.size()));
            write(zip, "_rels/.rels", rootRelationships());
            write(zip, "xl/workbook.xml", workbookXml(sheets));
            write(zip, "xl/_rels/workbook.xml.rels", workbookRelationships(sheets.size()));
            for (int index = 0; index < sheets.size(); index++) {
                write(zip, "xl/worksheets/sheet" + (index + 1) + ".xml", worksheetXml(sheets.get(index)));
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not render xlsx export", exception);
        }
    }

    private void write(ZipOutputStream zip, String path, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String contentTypes(int sheetCount) {
        StringBuilder xml = new StringBuilder(xmlDeclaration());
        xml.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        for (int index = 1; index <= sheetCount; index++) {
            xml.append("<Override PartName=\"/xl/worksheets/sheet")
                    .append(index)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return xml.append("</Types>").toString();
    }

    private String rootRelationships() {
        return xmlDeclaration()
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private String workbookXml(List<Sheet> sheets) {
        StringBuilder xml = new StringBuilder(xmlDeclaration());
        xml.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int index = 0; index < sheets.size(); index++) {
            xml.append("<sheet name=\"")
                    .append(escape(sheets.get(index).name()))
                    .append("\" sheetId=\"")
                    .append(index + 1)
                    .append("\" r:id=\"rId")
                    .append(index + 1)
                    .append("\"/>");
        }
        return xml.append("</sheets></workbook>").toString();
    }

    private String workbookRelationships(int sheetCount) {
        StringBuilder xml = new StringBuilder(xmlDeclaration());
        xml.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int index = 1; index <= sheetCount; index++) {
            xml.append("<Relationship Id=\"rId")
                    .append(index)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(index)
                    .append(".xml\"/>");
        }
        return xml.append("</Relationships>").toString();
    }

    private String worksheetXml(Sheet sheet) {
        StringBuilder xml = new StringBuilder(xmlDeclaration());
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (int rowIndex = 0; rowIndex < sheet.rows().size(); rowIndex++) {
            Row row = sheet.rows().get(rowIndex);
            int rowNumber = rowIndex + 1;
            xml.append("<row r=\"").append(rowNumber).append("\">");
            for (int cellIndex = 0; cellIndex < row.cells().size(); cellIndex++) {
                Cell cell = row.cells().get(cellIndex);
                String reference = columnName(cellIndex) + rowNumber;
                if (cell.number() != null) {
                    xml.append("<c r=\"")
                            .append(reference)
                            .append("\"><v>")
                            .append(cell.number().toPlainString())
                            .append("</v></c>");
                } else {
                    xml.append("<c r=\"")
                            .append(reference)
                            .append("\" t=\"inlineStr\"><is><t>")
                            .append(escape(cell.text()))
                            .append("</t></is></c>");
                }
            }
            xml.append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private List<Sheet> uniqueSheets(List<Sheet> sheets) {
        Map<String, Integer> names = new LinkedHashMap<>();
        List<Sheet> unique = new ArrayList<>();
        for (Sheet sheet : sheets) {
            String baseName = sheetName(sheet.name());
            int count = names.merge(baseName, 1, Integer::sum);
            String name = count == 1 ? baseName : withSuffix(baseName, count);
            unique.add(new Sheet(name, sheet.rows()));
        }
        return unique;
    }

    private String sheetName(String value) {
        String name = value == null ? "" : value.replaceAll("[\\\\/*?:\\[\\]]", "-").trim();
        if (name.isBlank()) {
            name = "Planilha";
        }
        return name.length() > 31 ? name.substring(0, 31).trim() : name;
    }

    private String withSuffix(String baseName, int count) {
        String suffix = " " + count;
        int maxBaseLength = 31 - suffix.length();
        String prefix = baseName.length() > maxBaseLength ? baseName.substring(0, maxBaseLength).trim() : baseName;
        return prefix + suffix;
    }

    private Row row(Object... values) {
        List<Cell> cells = new ArrayList<>();
        for (Object value : values) {
            cells.add(cell(value));
        }
        return new Row(cells);
    }

    private Cell cell(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return new Cell("", bigDecimal.setScale(2, RoundingMode.HALF_UP));
        }
        if (value instanceof Number number) {
            return new Cell("", BigDecimal.valueOf(number.longValue()));
        }
        if (value instanceof LocalDate localDate) {
            return new Cell(localDate.toString(), null);
        }
        return new Cell(value == null ? "" : value.toString(), null);
    }

    private String columnName(int index) {
        StringBuilder name = new StringBuilder();
        int current = index;
        do {
            name.insert(0, (char) ('A' + (current % 26)));
            current = current / 26 - 1;
        } while (current >= 0);
        return name.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String xmlDeclaration() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    }

    private record Sheet(String name, List<Row> rows) {
    }

    private record Row(List<Cell> cells) {
    }

    private record Cell(String text, BigDecimal number) {
    }
}
