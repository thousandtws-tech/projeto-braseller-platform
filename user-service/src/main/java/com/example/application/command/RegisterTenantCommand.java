package com.example.application.command;

public record RegisterTenantCommand(String legalName, String tradeName, String adminName, String email, String password) {
}
