package com.example.application.command;

public record UpdateNotificationPreferenceCommand(
        String tenantId,
        Boolean emailEnabled,
        Boolean newSaleEnabled,
        Boolean monthlyClosingEnabled,
        Boolean mlPaymentReleaseEnabled,
        Boolean weeklyAccountantReportEnabled,
        String recipientEmail,
        String accountantEmail) {
}
