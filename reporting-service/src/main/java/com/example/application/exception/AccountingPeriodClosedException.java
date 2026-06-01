package com.example.application.exception;

public class AccountingPeriodClosedException extends RuntimeException {
    public AccountingPeriodClosedException(String message) {
        super(message);
    }
}
