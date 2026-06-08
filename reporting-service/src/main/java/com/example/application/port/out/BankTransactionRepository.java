package com.example.application.port.out;

import com.example.domain.model.BankTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BankTransactionRepository {
    void saveAll(List<BankTransaction> transactions);
    List<BankTransaction> findByPeriod(String tenantId, LocalDate from, LocalDate to);
    BigDecimal sumExpenses(String tenantId, LocalDate from, LocalDate to);
}
