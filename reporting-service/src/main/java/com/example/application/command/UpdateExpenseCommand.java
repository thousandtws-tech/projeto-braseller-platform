package com.example.application.command;

import com.example.domain.model.ExpenseAttachment;
import com.example.domain.model.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateExpenseCommand(
        String tenantId,
        String expenseId,
        LocalDate expenseDate,
        ExpenseCategory category,
        String description,
        BigDecimal amount,
        ExpenseAttachment attachment) {
}
