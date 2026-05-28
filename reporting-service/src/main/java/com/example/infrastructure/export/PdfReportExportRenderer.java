package com.example.infrastructure.export;

import com.example.application.port.out.ReportExportRenderer;
import com.example.domain.model.PlatformComparisonPoint;
import com.example.domain.model.ReportEntry;
import com.example.domain.model.ReportExportData;
import com.example.domain.model.ReportExportFormat;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class PdfReportExportRenderer implements ReportExportRenderer {
    private static final int MAX_LINES_PER_PAGE = 49;

    @Override
    public ReportExportFormat format() {
        return ReportExportFormat.PDF;
    }

    @Override
    public byte[] render(ReportExportData data) {
        List<PdfLine> lines = new ArrayList<>();
        lines.add(new PdfLine("F2", 16, data.title()));
        lines.add(new PdfLine("F1", 10, "Tenant: " + data.tenantId()));
        lines.add(new PdfLine("F1", 10, "Periodo: " + data.periodLabel()));
        lines.add(new PdfLine("F1", 10, "Gerado em: " + data.generatedAt()));
        lines.add(PdfLine.blank());
        lines.add(new PdfLine("F2", 12, "Resumo financeiro"));
        lines.add(new PdfLine("F1", 10, "Faturado: " + money(data.summary().grossValue())));
        lines.add(new PdfLine("F1", 10, "Recebido: " + money(data.summary().receivedValue())));
        lines.add(new PdfLine("F1", 10, "Taxas: " + money(data.summary().feeValue())));
        lines.add(new PdfLine("F1", 10, "A receber: " + money(data.summary().receivableValue())));
        lines.add(new PdfLine("F1", 10, "Lancamentos: " + data.summary().entryCount()));
        lines.add(PdfLine.blank());
        lines.add(new PdfLine("F2", 12, "Resumo por marketplace"));
        lines.add(new PdfLine("F3", 9, fixed("Marketplace", 18) + fixed("Faturado", 12) + fixed("Recebido", 12)
                + fixed("Taxas", 12) + fixed("A receber", 12) + "Qtde"));
        for (PlatformComparisonPoint point : data.platformSummaries()) {
            lines.add(new PdfLine("F3", 9, fixed(point.platform(), 18) + fixed(money(point.grossValue()), 12)
                    + fixed(money(point.receivedValue()), 12) + fixed(money(point.feeValue()), 12)
                    + fixed(money(point.receivableValue()), 12) + point.entryCount()));
        }
        lines.add(PdfLine.blank());
        lines.add(new PdfLine("F2", 12, "Lancamentos"));
        lines.add(new PdfLine("F3", 8, fixed("Data", 11) + fixed("Marketplace", 16) + fixed("Pedido", 15)
                + fixed("Bruto", 11) + fixed("Recebido", 11) + fixed("Taxa", 10)
                + fixed("A receber", 11) + "Status"));
        if (data.entries().isEmpty()) {
            lines.add(new PdfLine("F1", 10, "Sem lancamentos no periodo."));
        }
        for (ReportEntry entry : data.entries()) {
            lines.add(new PdfLine("F3", 8, fixed(entry.saleDate(), 11)
                    + fixed(entry.platform(), 16)
                    + fixed(entry.orderId(), 15)
                    + fixed(money(entry.grossValue()), 11)
                    + fixed(money(entry.receivedValue()), 11)
                    + fixed(money(entry.feeValue()), 10)
                    + fixed(money(entry.receivableValue()), 11)
                    + entry.status()));
            String detail = "Comprador: " + blank(entry.buyerName()) + " | NF: " + blank(entry.invoiceNumber())
                    + " | Liberacao: " + blank(entry.releaseDate());
            lines.add(new PdfLine("F3", 8, "  " + detail));
        }
        return pdf(paginate(lines));
    }

    private List<List<PdfLine>> paginate(List<PdfLine> lines) {
        List<List<PdfLine>> pages = new ArrayList<>();
        List<PdfLine> page = new ArrayList<>();
        for (PdfLine line : lines) {
            if (page.size() >= MAX_LINES_PER_PAGE) {
                pages.add(page);
                page = new ArrayList<>();
            }
            page.add(line);
        }
        if (!page.isEmpty()) {
            pages.add(page);
        }
        return pages;
    }

    private byte[] pdf(List<List<PdfLine>> pages) {
        List<String> objects = new ArrayList<>();
        StringBuilder kids = new StringBuilder();
        int catalogObject = 1;
        int pagesObject = 2;
        int regularFontObject = 3;
        int boldFontObject = 4;
        int monoFontObject = 5;
        int nextObject = 6;

        objects.add("<< /Type /Catalog /Pages " + pagesObject + " 0 R >>");
        objects.add("");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>");

        for (List<PdfLine> page : pages) {
            int contentObject = nextObject++;
            int pageObject = nextObject++;
            kids.append(pageObject).append(" 0 R ");
            String content = pageContent(page);
            objects.add("<< /Length " + content.getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n"
                    + content + "endstream");
            objects.add("<< /Type /Page /Parent " + pagesObject + " 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 " + regularFontObject + " 0 R /F2 " + boldFontObject
                    + " 0 R /F3 " + monoFontObject + " 0 R >> >> /Contents " + contentObject + " 0 R >>");
        }

        objects.set(pagesObject - 1, "<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
            pdf.append(index + 1).append(" 0 obj\n")
                    .append(objects.get(index))
                    .append("\nendobj\n");
        }
        int xrefOffset = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n")
                .append("0000000000 65535 f \n");
        for (int offset : offsets) {
            pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1)
                .append(" /Root ").append(catalogObject).append(" 0 R >>\n")
                .append("startxref\n").append(xrefOffset).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private String pageContent(List<PdfLine> lines) {
        StringBuilder content = new StringBuilder();
        int y = 802;
        for (PdfLine line : lines) {
            if (!line.text().isBlank()) {
                content.append("BT /")
                        .append(line.font())
                        .append(' ')
                        .append(line.size())
                        .append(" Tf 40 ")
                        .append(y)
                        .append(" Td (")
                        .append(escape(line.text()))
                        .append(") Tj ET\n");
            }
            y -= line.size() >= 12 ? 17 : 12;
        }
        return content.toString();
    }

    private String escape(String value) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\x20-\\x7E]", "?");
        return ascii
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String fixed(Object value, int size) {
        String text = blank(value);
        if (text.length() > size - 1) {
            text = text.substring(0, size - 1);
        }
        return String.format(Locale.ROOT, "%-" + size + "s", text);
    }

    private String blank(Object value) {
        return value == null ? "" : value.toString();
    }

    private String money(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return safeValue.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private record PdfLine(String font, int size, String text) {
        static PdfLine blank() {
            return new PdfLine("F1", 10, "");
        }
    }
}
