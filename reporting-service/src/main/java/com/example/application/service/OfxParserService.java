package com.example.application.service;

import com.example.application.exception.ValidationException;
import com.example.domain.model.BankTransaction;
import com.example.domain.model.BankTransactionCategory;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Supports both SGML-style OFX (no closing tags, common in Brazil) and XML-style OFX
@ApplicationScoped
public class OfxParserService {
    private static final Pattern STMTTRN = Pattern.compile("<STMTTRN>([\\s\\S]*?)(?=<STMTTRN>|</BANKTRANLIST>|$)");
    private static final Pattern TAG     = Pattern.compile("<([A-Z]+)>([^<\n\r]+)");
    private static final DateTimeFormatter OFX_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter OFX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<BankTransaction> parse(String tenantId, String ofxContent) {
        if (ofxContent == null || ofxContent.isBlank()) {
            throw new ValidationException("ofx_content_empty");
        }
        String content = ofxContent.trim();
        List<BankTransaction> result = new ArrayList<>();
        Matcher blockMatcher = STMTTRN.matcher(content);
        while (blockMatcher.find()) {
            String block = blockMatcher.group(1);
            String fitId = tag(block, "FITID");
            String type = tag(block, "TRNTYPE");
            String amountStr = tag(block, "TRNAMT");
            String dateStr = tag(block, "DTPOSTED");
            String memo = tag(block, "MEMO");
            if (memo.isBlank()) memo = tag(block, "NAME");

            if (fitId.isBlank() || amountStr.isBlank()) continue;

            BigDecimal amount;
            try { amount = new BigDecimal(amountStr.replace(",", ".")); }
            catch (NumberFormatException e) { continue; }

            LocalDate date = parseDate(dateStr);
            BankTransactionCategory category = categorize(memo, type, amount);

            result.add(new BankTransaction(
                    UUID.randomUUID().toString(),
                    tenantId,
                    fitId,
                    type.isBlank() ? (amount.signum() < 0 ? "DEBIT" : "CREDIT") : type.toUpperCase(),
                    amount.abs(),
                    date,
                    memo,
                    category,
                    null
            ));
        }
        if (result.isEmpty()) {
            throw new ValidationException("ofx_no_transactions_found");
        }
        return result;
    }

    private BankTransactionCategory categorize(String memo, String type, BigDecimal amount) {
        String upper = (memo == null ? "" : memo).toUpperCase(Locale.ROOT);
        if (upper.contains("TARIFA") || upper.contains("ANUIDADE") || upper.contains("MANUTENCAO")
                || upper.contains("MANUTENÇÃO") || upper.contains("PACOTE") || upper.contains("SERVICO BANCARIO")) {
            return BankTransactionCategory.TARIFA_BANCARIA;
        }
        if (upper.contains("JUROS") || upper.contains("ENCARGO") || upper.contains("MULTA")) {
            return BankTransactionCategory.JUROS;
        }
        if (upper.contains("IOF")) {
            return BankTransactionCategory.IOF;
        }
        if (upper.contains("PIX")) {
            return BankTransactionCategory.PIX;
        }
        if (upper.contains("TED") || upper.contains("DOC") || upper.contains("TRANSFERENCIA")) {
            return BankTransactionCategory.TED_DOC;
        }
        return BankTransactionCategory.OUTROS;
    }

    private String tag(String block, String tagName) {
        Matcher m = Pattern.compile("<" + tagName + ">([^<\n\r]+)").matcher(block);
        return m.find() ? m.group(1).trim() : "";
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return LocalDate.now();
        String clean = raw.trim().replaceAll("\\[.*", "").trim();
        try {
            if (clean.length() >= 14) return LocalDate.parse(clean.substring(0, 14), OFX_FMT);
            if (clean.length() >= 8)  return LocalDate.parse(clean.substring(0, 8), OFX_DATE);
        } catch (Exception ignored) {}
        return LocalDate.now();
    }
}
