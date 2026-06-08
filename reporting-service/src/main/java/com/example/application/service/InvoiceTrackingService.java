package com.example.application.service;

import com.example.application.port.out.InvoiceEntryRepository;
import com.example.domain.model.InvoiceEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class InvoiceTrackingService {

    @Inject
    InvoiceEntryRepository invoiceEntryRepository;

    public void recordInvoice(String tenantId, String platform, String orderId,
                              String invoiceNumber, String accessKey, LocalDate issuedAt) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            return;
        }
        InvoiceEntry entry = new InvoiceEntry(
                UUID.randomUUID().toString(),
                tenantId,
                platform,
                orderId,
                invoiceNumber,
                accessKey != null ? accessKey : "",
                issuedAt,
                "issued",
                LocalDate.now()
        );
        invoiceEntryRepository.upsert(entry);
    }

    public List<InvoiceEntry> listByPeriod(String tenantId, LocalDate from, LocalDate to) {
        return invoiceEntryRepository.findByPeriod(tenantId, from, to);
    }

    public List<InvoiceEntry> listUnmatched(String tenantId, LocalDate from, LocalDate to) {
        return invoiceEntryRepository.findUnmatched(tenantId, from, to);
    }
}
