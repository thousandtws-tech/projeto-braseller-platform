package com.example.application.service;

import com.example.application.exception.ValidationException;
import com.example.domain.model.PurchaseEntry;
import com.example.domain.model.PurchaseEntryItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class NfeXmlParserService {
    private static final DateTimeFormatter NF_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PurchaseEntry parse(String tenantId, String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Disable external entity processing (XXE protection)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            String nfeNumber = text(doc, "nNF");
            String serie = text(doc, "serie");
            String fullNumber = serie.isBlank() ? nfeNumber : serie + "-" + nfeNumber;

            String emitName = text(doc, "xNome");
            String rawDate = text(doc, "dhEmi");
            if (rawDate.isBlank()) rawDate = text(doc, "dEmi");
            LocalDate issueDate = parseDate(rawDate);

            List<PurchaseEntryItem> items = new ArrayList<>();
            BigDecimal totalCost = BigDecimal.ZERO;

            NodeList dets = doc.getElementsByTagNameNS("*", "det");
            if (dets.getLength() == 0) {
                dets = doc.getElementsByTagName("det");
            }

            for (int i = 0; i < dets.getLength(); i++) {
                org.w3c.dom.Element det = (org.w3c.dom.Element) dets.item(i);
                String sku = firstText(det, "cProd", "cEAN");
                String description = firstText(det, "xProd");
                BigDecimal qty = decimal(firstText(det, "qCom", "qTrib"));
                BigDecimal unitValue = decimal(firstText(det, "vUnCom", "vUnTrib"));
                BigDecimal total = decimal(firstText(det, "vProd"));
                if (!isPositive(total)) total = qty.multiply(unitValue).setScale(2, RoundingMode.HALF_UP);

                items.add(new PurchaseEntryItem(UUID.randomUUID().toString(), null, sku, description, qty, unitValue, total));
                totalCost = totalCost.add(total);
            }

            // Fallback: use vNF (total NF value) if no items parsed
            if (items.isEmpty()) {
                String vNf = text(doc, "vNF");
                if (!vNf.isBlank()) totalCost = decimal(vNf);
            }

            return new PurchaseEntry(UUID.randomUUID().toString(), tenantId, fullNumber, emitName,
                    issueDate, totalCost.setScale(2, RoundingMode.HALF_UP), items, null);

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("nfe_xml_invalid: " + e.getMessage());
        }
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return LocalDate.now();
        String date = raw.length() >= 10 ? raw.substring(0, 10) : raw;
        try { return LocalDate.parse(date, NF_DATE_FMT); }
        catch (Exception e) { return LocalDate.now(); }
    }

    private String text(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", tagName);
        if (nodes.getLength() == 0) nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0 && nodes.item(0) != null) {
            String v = nodes.item(0).getTextContent();
            return v == null ? "" : v.trim();
        }
        return "";
    }

    private String firstText(org.w3c.dom.Element el, String... tags) {
        for (String tag : tags) {
            NodeList nodes = el.getElementsByTagNameNS("*", tag);
            if (nodes.getLength() == 0) nodes = el.getElementsByTagName(tag);
            if (nodes.getLength() > 0 && nodes.item(0) != null) {
                String v = nodes.item(0).getTextContent();
                if (v != null && !v.trim().isEmpty()) return v.trim();
            }
        }
        return "";
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(value.replace(",", ".")); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
