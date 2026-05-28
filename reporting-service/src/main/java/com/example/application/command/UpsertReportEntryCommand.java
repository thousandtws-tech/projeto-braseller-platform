package com.example.application.command;

import com.example.domain.model.PaymentMethod;
import com.example.domain.model.ReportEntryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertReportEntryCommand(
        String tenantId,
        String platform,
        String orderId,
        LocalDate saleDate,
        BigDecimal grossValue,
        BigDecimal receivedValue,
        BigDecimal feeValue,
        BigDecimal receivableValue,
        PaymentMethod paymentMethod,
        ReportEntryStatus status,
        LocalDate releaseDate,
        String buyerName,
        String invoiceNumber) {
}
