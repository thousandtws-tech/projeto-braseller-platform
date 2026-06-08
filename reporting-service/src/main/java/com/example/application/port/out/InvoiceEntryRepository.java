package com.example.application.port.out;

import com.example.domain.model.InvoiceEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceEntryRepository {
    void upsert(InvoiceEntry entry);
    Optional<InvoiceEntry> findByOrderId(String tenantId, String platform, String orderId);
    List<InvoiceEntry> findByPeriod(String tenantId, LocalDate from, LocalDate to);
    List<InvoiceEntry> findUnmatched(String tenantId, LocalDate from, LocalDate to);
}
