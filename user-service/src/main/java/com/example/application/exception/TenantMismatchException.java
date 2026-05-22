package com.example.application.exception;

public class TenantMismatchException extends RuntimeException {
    public TenantMismatchException() {
        super("tenant_header_mismatch");
    }
}
