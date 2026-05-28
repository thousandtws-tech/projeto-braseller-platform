package com.example.infrastructure.export;

import com.example.application.port.out.ReportExportRenderer;
import com.example.domain.model.ReportEntry;
import com.example.domain.model.ReportExportData;
import com.example.domain.model.ReportExportFormat;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@ApplicationScoped
public class CsvReportExportRenderer implements ReportExportRenderer {
    @Override
    public ReportExportFormat format() {
        return ReportExportFormat.CSV;
    }

    @Override
    public byte[] render(ReportExportData data) {
        StringBuilder csv = new StringBuilder();
        csv.append("tenant_id,period,platform,order_id,sale_date,gross_value,received_value,fee_value,receivable_value,payment_method,status,release_date,buyer_name,invoice_number\r\n");
        for (ReportEntry entry : data.entries()) {
            append(csv,
                    data.tenantId(),
                    data.periodLabel(),
                    entry.platform(),
                    entry.orderId(),
                    entry.saleDate(),
                    money(entry.grossValue()),
                    money(entry.receivedValue()),
                    money(entry.feeValue()),
                    money(entry.receivableValue()),
                    entry.paymentMethod(),
                    entry.status(),
                    entry.releaseDate(),
                    entry.buyerName(),
                    entry.invoiceNumber());
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void append(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escape(values[index]));
        }
        csv.append("\r\n");
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value instanceof LocalDate localDate ? localDate.toString() : value.toString();
        boolean quote = text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n");
        String escaped = text.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
