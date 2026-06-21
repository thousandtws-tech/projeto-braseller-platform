package com.example.application.command;

public record RequestEmailVerificationCommand(String email, String requestIp) {
}
