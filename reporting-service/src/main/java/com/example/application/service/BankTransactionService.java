package com.example.application.service;

import com.example.application.port.out.BankTransactionRepository;
import com.example.domain.model.BankTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class BankTransactionService {

    @Inject
    BankTransactionRepository bankTransactionRepository;

    @Inject
    OfxParserService ofxParserService;

    public List<BankTransaction> importOfx(String tenantId, String ofxContent) {
        List<BankTransaction> transactions = ofxParserService.parse(tenantId, ofxContent);
        bankTransactionRepository.saveAll(transactions);
        return transactions;
    }

    public List<BankTransaction> listByPeriod(String tenantId, LocalDate from, LocalDate to) {
        return bankTransactionRepository.findByPeriod(tenantId, from, to);
    }

    public BigDecimal sumExpenses(String tenantId, LocalDate from, LocalDate to) {
        return bankTransactionRepository.sumExpenses(tenantId, from, to);
    }
}
