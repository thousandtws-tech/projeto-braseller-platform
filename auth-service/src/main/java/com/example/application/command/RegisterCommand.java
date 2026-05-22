package com.example.application.command;

public record RegisterCommand(String tenantName, String fullName, String email, String password) {
}
