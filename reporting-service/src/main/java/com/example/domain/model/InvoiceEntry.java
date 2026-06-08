package com.example.domain.model;

import java.time.LocalDate;

public record InvoiceEntry(
        String id,
        String tenantId,
        String platform,
        String orderId,
        String invoiceNumber,
        String accessKey,
        LocalDate issuedAt,
        String status,
        LocalDate createdAt) {
}
